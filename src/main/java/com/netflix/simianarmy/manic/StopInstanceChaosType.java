/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.chaos.ChaosInstance;
import com.netflix.simianarmy.chaos.ShutdownInstanceChaosType;
import com.netflix.simianarmy.client.gcloud.BasicClient;

/**
 * @author dxiong
 *
 */
public class StopInstanceChaosType extends ShutdownInstanceChaosType {

	/**
	 * @param config
	 */
	public StopInstanceChaosType(MonkeyConfiguration config) {
		super(config);
	}

	public boolean canApply(ChaosInstance instance) {
		CloudClient cloudClient = instance.getCloudClient();
		String instanceId = instance.getInstanceId();

		if (cloudClient instanceof BasicClient) {
			try {
				BasicClient gceClient = (BasicClient) cloudClient;
				switch (gceClient.get(instanceId).getStatus()) {
				case RUNNING:
				case SUSPENDED:
					return true;
				default:
					return false;

				}
			} catch (IOException ex) {
				ex.printStackTrace();
				return super.canApply(instance);
			}
		} else {
			return super.canApply(instance);
		}
	}
}
