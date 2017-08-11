package com.netflix.simianarmy.client.gcloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.gcloud.Gce.Instance;

/**
 * The Class BasicChaosCrawler.
 */
public class BasicChaosCrawler implements ChaosCrawler {

	private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosCrawler.class);

	/**
	 * The group types Types.
	 */
	public enum Types implements GroupType {
		ASG, TAG
	}

	/** The gce client. */
	private final BasicClient client;

	/**
	 * Instantiates a new basic chaos crawler.
	 * 
	 * @param client
	 *            the aws client
	 */
	public BasicChaosCrawler(BasicClient client) {
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

		List<String> groupNames = new ArrayList<String>();
		if (names != null)
			groupNames.addAll(Arrays.asList(names));
		try {
			list.addAll(client.listGroups());

			for (InstanceGroup group : list) {
				if (groupNames.isEmpty() || groupNames.contains(group.name())) {
					try {
						for (Instance instance : client.list(group.name())) {
							if (!group.instances().contains(instance.getName())) {
								group.addInstance(instance.getName());
							}
						}
					} catch (IOException ex) {
						LOGGER.error(ex.getMessage());
					}
				}
			}

		} catch (IOException ex) {
			LOGGER.error(ex.getMessage());
		}
		return list;
	}
}
