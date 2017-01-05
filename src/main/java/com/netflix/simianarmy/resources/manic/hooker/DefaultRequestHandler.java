/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import console.mw.sl.service.schema.Command;
import console.mw.sl.service.schema.Request;
import console.mw.sl.service.schema.Response;

/**
 * @author dxiong
 *
 */
public class DefaultRequestHandler implements RequestHandler {

	final Logger LOGGER = LogManager.getLogger(this.getClass());

	private SLResourceRecorder recorder;

	private Map<HookerType, HookerSearch> type2HookerSearch;

	private Map<Command, Hooker<?, ?>> defaultHookers = new HashMap<Command, Hooker<?, ?>>();

	private static final Timer TIMER = new Timer();

	/**
	 * 
	 */
	public DefaultRequestHandler(SLResourceRecorder recorder, Map<HookerType, HookerSearch> type2HookerSearch) {
		this.recorder = recorder;
		this.type2HookerSearch = type2HookerSearch;

		defaultHookers.put(Command.PING, new PingHooker());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.resources.manic.hooker.RequestHandler#onRequest(
	 * com.netflix.simianarmy.resources.manic.hooker.RequestContext,
	 * console.mw.sl.service.schema.Request, java.lang.String)
	 */
	@Override
	public void onRequest(final RequestContext context, final Request request, final String rawData) {
		String command = request.getCommand().toString();

		HookerType hookerType = null;
		if (command.startsWith("PORT")) {
			hookerType = HookerType.PORT;
		} else if (command.startsWith("CONNECTION")) {
			hookerType = HookerType.CONNECTION;
		}

		final HookerType type = hookerType;
		final Hooker<?, ?> hooker = findBestHooker(type, request, rawData);

		if (hooker == null) {
			context.disable();
			try {
				context.send(rawData);
			} finally {
				context.enable();
			}
			return;
		}

		switch (hooker.getAction()) {
		case DEFAULT:
			doIt(context, hooker, request.getId());
			break;

		case MANUAL:
			final long start = System.currentTimeMillis();
			class Task extends TimerTask {

				@Override
				public void run() {
					Hooker<?, ?> portHooker = recorder.getHookerByMessageId(type, request.getId());
					if (portHooker == null) {
						if ((System.currentTimeMillis() - start) < TIMEOUT) {
							doIt(context, new ProcessingHooker(), request.getId());
							TIMER.schedule(new Task(), DELAY_DEFAULT);
						} else {
							doIt(context, new InternalErrorHooker(), request.getId());
						}
					} else {
						doIt(context, portHooker, request.getId());
					}

				}

			}
			TIMER.schedule(new Task(), DELAY_DEFAULT);
			break;
		case DELAY:
			Long delay = (Long) hooker.getParameters().get(Hooker.PARAMETER_DELAY);
			if (delay == null) {
				delay = DELAY_DEFAULT;
			}
			TIMER.schedule(new TimerTask() {
				@Override
				public void run() {
					doIt(context, hooker, request.getId());
				}
			}, delay);
			break;
		case RANDOM:
			doIt(context, new InternalErrorHooker(), request.getId());
			break;
		}

	}

	protected void doIt(RequestContext context, Hooker<?, ?> hook, String id) {
		hook.getResponse().setId(id);
		String message = hook.getResponse().getMessage() == null ? "" : hook.getResponse().getMessage();
		hook.getResponse().setMessage("[MONKEY]" + message);
		context.broadcast(new Gson().toJson(hook.getResponse()));

		LOGGER.info("response>>" + new Gson().toJson(hook.getResponse()));
	}

	protected void doIt(RequestContext context, Response response, String id) {
		response.setId(id);
		response.setMessage("[Monkey]" + response.getMessage());
		context.broadcast(new Gson().toJson(response));
		LOGGER.info("response>>" + new Gson().toJson(response));
	}

	protected Hooker<?, ?> findBestHooker(HookerType type, Request request, String rawMessage) {
		HookerSearch search = type2HookerSearch.get(type);

		Hooker<?, ?> hooker = null;
		if (search != null) {
			hooker = search.findBestHooker(recorder.getHookers(type, request.getCommand()), request, rawMessage);
		}

		LOGGER.info("Best Hooker for " + request.getCommand() + "," + hooker);

		if (hooker == null) {
			hooker = defaultHookers.get(request.getCommand());
		}

		return hooker;
	}

}
