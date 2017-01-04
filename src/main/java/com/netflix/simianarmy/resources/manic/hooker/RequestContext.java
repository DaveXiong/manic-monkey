/**
 * 
 */
package com.netflix.simianarmy.resources.manic.hooker;

/**
 * @author dxiong
 *
 */
public interface RequestContext {
	void disableConsume();

	void enableConsume();

	void send(String message);

	void broadcast(String message);

}
