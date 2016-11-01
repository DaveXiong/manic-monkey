package com.netflix.simianarmy.client.gcloud;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicScheduler;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosMonkey;

/**
 * The Class GCloudClientContext.
 */
public class BasicClientContext implements ChaosMonkey.Context {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicClientContext.class);

	/** The configuration properties. */
	private final Properties properties = new Properties();

	/** The scheduler. */
	private MonkeyScheduler scheduler;

	/** The calendar. */
	private MonkeyCalendar calendar;

	/** The config. */
	private BasicConfiguration config;

	/** The client. */
	private BasicClient client;

	/** The recorder. */
	private MonkeyRecorder recorder;

	/** The reported events. */
	private final LinkedList<Event> eventReport;

	private final String zone;

	/** The crawler. */
	private ChaosCrawler crawler;

	/** The selector. */
	private ChaosInstanceSelector selector;

	/** The chaos email notifier. */
	private ChaosEmailNotifier chaosEmailNotifier;

	/**
	 * The key name of the tag owner used to tag resources - across all Monkeys
	 */
	public static String GLOBAL_OWNER_TAGKEY;

	/** protected constructor as the Shell is meant to be subclassed. */
	public BasicClientContext() {
		eventReport = new LinkedList<Event>();
		// Load the config files into props following the provided order.
		for (String properties : Definitions.GCloud.CONFIG) {
			loadConfigurationFileIntoProperties(properties);
		}

		LOGGER.info("The following are properties in the context.");
		for (Entry<Object, Object> prop : properties.entrySet()) {
			Object propertyKey = prop.getKey();
			if (isSafeToLog(propertyKey)) {
				LOGGER.info(String.format("%s = %s", propertyKey, prop.getValue()));
			} else {
				LOGGER.info(String.format("%s = (not shown here)", propertyKey));
			}
		}

		config = new BasicConfiguration(properties);
		zone = config.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT);

		this.createClient();
		this.createCalendar();
		this.createScheduler();
		this.createRecorder();
		this.createCrawler();
		this.createInstanceSelector();
		this.createEmailNotificator();

	}

	/**
	 * Checks whether it is safe to log the property based on the given property
	 * key.
	 * 
	 * @param propertyKey
	 *            The key for the property, expected to be resolvable to a
	 *            String
	 * @return A boolean indicating whether it is safe to log the corresponding
	 *         property
	 */
	protected boolean isSafeToLog(Object propertyKey) {
		String propertyKeyName = propertyKey.toString();
		return !propertyKeyName.contains("secretKey") && !propertyKeyName.contains("vsphere.password");
	}

	/** loads the given config on top of the config read by previous calls. */
	protected void loadConfigurationFileIntoProperties(String propertyFileName) {
		String propFile = System.getProperty(propertyFileName, "/" + propertyFileName);
		try {
			LOGGER.info("loading properties file: " + propFile);
			InputStream is = BasicClientContext.class.getResourceAsStream(propFile);
			try {
				properties.load(is);
			} finally {
				is.close();
			}
		} catch (Exception e) {
			String msg = "Unable to load properties file " + propFile + " set System property \"" + propertyFileName
					+ "\" to valid file";
			LOGGER.error(msg);
			throw new RuntimeException(msg, e);
		}
	}

	private void createScheduler() {
		int freq = (int) config.getNumOrElse(Definitions.Scheduler.FREQUEUE, Definitions.Scheduler.FREQUEUE_DEFAULT);
		TimeUnit freqUnit = TimeUnit.valueOf(
				config.getStrOrElse(Definitions.Scheduler.FREQUEUE_UNIT, Definitions.Scheduler.FREQUEUE_UNIT_DEFAULT));
		int threads = (int) config.getNumOrElse(Definitions.Scheduler.THREADS, Definitions.Scheduler.THREADS_DEFAULT);
		setScheduler(new BasicScheduler(freq, freqUnit, threads));
	}

	@SuppressWarnings("unchecked")
	private void createRecorder() {

		@SuppressWarnings("rawtypes")
		Class recorderClass = loadClientClass(Definitions.Recorder.CLASS, Definitions.Recorder.CLASS_DEFAULT);
		if (recorderClass != null && recorderClass.equals(RDSRecorder.class)) {
			String dbDriver = configuration().getStr(Definitions.Recorder.RDS.DRIVER);
			String dbUser = configuration().getStr(Definitions.Recorder.RDS.USER);
			String dbPass = configuration().getStr(Definitions.Recorder.RDS.PASSWORD);
			String dbUrl = configuration().getStr(Definitions.Recorder.RDS.URL);
			String dbTable = configuration().getStr(Definitions.Recorder.RDS.TABLE);

			RDSRecorder rdsRecorder = new RDSRecorder(dbDriver, dbUser, dbPass, dbUrl, dbTable, this.zone);
			rdsRecorder.init();
			setRecorder(rdsRecorder);
		} else {
			setRecorder((MonkeyRecorder) factory(recorderClass));
		}
	}

	@SuppressWarnings("unchecked")
	private void createCrawler() {
		@SuppressWarnings("rawtypes")
		Class cls = loadClientClass(Definitions.Crawler.CLASS, Definitions.Crawler.CLASS_DEFAULT);
		if (cls != null && cls.equals(BasicChaosCrawler.class)) {
			ChaosCrawler chaosCrawler = new BasicChaosCrawler(this.client());
			this.setChaosCrawler(chaosCrawler);

		} else {
			this.setChaosCrawler((ChaosCrawler) this.factory(cls));
		}
	}

	@SuppressWarnings("unchecked")
	private void createCalendar() {
		@SuppressWarnings("rawtypes")
		Class calendarClass = loadClientClass(Definitions.Calendar.CLASS, Definitions.Calendar.CLASS_DEFAULT);
		setCalendar((MonkeyCalendar) factory(calendarClass));
	}

	@SuppressWarnings("unchecked")
	private void createInstanceSelector() {
		@SuppressWarnings("rawtypes")
		Class cls = loadClientClass(Definitions.InstanceSelector.CLASS, Definitions.InstanceSelector.CLASS_DEFAULT);
		this.setChaosInstanceSelector((ChaosInstanceSelector) this.factory(cls));

	}

	@SuppressWarnings("unchecked")
	private void createEmailNotificator() {
		@SuppressWarnings("rawtypes")
		Class cls = loadClientClass(Definitions.Notifier.Email.CLASS, Definitions.Notifier.Email.CLASS_DEFAULT);
		this.setChaosEmailNotifier((ChaosEmailNotifier) this.factory(cls));

	}

	/**
	 * Create the specific client within passed region, using the appropriate
	 * AWS credentials provider and client configuration.
	 * 
	 * @param clientRegion
	 */
	@SuppressWarnings("unchecked")
	protected void createClient() {
		@SuppressWarnings("rawtypes")
		Class cls = loadClientClass(Definitions.Client.CLASS, Definitions.Client.CLASS_DEFAULT);
		setCloudClient((CloudClient) this.factory(cls));
	}

	/**
	 * Gets the AWS client.
	 * 
	 * @return the AWS client
	 */
	public BasicClient client() {
		return client;
	}

	@Override
	public void reportEvent(Event evt) {
		this.eventReport.add(evt);
	}

	@Override
	public void resetEventReport() {
		eventReport.clear();
	}

	@Override
	public String getEventReport() {
		StringBuilder report = new StringBuilder();
		for (Event event : this.eventReport) {
			report.append(String.format("%s %s (", event.eventType(), event.id()));
			boolean isFirst = true;
			for (Entry<String, String> field : event.fields().entrySet()) {
				if (!isFirst) {
					report.append(", ");
				} else {
					isFirst = false;
				}
				report.append(String.format("%s:%s", field.getKey(), field.getValue()));
			}
			report.append(")\n");
		}
		return report.toString();
	}

	/** {@inheritDoc} */
	@Override
	public MonkeyScheduler scheduler() {
		return scheduler;
	}

	/**
	 * Sets the scheduler.
	 * 
	 * @param scheduler
	 *            the new scheduler
	 */
	protected void setScheduler(MonkeyScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/** {@inheritDoc} */
	@Override
	public MonkeyCalendar calendar() {
		return calendar;
	}

	/**
	 * Sets the calendar.
	 * 
	 * @param calendar
	 *            the new calendar
	 */
	protected void setCalendar(MonkeyCalendar calendar) {
		this.calendar = calendar;
	}

	/** {@inheritDoc} */
	@Override
	public MonkeyConfiguration configuration() {
		return config;
	}

	/**
	 * Sets the configuration.
	 * 
	 * @param configuration
	 *            the new configuration
	 */
	protected void setConfiguration(MonkeyConfiguration configuration) {
		this.config = (BasicConfiguration) configuration;
	}

	/** {@inheritDoc} */
	@Override
	public CloudClient cloudClient() {
		return client;
	}

	/**
	 * Sets the cloud client.
	 * 
	 * @param cloudClient
	 *            the new cloud client
	 */
	protected void setCloudClient(CloudClient cloudClient) {
		this.client = (BasicClient) cloudClient;
	}

	/** {@inheritDoc} */
	@Override
	public MonkeyRecorder recorder() {
		return recorder;
	}

	/**
	 * Sets the recorder.
	 * 
	 * @param recorder
	 *            the new recorder
	 */
	protected void setRecorder(MonkeyRecorder recorder) {
		this.recorder = recorder;
	}

	/**
	 * Gets the configuration properties.
	 * 
	 * @return the configuration properties
	 */
	protected Properties getProperties() {
		return this.properties;
	}

	/**
	 * Load a class specified by the config; for drop-in replacements.
	 * (Duplicates a method in MonkeyServer; refactor to util?).
	 * 
	 * @param key
	 * @return the loaded class or null if the class is not found
	 */
	@SuppressWarnings("rawtypes")
	private Class loadClientClass(String key, String defaultValue) {
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			String clientClassName = config.getStrOrElse(key, defaultValue);
			if (clientClassName == null || clientClassName.isEmpty()) {
				LOGGER.info("using standard class for " + key);
				return null;
			}
			Class newClass = classLoader.loadClass(clientClassName);
			LOGGER.info("using " + key + " loaded " + newClass.getCanonicalName());
			return newClass;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not load " + key, e);
		}
	}

	/**
	 * Generic factory to create monkey collateral types.
	 * 
	 * @param <T>
	 *            the generic type to create
	 * @param implClass
	 *            the actual concrete type to instantiate.
	 * @return an object of the requested type
	 */
	private <T> T factory(Class<T> implClass) {
		try {
			// then find corresponding ctor
			for (Constructor<?> ctor : implClass.getDeclaredConstructors()) {
				Class<?>[] paramTypes = ctor.getParameterTypes();
				if (paramTypes.length != 1) {
					continue;
				}
				if (paramTypes[0].getName().endsWith("Configuration")) {
					@SuppressWarnings("unchecked")
					T impl = (T) ctor.newInstance(config);
					return impl;
				}
			}
			// Last ditch; try no-arg.
			return implClass.newInstance();
		} catch (Exception e) {
			LOGGER.error("context config error, cannot make an instance of " + implClass.getName(), e);
		}
		return null;
	}

	/** {@inheritDoc} */
	public ChaosCrawler chaosCrawler() {
		return crawler;
	}

	/**
	 * Sets the chaos crawler.
	 * 
	 * @param chaosCrawler
	 *            the new chaos crawler
	 */
	protected void setChaosCrawler(ChaosCrawler chaosCrawler) {
		this.crawler = chaosCrawler;
	}

	/** {@inheritDoc} */
	public ChaosInstanceSelector chaosInstanceSelector() {
		return selector;
	}

	/**
	 * Sets the chaos instance selector.
	 * 
	 * @param chaosInstanceSelector
	 *            the new chaos instance selector
	 */
	protected void setChaosInstanceSelector(ChaosInstanceSelector chaosInstanceSelector) {
		this.selector = chaosInstanceSelector;
	}

	public ChaosEmailNotifier chaosEmailNotifier() {
		return chaosEmailNotifier;
	}

	/**
	 * Sets the chaos email notifier.
	 * 
	 * @param notifier
	 *            the chaos email notifier
	 */
	protected void setChaosEmailNotifier(ChaosEmailNotifier notifier) {
		this.chaosEmailNotifier = notifier;
	}

}
