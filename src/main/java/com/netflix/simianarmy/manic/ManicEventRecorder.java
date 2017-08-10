/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.aws.SimpleDBRecorder;
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
	
	   public List<Event> findEvents(MonkeyType monkeyType, EventType eventType, Map<String, String> query, Date after) {
	        ArrayList<Object> args = new ArrayList<>();
	        StringBuilder sqlquery = new StringBuilder(
	                String.format("select * from %s where ", this.getTable()));
	        
	        if (monkeyType != null) {
	        	sqlquery.append(String.format(" %s = ?", FIELD_MONKEY_TYPE));
	        	args.add(SimpleDBRecorder.enumToValue(monkeyType));
	        }

	        if (eventType != null) {
	        	sqlquery.append(String.format(" and %s = ?", FIELD_EVENT_TYPE));
	        	args.add(SimpleDBRecorder.enumToValue(eventType));
	        }
	        
	        for (Map.Entry<String, String> pair : query.entrySet()) {
	        	sqlquery.append(String.format(" and %s like ?", FIELD_DATA_JSON));
	            args.add((String.format("%s: \"%s\"", pair.getKey(), pair.getValue())));
	        }
	        sqlquery.append(String.format(" and %s > ? order by %s desc", FIELD_EVENT_TIME, FIELD_EVENT_TIME));
	        args.add(new Long(after.getTime()));
	        
	        LOGGER.info(String.format("Query is '%s'", sqlquery));
	        List<Event> events = this.getJdbcTemplate().query(sqlquery.toString(), args.toArray(), new RowMapper<Event>() {
	            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
	            	return mapEvent(rs);                
	            }             
	        });                
	        return events;
	    }

}
