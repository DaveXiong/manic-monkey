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
public class ProcessingHooker extends Hooker<Object, Object> {
	public ProcessingHooker() {
		TemplateResponse<Object> response = new TemplateResponse<Object>();
		response.setCode(100);
		response.setMessage("You request is processing");
		this.setResponse(response);
		
		TemplateRequest<Object> request = new TemplateRequest<Object>();
		request.setCommand(Command.PING);
		this.setRequest(request);
	}
}
