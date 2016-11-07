/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.services.compute.model.Tags;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.gcloud.BasicChaosCrawler.Types;
import com.netflix.simianarmy.client.gcloud.BasicClient;

/**
 * @author dxiong
 *
 */
public class ManicGceClient extends BasicClient {

	private List<com.google.api.services.compute.model.Instance> cache = new ArrayList<com.google.api.services.compute.model.Instance>();
	private long cacheUpdatedAt = System.currentTimeMillis();

	private static final long CACHE_UPDATED_THTREDHOLD = 1 * 60 * 60 * 1000;// 1minute

	private synchronized void updateCache() throws IOException {
		updateCache(false);
	}

	private synchronized void updateCache(boolean force) throws IOException {
		if (force || cache.isEmpty() || (System.currentTimeMillis() - cacheUpdatedAt) >= CACHE_UPDATED_THTREDHOLD) {
			cache.clear();
			cache.addAll(internalList());
		}
	}

	private synchronized List<com.google.api.services.compute.model.Instance> getInstances() {
		List<com.google.api.services.compute.model.Instance> instances = new ArrayList<com.google.api.services.compute.model.Instance>();
		instances.addAll(cache);
		return instances;
	}

	/**
	 * @param configuration
	 * @throws Exception
	 */
	public ManicGceClient(MonkeyConfiguration configuration) throws Exception {
		super(configuration);
	}

	public List<InstanceGroup> listGroups() throws IOException {

		updateCache(true);

		List<InstanceGroup> groups = new ArrayList<InstanceGroup>();

		Set<String> tags = new HashSet<String>();

		for (com.google.api.services.compute.model.Instance instance : getInstances()) {
			Tags tag = instance.getTags();
			if (tag != null && tag.getItems() != null)
				tags.addAll(tag.getItems());
		}

		for (String tag : tags) {
			groups.add(new BasicInstanceGroup(tag, Types.TAG, zone, null));
		}

		return groups;

	}

	public List<Instance> list(String group) throws IOException {

		updateCache();

		List<Instance> instances = new ArrayList<Instance>();

		for (com.google.api.services.compute.model.Instance instance : getInstances()) {
			Tags tag = instance.getTags();
			if (tag != null && tag.getItems() != null) {
				if (instance.getTags().getItems().contains(group)) {
					instances.add(new Instance(instance.getId().longValue(), instance.getName(),
							Status.parse(instance.getStatus())));
				}
			}
		}

		return instances;
	}
}
