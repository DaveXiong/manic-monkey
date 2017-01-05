/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

/**
 * @author dxiong
 *
 */
public interface RequestContext {
	void disable();

	void enable();

	void send(String message);

	void broadcast(String message);

}
