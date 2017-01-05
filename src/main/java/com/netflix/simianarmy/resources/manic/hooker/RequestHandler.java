/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import console.mw.sl.service.schema.Request;

/**
 * @author dxiong
 *
 */
public interface RequestHandler {

	long DELAY_DEFAULT = 30000l;
	long TIMEOUT = 15*60*1000l;

	void onRequest(RequestContext context, Request request, String rawData);
}
