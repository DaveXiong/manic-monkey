/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.util.List;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.gcloud.BasicClient;

/**
 * @author dxiong
 *
 */
public class ManicGceClient extends BasicClient {

	private InstanceMonitor monitor;

	/**
	 * @param configuration
	 * @throws Exception
	 */
	public ManicGceClient(MonkeyConfiguration configuration) throws Exception {
		super(configuration);
	}

	public List<InstanceGroup> listGroups() throws IOException {
		return this.monitor.groups();
	}

	public List<Instance> list(String group) throws IOException {
		return this.monitor.instances(group);
	}

	public void setMonitor(InstanceMonitor monitor) {
		this.monitor = monitor;
	}
}
