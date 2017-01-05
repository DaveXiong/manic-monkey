/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

import com.netflix.simianarmy.EventType;

/**
 * @author dxiong
 *
 */
public enum HookerType implements EventType {
	PORT, CONNECTION, CR, PEER, AWS;

	public static HookerType parse(String type){
		for(HookerType hookerType:HookerType.values()){
			if(hookerType.toString().equalsIgnoreCase(type)){
				return hookerType;
			}
		}
		return null;
	}
}
