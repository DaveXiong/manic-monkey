/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.util.List;
import java.util.Map;

import console.mw.sl.service.schema.Request;

/**
 * @author dxiong
 *
 */
public interface HookerSearch {
	enum INDEX{
		CUSTOMER,UUID,COMMAND,MESSAGEID
	}

	Hooker<?, ?> findBestHooker(List<Hooker<?, ?>> hookers, Request request, String rawMessage);

	Map<INDEX, String> parseIndexes(String rawMessage);
}
