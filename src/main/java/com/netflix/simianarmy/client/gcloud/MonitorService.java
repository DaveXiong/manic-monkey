/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import console.mw.monitor.slack.SlackMessage;
import console.mw.monitor.slack.SlackMonitorListener;

/**
 * @author dxiong
 * 
 */
public class MonitorService {

	private static SlackMonitorListener slack = null;

	private static final Logger LOG = Logger.getLogger(MonitorService.class);

	static {

		try {
			slack = new SlackMonitorListener(
					new URL(
							"https://hooks.slack.com/services/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"),
					"#manic-monkey", "manic-monkey");
			// MonitorPlatform.INSTANCE.registerListener(slack);

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void sendToSlack(SlackMessage message) {
		if (slack != null) {
			// send data to slack
			slack.sendMessage(message);
		}
	}

}
