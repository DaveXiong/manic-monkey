/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import console.mw.sl.service.schema.TemplateResponse;

/**
 * @author dxiong
 *
 */
public class ProcessingHooker extends Hooker<Object, Object> {
	public ProcessingHooker(Hooker<?, ?> hooker) {
		this.setAction(hooker.getAction());
		this.setParameters(hooker.getParameters());

		TemplateResponse<Object> response = new TemplateResponse<Object>();
		response.setCode(100);
		response.setMessage("You request is processing");
		this.setResponse(response);
	}
}
