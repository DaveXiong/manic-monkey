/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
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
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.basic.BasicCalendar;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicScheduler;

/**
 * The Class BasicSimianArmyContext.
 */
public class GCloudSimianArmyContext implements Monkey.Context {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(GCloudSimianArmyContext.class);

	/** The configuration properties. */
	private final Properties properties = new Properties();

	/** The Constant MONKEY_THREADS. */
	private static final int MONKEY_THREADS = 1;

	/** The scheduler. */
	private MonkeyScheduler scheduler;

	/** The calendar. */
	private MonkeyCalendar calendar;

	/** The config. */
	private BasicConfiguration config;

	/** The client. */
	private GCloudClient client;

	/** The recorder. */
	private MonkeyRecorder recorder;

	/** The reported events. */
	private final LinkedList<Event> eventReport;

	private final String credential;

	private final String zone;
	private final String project;

	private static final String DEFAULT_REGION = "us-east-1";;

	/**
	 * The key name of the tag owner used to tag resources - across all Monkeys
	 */
	public static String GLOBAL_OWNER_TAGKEY;

	/** protected constructor as the Shell is meant to be subclassed. */
	protected GCloudSimianArmyContext(String... configFiles) {
		eventReport = new LinkedList<Event>();
		// Load the config files into props following the provided order.
		for (String configFile : configFiles) {
			loadConfigurationFileIntoProperties(configFile);
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

		credential = config.getStr("simianarmy.client.gcloud.credential");
		zone = config.getStrOrElse("simianarmy.client.gcloud.zone", DEFAULT_REGION);
		project = config.getStrOrElse("simianarmy.client.gcloud.project", null);

		createClient();

		createCalendar();

		createScheduler();

		createRecorder();

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
			InputStream is = GCloudSimianArmyContext.class.getResourceAsStream(propFile);
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
		int freq = (int) config.getNumOrElse("simianarmy.scheduler.frequency", 1);
		TimeUnit freqUnit = TimeUnit.valueOf(config.getStrOrElse("simianarmy.scheduler.frequencyUnit", "HOURS"));
		int threads = (int) config.getNumOrElse("simianarmy.scheduler.threads", MONKEY_THREADS);
		setScheduler(new BasicScheduler(freq, freqUnit, threads));
	}

	@SuppressWarnings("unchecked")
	private void createRecorder() {
		@SuppressWarnings("rawtypes")
		Class recorderClass = loadClientClass("simianarmy.client.recorder.class");
		if (recorderClass != null && recorderClass.equals(RDSRecorder.class)) {
			String dbDriver = configuration().getStr("simianarmy.recorder.db.driver");
			String dbUser = configuration().getStr("simianarmy.recorder.db.user");
			String dbPass = configuration().getStr("simianarmy.recorder.db.pass");
			String dbUrl = configuration().getStr("simianarmy.recorder.db.url");
			String dbTable = configuration().getStr("simianarmy.recorder.db.table");

			RDSRecorder rdsRecorder = new RDSRecorder(dbDriver, dbUser, dbPass, dbUrl, dbTable, client.region());
			rdsRecorder.init();
			setRecorder(rdsRecorder);
		} else if (recorderClass != null && recorderClass.equals(GCloudEventRecorder.class)) {
			String dbDriver = configuration().getStr("simianarmy.recorder.db.driver");
			String dbUser = configuration().getStr("simianarmy.recorder.db.user");
			String dbPass = configuration().getStr("simianarmy.recorder.db.pass");
			String dbUrl = configuration().getStr("simianarmy.recorder.db.url");
			String dbTable = configuration().getStr("simianarmy.recorder.db.table");

			GCloudEventRecorder rdsRecorder = new GCloudEventRecorder(dbDriver, dbUser, dbPass, dbUrl, dbTable,
					client.region(),this);
			rdsRecorder.init();
			setRecorder(rdsRecorder);
		} else {
			setRecorder((MonkeyRecorder) factory(recorderClass));
		}
	}

	@SuppressWarnings("unchecked")
	private void createCalendar() {
		@SuppressWarnings("rawtypes")
		Class calendarClass = loadClientClass("simianarmy.calendar.class");
		if (calendarClass == null || calendarClass.equals(BasicCalendar.class)) {
			setCalendar(new BasicCalendar(config));
		} else {
			setCalendar((MonkeyCalendar) factory(calendarClass));
		}
	}

	/**
	 * Create the specific client within passed region, using the appropriate
	 * AWS credentials provider and client configuration.
	 * 
	 * @param clientRegion
	 */
	protected void createClient() {
		this.client = new GCloudClient(project, zone, credential);
		setCloudClient(this.client);
	}

	/**
	 * Gets the AWS client.
	 * 
	 * @return the AWS client
	 */
	public GCloudClient client() {
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
		this.client = (GCloudClient) cloudClient;
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
	private Class loadClientClass(String key) {
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			String clientClassName = config.getStrOrElse(key, null);
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

}
