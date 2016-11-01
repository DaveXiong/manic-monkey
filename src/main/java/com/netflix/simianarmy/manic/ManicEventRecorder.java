/**
 * 
 */
package com.netflix.simianarmy.manic;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.client.gcloud.Definitions;
import com.netflix.simianarmy.manic.ManicChaosMonkey.ManicEventTypes;

import console.mw.monitor.slack.SlackMessage;

/**
 * @author dxiong
 * 
 */
public class ManicEventRecorder extends RDSRecorder {

	private Slack slack;

	public ManicEventRecorder(MonkeyConfiguration configuration) {
		super(configuration.getStr(Definitions.Recorder.RDS.DRIVER),
				configuration.getStr(Definitions.Recorder.RDS.USER),
				configuration.getStr(Definitions.Recorder.RDS.PASSWORD),
				configuration.getStr(Definitions.Recorder.RDS.URL),
				configuration.getStr(Definitions.Recorder.RDS.TABLE),
				configuration.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT));
		this.init();

		this.slack = new Slack(configuration);
	}

	private SlackMessage event2Message(Event evt) {
		SlackMessage message = new SlackMessage();
		if (evt.eventType().toString().equalsIgnoreCase(ChaosMonkey.EventTypes.CHAOS_TERMINATION.toString())) {
			message.setText(evt.field("chaosType") + " " + evt.id());
		} else {
			switch ((ManicEventTypes) evt.eventType()) {
			case MONKEY_START:
				message.setText("Start moneky");
				break;
			case MONKEY_STOP:
				message.setText("Stop monkey");
				break;
			case MONKEY_PAUSE:
				message.setText("Pause monkey");
				break;
			case MONKEY_RESUME:
				message.setText("Resume monkey");
				break;
			default:
				message.setText(evt.eventType() + " monkey");
				break;
			}
		}

		return message;
	}

	public void recordEvent(Event evt) {
		slack.sendToSlack(event2Message(evt));
		super.recordEvent(evt);
	}

}
