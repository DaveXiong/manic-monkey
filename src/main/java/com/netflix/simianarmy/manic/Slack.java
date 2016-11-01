/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.net.MalformedURLException;
import java.net.URL;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.client.gcloud.Definitions;

import console.mw.monitor.slack.SlackMessage;
import console.mw.monitor.slack.SlackMonitorListener;

/**
 * @author dxiong
 * 
 */
public class Slack {

	private static SlackMonitorListener slack = null;

	private String url;
	private String channel;
	private String agent;

	public Slack(MonkeyConfiguration configuration) {
		this.url = configuration.getStr(Definitions.Notifier.SLACK.URL);
		this.channel = configuration.getStrOrElse(Definitions.Notifier.SLACK.CHANNEL,
				Definitions.Notifier.SLACK.CHANNEL_DEFAULT);
		this.agent = configuration.getStrOrElse(Definitions.Notifier.SLACK.AGENT,
				Definitions.Notifier.SLACK.AGENT_DEFAULT);
	}

	public synchronized void sendToSlack(SlackMessage message) {
		if (slack == null) {
			try {
				slack = new SlackMonitorListener(new URL(this.url), this.channel, this.agent);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (slack != null) {
			slack.sendMessage(message);
		}
	}

}
