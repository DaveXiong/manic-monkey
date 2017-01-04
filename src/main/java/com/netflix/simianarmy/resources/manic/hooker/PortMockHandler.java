/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import console.mw.sl.service.schema.AllocatePortPayload;
import console.mw.sl.service.schema.Request;
import console.mw.sl.service.schema.Response;
import console.mw.sl.service.schema.TemplateRequest;
import console.mw.sl.service.schema.TemplateResponse;

/**
 * @author dxiong
 *
 */
public class PortMockHandler implements RequestHandler {

	private static final Logger LOGGER = LogManager.getLogger(PortMockHandler.class);

	private SLResourceRecorder recorder;

	private static final Timer TIMER = new Timer();

	/**
	 * 
	 */
	public PortMockHandler(SLResourceRecorder recorder) {
		this.recorder = recorder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.resources.manic.SL.RequestHandler#onRequest(
	 * console.mw.sl.service.schema.Request, java.lang.String)
	 */
	@Override
	public Response onRequest(final RequestContext context, final Request request, final String rawData) {

		LOGGER.info("Request:" + new Gson().toJson(request));
		Type type = new TypeToken<TemplateRequest<PortCommandArgs>>() {
		}.getType();

		TemplateRequest<PortCommandArgs> portRequest = new Gson().fromJson(rawData, type);

		final PortHooker hooker = findBestHooker(this.recorder.getPortHookers(request.getCommand()),
				portRequest.getCommandArgs().getCustomerId(), portRequest.getCommandArgs().getPortUuid());

		if (hooker == null) {
			context.disableConsume();
			try {
				context.send(rawData);
			} finally {
				context.enableConsume();
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

	private void doIt(RequestContext context, PortHooker hook, String id) {
		TemplateResponse<AllocatePortPayload> response = hook.getResponse();
		response.setId(id);
		response.setMessage("[MONKEY]" + (response.getMessage() == null ? "" : response.getMessage()));
		context.broadcast(new Gson().toJson(response));
	}

	private PortHooker findBestHooker(List<PortHooker> hookers, String customerId, String uuid) {

		PortHooker bestOne = null;
		/**
		 * matchUUID:1
		 * <p>
		 * matchCustomerId:2
		 * <p>
		 * matchCustomerId&Uuid:3
		 */
		int matchPoint = 0;

		if (hookers != null) {
			for (PortHooker hooker : hookers) {
				PortCommandArgs commandArgs = hooker.getRequest().getCommandArgs();
				if (commandArgs != null) {

					if (commandArgs.getCustomerId() == null) {
						if (commandArgs.getPortUuid() == null) {
							if (bestOne == null) {
								bestOne = hooker;
							}
						} else if (commandArgs.getPortUuid().equalsIgnoreCase(uuid)) {
							if (bestOne == null || matchPoint < 1) {
								bestOne = hooker;
								matchPoint = 1;
							}
						}
					} else if (commandArgs.getCustomerId().equalsIgnoreCase(customerId)) {
						if (commandArgs.getPortUuid() == null) {
							if (bestOne == null || matchPoint < 2) {
								bestOne = hooker;
								matchPoint = 2;
							}
						} else if (commandArgs.getPortUuid().equalsIgnoreCase(uuid)) {
							if (bestOne == null || matchPoint < 3) {
								bestOne = hooker;
								matchPoint = 3;
							}
						}
					}
				} else {
					if (bestOne == null) {
						bestOne = hooker;
					}
				}
			}
		}
		LOGGER.info("Best Hooker:" + bestOne);
		return bestOne;
	}

}
