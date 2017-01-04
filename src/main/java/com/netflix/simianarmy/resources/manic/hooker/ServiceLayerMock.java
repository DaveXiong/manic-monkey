/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.io.InputStream;
import java.util.Properties;

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
	
	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLayerMock.class);

	private SLResourceRecorder recorder;
	private ActiveMQ activeMQ;
	
	private Slack slack;

	private PortMockHandler portRequestHandler;

	public static String PORT_REQ_QUEUE = "localhost_dxiong/" + ServiceLayerSetting.NETWORK_REQ_DEFAULT;
	public static String PORT_RES_QUEUE = "localhost_dxiong/" + ServiceLayerSetting.NETWORK_RES_DEFAULT;

	final ActiveMQMessageListener portRequestListener = new ActiveMQMessageListener() {

		final RequestContext portRequestContext = new RequestContext() {

			@Override
			public void disableConsume() {
				disablePortMock();
			}

			@Override
			public void enableConsume() {
				enablePortMock();
			}

			@Override
			public void send(String message) {
				sendMessage(PORT_REQ_QUEUE, message);

			}

			@Override
			public void broadcast(String message) {
				broadcastMessage(PORT_RES_QUEUE, message);
			}

		};

		@Override
		public void onMessage(Message message) {
			try {
				String msg = ((TextMessage) message).getText();
				System.out.println("[RECEIVED]" + msg);

				Request request = new Gson().fromJson(msg, Request.class);
				Response response = portRequestHandler.onRequest(portRequestContext, request, msg);
				if (response != null) {
					portRequestContext.broadcast(new Gson().toJson(response));
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

		portRequestHandler = new PortMockHandler(recorder);
	}

	public void enablePortMock() {
		activeMQ.subscribe(PORT_REQ_QUEUE, portRequestListener);
	}

	public void disablePortMock() {
		portRequestListener.close();
	}

	public void sendMessage(String target, String data) {
		this.activeMQ.sendMessage(target, data);
	}

	public void broadcastMessage(String target, String data) {
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
