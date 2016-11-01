/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosType;

/**
 * @author dxiong
 *
 */
public class DummyMonkyEmailNotifier extends ChaosEmailNotifier {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DummyMonkyEmailNotifier.class);

	/**
	 * 
	 */
	public DummyMonkyEmailNotifier() {
		super(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.MonkeyEmailNotifier#isValidEmail(java.lang.String)
	 */
	@Override
	public boolean isValidEmail(String email) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.MonkeyEmailNotifier#buildEmailSubject(java.lang.
	 * String)
	 */
	@Override
	public String buildEmailSubject(String to) {
		return to;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.MonkeyEmailNotifier#getCcAddresses(java.lang.
	 * String)
	 */
	@Override
	public String[] getCcAddresses(String to) {
		return new String[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.MonkeyEmailNotifier#getSourceAddress(java.lang.
	 * String)
	 */
	@Override
	public String getSourceAddress(String to) {
		return to;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.MonkeyEmailNotifier#sendEmail(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void sendEmail(String to, String subject, String body) {
		LOGGER.info("sendEmail>>to:{},subject:{},body:{}", to, subject, body);
	}

	@Override
	public void sendTerminationNotification(InstanceGroup group, String instance, ChaosType chaosType) {
		LOGGER.info("sendTerminationNotification>>", group, instance, chaosType);

	}

	@Override
	public void sendTerminationGlobalNotification(InstanceGroup group, String instance, ChaosType chaosType) {
		LOGGER.info("sendTerminationGlobalNotification>>", group, instance, chaosType);

	}

}
