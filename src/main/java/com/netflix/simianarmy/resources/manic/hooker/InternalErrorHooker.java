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
public class InternalErrorHooker extends Hooker<Object, Object> {
	public InternalErrorHooker() {
		TemplateResponse<Object> response = new TemplateResponse<Object>();
		response.setCode(500);
		response.setMessage("User didn't take actions");
		this.setResponse(response);
		
		TemplateRequest<Object> request = new TemplateRequest<Object>();
		request.setCommand(Command.PING);
		this.setRequest(request);
	}
}
