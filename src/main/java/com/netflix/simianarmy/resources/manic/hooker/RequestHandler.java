/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import console.mw.sl.service.schema.Request;
import console.mw.sl.service.schema.Response;

/**
 * @author dxiong
 *
 */
public interface RequestHandler {

	int DELAY_DEFAULT = 30000;

	Response onRequest(RequestContext context, Request request, String rawData);
}
