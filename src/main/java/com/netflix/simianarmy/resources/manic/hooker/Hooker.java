/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import java.util.Map;

import console.mw.sl.service.schema.TemplateRequest;
import console.mw.sl.service.schema.TemplateResponse;

/**
 * @author dxiong
 *
 */
public class Hooker<T, R> {

	private String id;

	private HookerType type = HookerType.PORT;

	private HookerAction action = HookerAction.DEFAULT;

	private Map<String, Object> parameters;

	private TemplateRequest<T> request;

	private TemplateResponse<R> response;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public HookerType getType() {
		return type;
	}

	public void setType(HookerType type) {
		this.type = type;
	}

	public HookerAction getAction() {
		return action;
	}

	public void setAction(HookerAction action) {
		this.action = action;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public TemplateRequest<T> getRequest() {
		return request;
	}

	public void setRequest(TemplateRequest<T> request) {
		this.request = request;
	}

	public TemplateResponse<R> getResponse() {
		return response;
	}

	public void setResponse(TemplateResponse<R> response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "Hooker [action=" + action + ", parameters=" + parameters + ", request=" + request + ", response="
				+ response + "]";
	}

}
