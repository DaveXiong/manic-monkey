/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

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

	private static final Timer TIMER = new Timer();

	/**
	 * 
	 */
	public DefaultRequestHandler(SLResourceRecorder recorder, Map<HookerType, HookerSearch> type2HookerSearch) {
		this.recorder = recorder;
		this.type2HookerSearch = type2HookerSearch;
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
	public Response onRequest(final RequestContext context, final Request request, final String rawData) {
		String command = request.getCommand().toString();

		HookerType type = null;
		if (command.startsWith("PORT")) {
			type = HookerType.PORT;
		}

		final Hooker<?, ?> hooker = findBestHooker(type, request, rawData);
		;
		if (hooker == null) {
			context.disable();
			try {
				context.send(rawData);
			} finally {
				context.enable();
			}
			return null;
		}

		Response response = null;

		switch (hooker.getAction()) {
		case DEFAULT:
			response = hooker.getResponse();
			response.setId(request.getId());
			break;

		case MANUAL:
			class Task extends TimerTask {

				@Override
				public void run() {
					PortHooker portHooker = recorder.getPortHookerByMessageId(request.getId());
					if (portHooker == null) {
						doIt(context, new ProcessingHooker(hooker), request.getId());
						TIMER.schedule(new Task(), DELAY_DEFAULT);
					} else {
						doIt(context, portHooker, request.getId());
					}

				}

			}
			TIMER.schedule(new Task(), DELAY_DEFAULT);
			break;
		case DELAY:
			Integer delay = (Integer) hooker.getParameters().get("delay");
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
			response = new Response();
			response.setCode(500);
			response.setMessage("Internal error");
			response.setId(request.getId());
		}
		return response;
	}

	protected void doIt(RequestContext context, Hooker<?, ?> hook, String id) {
		hook.getResponse().setId(id);
		String message = hook.getResponse().getMessage() == null ? "" : hook.getResponse().getMessage();
		hook.getResponse().setMessage("[MONKEY]" + message);
		context.broadcast(new Gson().toJson(hook.getResponse()));
	}

	protected Hooker<?, ?> findBestHooker(HookerType type, Request request, String rawMessage) {
		HookerSearch search = type2HookerSearch.get(type);

		if (search != null) {
			return search.findBestHooker(recorder.getHookers(type), request, rawMessage);
		}
		return null;
	}

}
