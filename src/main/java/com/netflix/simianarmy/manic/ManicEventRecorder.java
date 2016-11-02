/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.util.ArrayList;
import java.util.List;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.client.gcloud.Definitions;
import com.netflix.simianarmy.manic.ManicChaosMonkey.ManicEventTypes;

import console.mw.monitor.slack.AttachmentField;
import console.mw.monitor.slack.MessageAttachment;
import console.mw.monitor.slack.SlackMessage;

/**
 * @author dxiong
 * 
 */
public class ManicEventRecorder extends RDSRecorder {

	private Slack slack;

	private ManicChaosMonkey monkey;

	private MonkeyConfiguration configuration;

	public ManicEventRecorder(MonkeyConfiguration configuration) {
		super(configuration.getStr(Definitions.Recorder.RDS.DRIVER),
				configuration.getStr(Definitions.Recorder.RDS.USER),
				configuration.getStr(Definitions.Recorder.RDS.PASSWORD),
				configuration.getStr(Definitions.Recorder.RDS.URL),
				configuration.getStr(Definitions.Recorder.RDS.TABLE),
				configuration.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT));
		this.init();

		this.slack = new Slack(configuration);

		for (Monkey runningMonkey : MonkeyRunner.getInstance().getMonkeys()) {
			if (runningMonkey instanceof ManicChaosMonkey) {
				this.monkey = (ManicChaosMonkey) runningMonkey;
				break;
			}
		}

		this.configuration = configuration;
	}

	private SlackMessage event2Message(Event evt) {
		SlackMessage message = new SlackMessage();
		if (evt.eventType().toString().equalsIgnoreCase(ChaosMonkey.EventTypes.CHAOS_TERMINATION.toString())) {
			message.setText(evt.field("chaosType") + " " + evt.id());
		} else {
			switch ((ManicEventTypes) evt.eventType()) {
			case MONKEY_START:
				message.setText("Monkey is ready");
				break;
			case MONKEY_STOP:
				message.setText("Monkey has been terminated");
				break;
			case MONKEY_PAUSE:
				message.setText("Monkey has been paused");
				break;
			case MONKEY_RESUME:
				message.setText("Monkey has been resumed");
				break;
			default:
				message.setText(evt.eventType() + " monkey");
				break;
			}
			
			List<MessageAttachment> attachments = new ArrayList<MessageAttachment>();
			MessageAttachment attachment = new MessageAttachment();

			List<AttachmentField> fields = new ArrayList<AttachmentField>();
			AttachmentField field = new AttachmentField();
			field.setShortText(true);
			field.setTitle("Version");
			field.setValue(com.netflix.simianarmy.manic.Definitions.VERSION);
			fields.add(field);

			field = new AttachmentField();
			field.setShortText(true);
			field.setTitle("Enabled");
			field.setValue((monkey == null || monkey.isPaused()) ? "false" : "true");

			fields.add(field);

			field = new AttachmentField();
			field.setShortText(true);
			field.setTitle("Project");
			field.setValue(configuration.getStrOrElse(Definitions.GCloud.PROJECT, "UNKNOWN"));

			fields.add(field);

			field = new AttachmentField();
			field.setShortText(true);
			field.setTitle("Zone");
			field.setValue(configuration.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT));

			fields.add(field);

			field = new AttachmentField();
			field.setShortText(true);
			field.setTitle("Schedule");
			String schedule = configuration.getNumOrElse(Definitions.Scheduler.FREQUEUE,
					Definitions.Scheduler.FREQUEUE_DEFAULT) + " "
					+ configuration.getStrOrElse(Definitions.Scheduler.FREQUEUE_UNIT,
							Definitions.Scheduler.FREQUEUE_UNIT_DEFAULT);
			field.setValue(schedule);

			fields.add(field);

			attachment.setFields(fields);
			attachments.add(attachment);
			message.setAttachments(attachments);

		}

		
		return message;
	}

	public void recordEvent(Event evt) {
		slack.sendToSlack(event2Message(evt));
		super.recordEvent(evt);
	}

}
