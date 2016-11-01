/*
 *
 *  Copyright 2013 Justin Santa Barbara.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.chaos.ChaosInstance;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.client.gcloud.BasicClient;

/**
 * Shuts down the instance using the cloud instance-termination API.
 *
 * This is the classic chaos-monkey strategy.
 */
public class StartInstanceChaosType extends ChaosType {
	/**
	 * Constructor.
	 *
	 * @param config
	 *            Configuration to use
	 */
	public StartInstanceChaosType(MonkeyConfiguration config) {
		super(config, "StartInstance");
	}

	public boolean canApply(ChaosInstance instance) {
		CloudClient cloudClient = instance.getCloudClient();
		String instanceId = instance.getInstanceId();

		if (cloudClient instanceof BasicClient) {
			try {
				BasicClient gceClient = (BasicClient) cloudClient;
				switch (gceClient.get(instanceId).getStatus()) {
				case TERMINATED:
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

	/**
	 * Shuts down the instance.
	 */
	@Override
	public void apply(ChaosInstance instance) {
		CloudClient cloudClient = instance.getCloudClient();
		String instanceId = instance.getInstanceId();

		try {
			Method method = cloudClient.getClass().getMethod("start", String.class);
			if (method != null) {
				method.invoke(cloudClient, instanceId);
			}
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
