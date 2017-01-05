/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.amazonaws.AmazonClientException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.aws.RDSRecorder;
import com.netflix.simianarmy.aws.SimpleDBRecorder;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicRecorderEvent;
import com.netflix.simianarmy.client.gcloud.Definitions;

import console.mw.sl.service.schema.AllocatePortCommandArgs;
import console.mw.sl.service.schema.AllocatePortPayload;
import console.mw.sl.service.schema.Command;

/**
 * @author dxiong
 *
 */
public class SLResourceRecorder extends RDSRecorder {

	private static final Logger LOGGER = LoggerFactory.getLogger(SLResourceRecorder.class);

	public static final String FIELD_COMMAND = "command";
	public static final String FIELD_UUID = "uuid";
	public static final String FIELD_MSGID = "msgId";

	private enum Enums implements MonkeyType {
		SL
	};

	// private enum EventEnums implements EventType {
	// PORT
	// }

	/**
	 * @param dbDriver
	 * @param dbUser
	 * @param dbPass
	 * @param dbUrl
	 * @param dbTable
	 * @param region
	 */
	public SLResourceRecorder(BasicConfiguration configuration) {
		super(configuration.getStr(Definitions.Recorder.RDS.DRIVER),
				configuration.getStr(Definitions.Recorder.RDS.USER),
				configuration.getStr(Definitions.Recorder.RDS.PASSWORD),
				configuration.getStr(Definitions.Recorder.RDS.URL), getTable(), getRegion());
		this.init();
	}

	/**
	 * Creates the RDS table, if it does not already exist.
	 */
	public void init() {
		try {

			LOGGER.info("Creating RDS table: {}", getTable());
			String sql = String.format(
					"create table if not exists %s (" + " %s varchar(255)," + " %s BIGINT," + " %s varchar(255),"
							+ " %s varchar(255)," + " %s varchar(255)," + " %s varchar(255)," + " %s varchar(255),"
							+ " %s varchar(4096) )",
					getTable(), FIELD_ID, FIELD_EVENT_TIME, FIELD_MONKEY_TYPE, FIELD_EVENT_TYPE, FIELD_COMMAND,
					FIELD_UUID, FIELD_MSGID, FIELD_DATA_JSON);
			LOGGER.debug("Create SQL is: '{}'", sql);
			getJdbcTemplate().execute(sql);

		} catch (AmazonClientException e) {
			LOGGER.warn("Error while trying to auto-create RDS table", e);
		}
	}

	public static String getRegion() {
		return "SL";
	}

	public static String getTable() {
		return "SL";
	}

	public PortHooker addHooker(PortHooker hooker) {
		BasicRecorderEvent event = new BasicRecorderEvent(Enums.SL, HookerType.PORT, SLResourceRecorder.getRegion(),
				UUID.randomUUID().toString());

		String uuid = null;
		String command = hooker.getRequest().getCommand().toString();
		String messageId = hooker.getRequest().getId();
		AllocatePortCommandArgs commandArgs = hooker.getRequest().getCommandArgs();
		if (commandArgs != null) {
			uuid = commandArgs.getPortUuid();
		}
		event.addField("json", new Gson().toJson(hooker));

		String evtTime = String.valueOf(event.eventTime().getTime());
		String name = String.format("%s-%s-%s-%s", event.monkeyType().name(), event.id(), getRegion(), evtTime);
		String json = new Gson().toJson(hooker);

		LOGGER.debug(String.format("Saving event %s to RDS table %s", name, getTable()));
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(getTable());
		sb.append(" (");
		sb.append(FIELD_ID).append(",");
		sb.append(FIELD_EVENT_TIME).append(",");
		sb.append(FIELD_MONKEY_TYPE).append(",");
		sb.append(FIELD_EVENT_TYPE).append(",");
		sb.append(FIELD_COMMAND).append(",");
		sb.append(FIELD_UUID).append(",");
		sb.append(FIELD_MSGID).append(",");
		sb.append(FIELD_DATA_JSON).append(") values (?,?,?,?,?,?,?,?)");

		LOGGER.debug(String.format("Insert statement is '%s'", sb));
		int updated = this.getJdbcTemplate().update(sb.toString(), event.id(), event.eventTime().getTime(),
				SimpleDBRecorder.enumToValue(event.monkeyType()), SimpleDBRecorder.enumToValue(event.eventType()),
				command, uuid, messageId, json);
		LOGGER.debug(String.format("%d rows inserted", updated));

		return hooker;
	}

	public void addHooker(HookerType type, String messageId, String uuid, String command, String json) {
		BasicRecorderEvent event = new BasicRecorderEvent(Enums.SL, type, SLResourceRecorder.getRegion(),
				UUID.randomUUID().toString());
		event.addField("json", json);

		String evtTime = String.valueOf(event.eventTime().getTime());
		String name = String.format("%s-%s-%s-%s", event.monkeyType().name(), event.id(), getRegion(), evtTime);

		LOGGER.debug(String.format("Saving event %s to RDS table %s", name, getTable()));
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(getTable());
		sb.append(" (");
		sb.append(FIELD_ID).append(",");
		sb.append(FIELD_EVENT_TIME).append(",");
		sb.append(FIELD_MONKEY_TYPE).append(",");
		sb.append(FIELD_EVENT_TYPE).append(",");
		sb.append(FIELD_COMMAND).append(",");
		sb.append(FIELD_UUID).append(",");
		sb.append(FIELD_MSGID).append(",");
		sb.append(FIELD_DATA_JSON).append(") values (?,?,?,?,?,?,?,?)");

		LOGGER.debug(String.format("Insert statement is '%s'", sb));
		int updated = this.getJdbcTemplate().update(sb.toString(), event.id(), event.eventTime().getTime(),
				SimpleDBRecorder.enumToValue(event.monkeyType()), SimpleDBRecorder.enumToValue(event.eventType()),
				command, uuid, messageId, json);
		LOGGER.debug(String.format("%d rows inserted", updated));

	}

	public void deleteHooker(String hookerId) {
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE from  ").append(getTable());
		sb.append(" where ").append(FIELD_ID).append(" = '").append(hookerId).append("'");

		LOGGER.debug(String.format("Delete statement is '%s'", sb));
		this.getJdbcTemplate().execute(sb.toString());
	}

	public List<Hooker<?, ?>> getHookers(HookerType hookerType) {
		List<Hooker<?, ?>> hookers = new ArrayList<Hooker<?, ?>>();

		Type type = null;

		switch (hookerType) {
		case PORT:
		case CONNECTION:
		case CR:
		case PEER:
		case AWS:
		default:
			type = new TypeToken<Hooker<AllocatePortCommandArgs, AllocatePortPayload>>() {
			}.getType();
		}
		List<Event> events = this.findEvents(Enums.SL, hookerType, new HashMap<String, String>());
		for (Event event : events) {
			Hooker<?, ?> hooker = new Gson().fromJson(event.field("json"), type);
			hooker.setId(event.id());
			hookers.add(hooker);
		}
		return hookers;
	}

	public List<PortHooker> getPortHookers() {
		return getPortHookers(new HashMap<String, String>());
	}

	public List<PortHooker> getPortHookers(Command command) {

		Map<String, String> query = new HashMap<String, String>();
		query.put(FIELD_COMMAND, command.toString());
		return getPortHookers(query);
	}

	public List<PortHooker> getPortHookers(Command command, String uuid) {

		Map<String, String> query = new HashMap<String, String>();
		query.put(FIELD_COMMAND, command.toString());
		query.put(FIELD_UUID, uuid);

		return getPortHookers(query);
	}

	// public List<PortHooker> getPortHookersByCustomerId(String command, String
	// customerId) {
	//
	// Map<String, String> query = new HashMap<String, String>();
	// query.put("command", command);
	// query.put("customerId", customerId);
	//
	// return getPortHookers(query);
	// }

	public PortHooker getPortHookerByMessageId(String messageId) {
		Map<String, String> query = new HashMap<String, String>();
		query.put(FIELD_MSGID, messageId);

		List<PortHooker> hookers = getPortHookers(query);

		if (hookers == null || hookers.isEmpty()) {
			return null;
		}

		return hookers.get(0);
	}

	public List<PortHooker> getPortHookers(Map<String, String> query) {
		List<PortHooker> hookers = new ArrayList<PortHooker>();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);

		List<Event> events = this.findEvents(Enums.SL, HookerType.PORT, query);
		for (Event event : events) {
			PortHooker hooker = new Gson().fromJson(event.field("json"), PortHooker.class);
			hooker.setId(event.id());
			hookers.add(hooker);
		}
		return hookers;
	}

	public List<Event> findEvents(MonkeyType monkeyType, EventType eventType, Map<String, String> query) {
		ArrayList<Object> args = new ArrayList<>();
		StringBuilder sqlquery = new StringBuilder(String.format("select * from %s where ", getTable()));

		sqlquery.append(String.format(" %s = ?", FIELD_MONKEY_TYPE));
		args.add(SimpleDBRecorder.enumToValue(monkeyType));

		sqlquery.append(String.format(" and %s = ?", FIELD_EVENT_TYPE));
		args.add(SimpleDBRecorder.enumToValue(eventType));

		for (Map.Entry<String, String> pair : query.entrySet()) {
			sqlquery.append(String.format(" and %s like \"%s\"", pair.getKey(), pair.getValue()));
		}

		LOGGER.debug(String.format("Query is '%s'", sqlquery));
		List<Event> events = this.getJdbcTemplate().query(sqlquery.toString(), args.toArray(), new RowMapper<Event>() {
			public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
				return mapEvent(rs);
			}
		});
		return events;
	}

	public Event mapEvent(ResultSet rs) throws SQLException {
		String json = rs.getString("dataJson");
		Event event = null;

		String id = rs.getString(FIELD_ID);
		MonkeyType monkeyType = SimpleDBRecorder.valueToEnum(MonkeyType.class, rs.getString(FIELD_MONKEY_TYPE));
		EventType eventType = SimpleDBRecorder.valueToEnum(EventType.class, rs.getString(FIELD_EVENT_TYPE));
		long time = rs.getLong(FIELD_EVENT_TIME);
		event = new BasicRecorderEvent(monkeyType, eventType, "", id, time);
		event.addField("json", json);

		return event;
	}

}
