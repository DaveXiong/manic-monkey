/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import console.mw.sl.service.schema.Command;
import console.mw.sl.service.schema.TemplateRequest;
import console.mw.sl.service.schema.TemplateResponse;

/**
 * @author dxiong
 *
 */
public class PingHooker extends Hooker<Object, Object> {
	public PingHooker() {
		TemplateResponse<Object> response = new TemplateResponse<Object>();
		response.setCode(200);
		response.setMessage("Pong");
		this.setResponse(response);
		
		TemplateRequest<Object> request = new TemplateRequest<Object>();
		request.setCommand(Command.PING);
		this.setRequest(request);
	}
}
