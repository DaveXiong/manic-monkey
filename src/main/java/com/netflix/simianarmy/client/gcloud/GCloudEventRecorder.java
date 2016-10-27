/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.compute.model.Instance;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey.EventTypes;

import console.mw.monitor.slack.AttachmentField;
import console.mw.monitor.slack.MessageAttachment;
import console.mw.monitor.slack.SlackMessage;

/**
 * @author dxiong
 * 
 */
public class GCloudEventRecorder extends RDSRecorder {

	GCloudSimianArmyContext context;

	public GCloudEventRecorder(String dbDriver, String dbUser, String dbPass, String dbUrl, String dbTable,
			String region, GCloudSimianArmyContext context) {
		super(dbDriver, dbUser, dbPass, dbUrl, dbTable, region);
		this.context = context;

	}

	private SlackMessage event2Message(Event evt) {
		SlackMessage message = new SlackMessage();
		if (evt.eventType() == EventTypes.START) {
			message.setText("Monkey is running");

			List<MessageAttachment> attachments = new ArrayList<MessageAttachment>();

			for (Instance instance : context.client().listInstances()) {
				MessageAttachment attachment = new MessageAttachment();
				attachment.setText("Instance:" + instance.getName());
				List<AttachmentField> fields = new ArrayList<AttachmentField>();
				AttachmentField field = new AttachmentField();
				field.setShortText(true);
				field.setTitle("Status");
				field.setValue(instance.getStatus());
				fields.add(field);

				field = new AttachmentField();
				field.setShortText(true);
				field.setTitle("Tags");

				field.setValue(instance.getTags().getItems().toString());

				fields.add(field);

				attachment.setFields(fields);
				attachments.add(attachment);
			}

			message.setAttachments(attachments);

		} else if (evt.eventType() == EventTypes.CHAOS_TERMINATION) {
			message.setText(evt.field("chaosType") + " >> " + evt.id());
		} else if (evt.eventType() == EventTypes.STOP) {
			message.setText("Monkey is stopped");
		}
		return message;
	}

	public void recordEvent(Event evt) {
		MonitorService.sendToSlack(event2Message(evt));
		super.recordEvent(evt);
	}

}
