/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.manic.ManicChaosMonkey;
import com.netflix.simianarmy.manic.Slack;

import console.mw.sl.ActiveMQ;
import console.mw.sl.Credentials;
import console.mw.sl.ServiceLayerSetting;
import console.mw.sl.activemq.ActiveMQImpl;
import console.mw.sl.activemq.ActiveMQMessageListener;
import console.mw.sl.service.schema.Request;
import console.mw.sl.service.schema.Response;

/**
 * @author dxiong
 *
 */
public class ServiceLayerMock {

	static {
		BasicConfigurator.configure();
	}

	public static enum Feature {
		L2, L3, POP, DCP
	}

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLayerMock.class);

	private SLResourceRecorder recorder;
	private ActiveMQ activeMQ;

	private Slack slack;

	private RequestHandler requestHandler;

	public static String L2_REQ_QUEUE = "localhost_dxiong/" + ServiceLayerSetting.NETWORK_REQ_DEFAULT;
	public static String L2_RES_QUEUE = "localhost_dxiong/" + ServiceLayerSetting.NETWORK_RES_DEFAULT;

	private Map<HookerType, HookerSearch> type2Search = new HashMap<HookerType, HookerSearch>();
	private Set<Feature> enabledFeatures = new HashSet<Feature>();

	final ActiveMQMessageListener l2RequestListener = new ActiveMQMessageListener() {

		final RequestContext l2RequestContext = new RequestContext() {

			@Override
			public void disable() {
				ServiceLayerMock.this.disable(Feature.L2);
			}

			@Override
			public void enable() {
				ServiceLayerMock.this.enable(Feature.L2);
			}

			@Override
			public void send(String message) {
				ServiceLayerMock.this.send(L2_REQ_QUEUE, message);
			}

			@Override
			public void broadcast(String message) {
				ServiceLayerMock.this.broadcast(L2_RES_QUEUE, message);
			}

		};

		@Override
		public void onMessage(Message message) {
			try {
				String msg = ((TextMessage) message).getText();
				System.out.println("[RECEIVED]" + msg);

				Request request = new Gson().fromJson(msg, Request.class);
				Response response = requestHandler.onRequest(l2RequestContext, request, msg);
				if (response != null) {
					l2RequestContext.broadcast(new Gson().toJson(response));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	};

	public ServiceLayerMock() {
		this.initialize();
	}

	private void initialize() {
		for (Monkey runningMonkey : MonkeyRunner.getInstance().getMonkeys()) {
			if (runningMonkey instanceof ManicChaosMonkey) {
				slack = ((ManicChaosMonkey) runningMonkey).getSlack();
				break;
			}
		}
		final BasicConfiguration configuration = new BasicConfiguration(
				loadConfigurationFileIntoProperties("sl.properties"));

		ActiveMQImpl activeMQImpl = new ActiveMQImpl(configuration.getStr("ACTIVE_MQ_SERVER"), new Credentials() {
			public String getUsername() {
				return configuration.getStr("ACTIVE_MQ_USERNAME");
			}

			public String getPassword() {
				return configuration.getStr("ACTIVE_MQ_PASSWORD");
			}

			public String getKeystore() {
				return configuration.getStr("ACTIVE_MQ_KEYSTORE");
			}

			public String getKeystorePassword() {
				return configuration.getStr("ACTIVE_MQ_KEYSTORE_PASSWORD");
			}
		});
		activeMQImpl.start();

		activeMQ = activeMQImpl;

		// initialize recorder
		recorder = new SLResourceRecorder(
				new BasicConfiguration(loadConfigurationFileIntoProperties("simianarmy.properties")));

		type2Search.put(HookerType.PORT, new PortHookerSearch());

		requestHandler = new DefaultRequestHandler(recorder, type2Search);
	}

	public HookerSearch getHookerSearch(HookerType type) {
		return type2Search.get(type);
	}

	public void enable(Feature feature) {
		if(enabledFeatures.contains(feature)){
			return;
		}
		switch (feature) {
		case L2:
			activeMQ.subscribe(L2_REQ_QUEUE, l2RequestListener);
			break;
		case L3:
		case POP:
		case DCP:
		}
		
		enabledFeatures.add(feature);
	}

	public void disable(Feature feature) {
		if(!enabledFeatures.contains(feature)){
			return;
		}
		switch(feature){
		case L2:
			l2RequestListener.close();
			break;
		case L3:
		case POP:
		case DCP:
		}
		
		enabledFeatures.remove(feature);
	}

	public void send(String target, String data) {
		this.activeMQ.sendMessage(target, data);
	}

	public void broadcast(String target, String data) {
		this.activeMQ.sendMessage(target, data, true, 0);
	}

	/** loads the given config on top of the config read by previous calls. */
	protected static Properties loadConfigurationFileIntoProperties(String propertyFileName) {
		Properties properties = new Properties();
		String propFile = System.getProperty(propertyFileName, "/" + propertyFileName);
		try {
			LOGGER.info("loading properties file: " + propFile);
			InputStream is = ManicSLResource.class.getResourceAsStream(propFile);
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

		return properties;
	}

	public SLResourceRecorder getRecorder() {
		return this.recorder;
	}

}
