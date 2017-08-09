/**
 * 
 */
package com.netflix.simianarmy.manic;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.client.gcloud.Definitions;
import com.netflix.simianarmy.client.gcloud.Gce;
import com.netflix.simianarmy.manic.ManicChaosMonkey.ManicEventTypes;
import com.netflix.simianarmy.manic.ManicEvent.Command;
import com.netflix.simianarmy.manic.ManicEvent.InstancePayload;

/**
 * @author dxiong
 * 
 */
public class ManicEventRecorder extends RDSRecorder {

	public ManicEventRecorder(MonkeyConfiguration configuration) {
		super(configuration.getStr(Definitions.Recorder.RDS.DRIVER),
				configuration.getStr(Definitions.Recorder.RDS.USER),
				configuration.getStr(Definitions.Recorder.RDS.PASSWORD),
				configuration.getStr(Definitions.Recorder.RDS.URL),
				configuration.getStr(Definitions.Recorder.RDS.TABLE),
				configuration.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT));
		this.init();
	}

	private ManicEvent toManicEvent(Event evt) {
		if (evt.eventType().toString().equalsIgnoreCase(ChaosMonkey.EventTypes.CHAOS_TERMINATION.toString())) {
			String type = evt.field("chaosType");

			ManicEvent.Command command = null;
			if ("ShutdownInstance".equalsIgnoreCase(type)) {
				command = ManicEvent.Command.STOP;
			} else if ("StartInstance".equalsIgnoreCase(type)) {
				command = ManicEvent.Command.START;
			}

			ManicEvent event = new ManicEvent(ManicEvent.Type.INSTANCE, command);
			InstancePayload payload = new InstancePayload();
			payload.setName(evt.id());
			payload.setGroup(evt.field("groupName"));
			payload.setRegion(evt.field("region"));
			event.setPayload(payload);

			return event;
		} else {
			ManicEvent.Command command = null;
			switch ((ManicEventTypes) evt.eventType()) {
			case MONKEY_START:
				command = Command.START;
				break;
			case MONKEY_STOP:
				command = Command.STOP;
				break;
			case MONKEY_PAUSE:
				command = Command.PAUSE;
				break;
			case MONKEY_RESUME:
				command = Command.RESUME;
				break;

			}

			ManicEvent event = new ManicEvent(ManicEvent.Type.MONKEY, command);
			InstancePayload payload = new InstancePayload();
			event.setPayload(payload);

			return event;
		}

	}

	public void recordEvent(Event evt) {
		evt.addField("region", Gce.getRegion(evt.id()));
		MonkeyEventDispatcher.INSTANCE.dispatch(toManicEvent(evt));
		super.recordEvent(evt);
	}

}
