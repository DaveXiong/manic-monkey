/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import console.mw.sl.service.schema.AllocatePortPayload;
import console.mw.sl.service.schema.TemplateResponse;

/**
 * @author dxiong
 *
 */
public class ProcessingHooker extends PortHooker {
	public ProcessingHooker(PortHooker hooker){
		this.setAction(hooker.getAction());
		this.setParameters(hooker.getParameters());
		this.setRequest(hooker.getRequest());
		
		TemplateResponse<AllocatePortPayload> response = new TemplateResponse<AllocatePortPayload>();
		response.setCode(100);
		response.setMessage("You request is processing");
		this.setResponse(response);
	}
}
