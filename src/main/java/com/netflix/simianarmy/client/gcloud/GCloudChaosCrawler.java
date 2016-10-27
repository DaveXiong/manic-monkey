/*
 *
 *  Copyright 2012 Netflix, Inc.
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
package com.netflix.simianarmy.client.gcloud;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import com.google.api.services.compute.model.Instance;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.chaos.ChaosCrawler;

/**
 * The Class ASGChaosCrawler. This will crawl for all available
 * AutoScalingGroups associated with the AWS account.
 */
public class GCloudChaosCrawler implements ChaosCrawler {

	/**
	 * The group types Types.
	 */
	public enum Types implements GroupType {

		/** only crawls AutoScalingGroups. */
		ASG;
	}

	/** The gce client. */
	private final GCloudClient client;

	/**
	 * Instantiates a new basic chaos crawler.
	 * 
	 * @param client
	 *            the aws client
	 */
	public GCloudChaosCrawler(GCloudClient client) {
		this.client = client;
	}

	/** {@inheritDoc} */
	@Override
	public EnumSet<?> groupTypes() {
		return EnumSet.allOf(Types.class);
	}

	/** {@inheritDoc} */
	@Override
	public List<InstanceGroup> groups() {
		return groups((String[]) null);
	}

	@Override
	public List<InstanceGroup> groups(String... names) {
		List<InstanceGroup> list = new LinkedList<InstanceGroup>();
		list.addAll(client.listGroups());

		for (InstanceGroup group : list) {
			for (Instance instance : client.listInstances(group)) {
				group.addInstance(instance.getName());
			}
		}

		return list;
	}

}
