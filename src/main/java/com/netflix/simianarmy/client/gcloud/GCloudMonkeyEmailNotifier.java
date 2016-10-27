package com.netflix.simianarmy.client.gcloud;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosType;

public class GCloudMonkeyEmailNotifier extends ChaosEmailNotifier {

	public GCloudMonkeyEmailNotifier(AmazonSimpleEmailServiceClient sesClient) {
		super(sesClient);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isValidEmail(String email) {
		return true;
	}

	@Override
	public String buildEmailSubject(String to) {
		return "SUBJECT";
	}

	@Override
	public String[] getCcAddresses(String to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceAddress(String to) {
		return to;
	}

	@Override
	public void sendEmail(String to, String subject, String body) {
		System.out.println("send mail to " + to);

	}

	@Override
	public void sendTerminationNotification(InstanceGroup group,
			String instance, ChaosType chaosType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendTerminationGlobalNotification(InstanceGroup group,
			String instance, ChaosType chaosType) {
		// TODO Auto-generated method stub
		
	}

}
