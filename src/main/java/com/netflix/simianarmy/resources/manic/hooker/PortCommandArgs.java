/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import com.google.gson.annotations.SerializedName;

import console.mw.sl.service.schema.SwitchCommandArgs;

/**
 * @author dxiong
 *
 */
public class PortCommandArgs extends SwitchCommandArgs {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6862088379760957943L;

	@SerializedName("customer_id")
	private String customerId;

	@SerializedName("pop_uuid")
	private String popUuid;

	@SerializedName("port_speed")
	private Integer portSpeed;

	@SerializedName("is_nsp")
	private Boolean isNsp;

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getPopUuid() {
		return popUuid;
	}

	public void setPopUuid(String popUuid) {
		this.popUuid = popUuid;
	}

	public Integer getPortSpeed() {
		return portSpeed;
	}

	public void setPortSpeed(Integer portSpeed) {
		this.portSpeed = portSpeed;
	}

	public Boolean getIsNsp() {
		return isNsp;
	}

	public void setIsNsp(Boolean isNsp) {
		this.isNsp = isNsp;
	}
	
	
}
