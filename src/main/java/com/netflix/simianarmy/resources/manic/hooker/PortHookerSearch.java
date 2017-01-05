/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import console.mw.sl.service.schema.AllocatePortCommandArgs;
import console.mw.sl.service.schema.AllocatePortPayload;
import console.mw.sl.service.schema.Request;

/**
 * @author dxiong
 *
 */
public class PortHookerSearch implements HookerSearch {

	private Hooker<?, ?> findBestHooker(List<Hooker<?, ?>> hookers, String customerId, String uuid) {

		Hooker<?, ?> bestOne = null;
		/**
		 * matchUUID:1
		 * <p>
		 * matchCustomerId:2
		 * <p>
		 * matchCustomerId&Uuid:3
		 */
		int matchPoint = 0;

		if (hookers != null) {
			for (Hooker<?, ?> hooker : hookers) {
				AllocatePortCommandArgs commandArgs = (AllocatePortCommandArgs) hooker.getRequest().getCommandArgs();
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
		return bestOne;
	}

	@Override
	public Hooker<?, ?> findBestHooker(List<Hooker<?, ?>> hookers, Request request, String rawMessage) {

		Map<INDEX, String> indexes = parseIndexes(rawMessage);

		return findBestHooker(hookers, indexes.get(INDEX.CUSTOMER), indexes.get(INDEX.UUID));
	}

	@Override
	public Map<INDEX, String> parseIndexes(String rawMessage) {
		Map<INDEX, String> index = new HashMap<INDEX, String>();

		Type type = new TypeToken<Hooker<AllocatePortCommandArgs, AllocatePortPayload>>() {
		}.getType();

		Hooker<AllocatePortCommandArgs, AllocatePortPayload> portHooker = new Gson().fromJson(rawMessage, type);

		AllocatePortCommandArgs commandArgs = portHooker.getRequest().getCommandArgs();

		String customerId = null;
		String uuid = null;

		if (commandArgs != null) {
			customerId = commandArgs.getCustomerId();
			uuid = commandArgs.getPortUuid();
		}

		index.put(INDEX.COMMAND, portHooker.getRequest().getCommand().toString());
		index.put(INDEX.COMMAND, customerId);
		index.put(INDEX.UUID, uuid);
		index.put(INDEX.MESSAGEID, portHooker.getRequest().getId());

		return index;
	}

}
