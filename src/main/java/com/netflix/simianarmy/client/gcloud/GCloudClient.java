/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jclouds.compute.ComputeService;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.compute.model.Instance;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.client.gcloud.GCloudChaosCrawler.Types;

/**
 * @author dxiong
 * 
 */
public class GCloudClient implements CloudClient {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(AWSClient.class);

	/** The region. */
	private final String zone;

	private final String project;

	private final String credential;

	public GCloudClient(String project, String zone, String credential) {
		this.project = project;
		this.zone = zone;
		this.credential = credential;
	}

	public String region() {
		return zone;
	}

	public List<Instance> listInstances() {
		try {
			return client().list(project, zone);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<Instance> listInstances(InstanceGroup group) {
		return listInstances();
	}

	public List<InstanceGroup> listGroups() {
		List<InstanceGroup> list = new LinkedList<InstanceGroup>();

		list.add(new BasicInstanceGroup("default", Types.ASG, zone, null));
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#terminateInstance(java.lang.String)
	 */
	@Override
	public void terminateInstance(String instanceId) {
		LOGGER.info("terminate instance:" + instanceId);
		try {
			client().stop(project, zone, instanceId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteAutoScalingGroup(java.lang.
	 * String )
	 */
	@Override
	public void deleteAutoScalingGroup(String asgName) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#deleteLaunchConfiguration(java.lang
	 * .String)
	 */
	@Override
	public void deleteLaunchConfiguration(String launchConfigName) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteVolume(java.lang.String)
	 */
	@Override
	public void deleteVolume(String volumeId) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteSnapshot(java.lang.String)
	 */
	@Override
	public void deleteSnapshot(String snapshotId) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteImage(java.lang.String)
	 */
	@Override
	public void deleteImage(String imageId) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#deleteElasticLoadBalancer(java.lang
	 * .String)
	 */
	@Override
	public void deleteElasticLoadBalancer(String elbId) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteDNSRecord(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteDNSRecord(String dnsName, String dnsType, String hostedZoneID) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#createTagsForResources(java.util.Map,
	 * java.lang.String[])
	 */
	@Override
	public void createTagsForResources(Map<String, String> keyValueMap, String... resourceIds) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#listAttachedVolumes(java.lang.String,
	 * boolean)
	 */
	@Override
	public List<String> listAttachedVolumes(String instanceId, boolean includeRoot) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#detachVolume(java.lang.String,
	 * java.lang.String, boolean)
	 */
	@Override
	public void detachVolume(String instanceId, String volumeId, boolean force) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#getJcloudsComputeService()
	 */
	@Override
	public ComputeService getJcloudsComputeService() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#getJcloudsId(java.lang.String)
	 */
	@Override
	public String getJcloudsId(String instanceId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#connectSsh(java.lang.String,
	 * org.jclouds.domain.LoginCredentials)
	 */
	@Override
	public SshClient connectSsh(String instanceId, LoginCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#findSecurityGroup(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public String findSecurityGroup(String instanceId, String groupName) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#createSecurityGroup(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String createSecurityGroup(String instanceId, String groupName, String description) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#canChangeInstanceSecurityGroups(java
	 * .lang.String)
	 */
	@Override
	public boolean canChangeInstanceSecurityGroups(String instanceId) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netflix.simianarmy.CloudClient#setInstanceSecurityGroups(java.lang
	 * .String, java.util.List)
	 */
	@Override
	public void setInstanceSecurityGroups(String instanceId, List<String> groupIds) {
		// TODO Auto-generated method stub

	}

	public GCloud client() {
		return new GCloud(this.credential);
	}

	@Override
	public void startInstance(String instanceId) {
		try {
			client().start(project, zone, instanceId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public CLIENT_STATUS getInstanceStatus(String instanceId) {
		try {
			return CLIENT_STATUS.parse(client().get(project, zone, instanceId).getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return CLIENT_STATUS.UNKNOWN;
		}
	}

	public Instance getInstance(String instanceId) {
		try {
			return client().get(project, zone, instanceId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
