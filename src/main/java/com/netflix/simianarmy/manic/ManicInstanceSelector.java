/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosInstance;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.chaos.SshConfig;

/**
 * @author dxiong
 *
 */
public class ManicInstanceSelector {

	private static final Logger LOGGER = LoggerFactory.getLogger(ManicInstanceSelector.class);

	private static final Random RANDOM = new Random();

	private ManicChaosMonkey manicMonkey;

	public ManicInstanceSelector(ManicChaosMonkey manicMonkey) {
		this.manicMonkey = manicMonkey;
	}

	public Collection<String> select(InstanceGroup group, ChaosType chaosType) {

		List<String> results = new ArrayList<String>();

		String genericKey = "simianarmy.chaos." + chaosType.getKey() + ".minInstances";
		String specificKey = "simianarmy.chaos.TAG." + group.name() + "." + chaosType.getKey() + ".minInstances";
		int minActiveInstance = (int) manicMonkey.context().configuration().getNumOrElse(specificKey.toLowerCase(),
				manicMonkey.context().configuration().getNumOrElse(genericKey.toLowerCase(), 0));

		List<String> activeInstances = new ArrayList<String>();
		SshConfig sshConfig = new SshConfig(manicMonkey.context().configuration());

		for (String instanceId : group.instances()) {
			ChaosInstance instance = new ChaosInstance(manicMonkey.context().cloudClient(), instanceId, sshConfig);
			if (chaosType.canApply(instance)) {
				activeInstances.add(instanceId);
			}
		}

		int max2Selected = activeInstances.size() - minActiveInstance;

		if (max2Selected > 0) {
			results.add(activeInstances.get(RANDOM.nextInt(activeInstances.size())));
		} else {
			LOGGER.info("group {} only has {} avaliable instances, {} avaliable instances are required", group.name(),
					activeInstances.size(), minActiveInstance);
		}

		return results;
	}
}
