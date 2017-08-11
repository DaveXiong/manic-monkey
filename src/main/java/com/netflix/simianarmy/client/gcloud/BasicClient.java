/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import java.util.List;
import java.util.Map;

import org.jclouds.compute.ComputeService;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.NotFoundException;

/**
 * @author dxiong
 * 
 */
public class BasicClient extends Gce implements CloudClient {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicClient.class);

	public BasicClient(MonkeyConfiguration configuration) throws Exception {
		super(configuration.getStr(Definitions.GCloud.CRENDENTIAL), configuration.getStr(Definitions.GCloud.PROJECT),
				configuration.getStrOrElse(Definitions.GCloud.ZONE, Definitions.GCloud.ZONE_DEFAULT));
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
			this.stop(instanceId);
		} catch (Exception e) {
			throw new NotFoundException(e);
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
		throw new UnsupportedOperationException("deleteAutoScalingGroup");
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
		throw new UnsupportedOperationException("deleteLaunchConfiguration");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteVolume(java.lang.String)
	 */
	@Override
	public void deleteVolume(String volumeId) {
		throw new UnsupportedOperationException("deleteVolume");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteSnapshot(java.lang.String)
	 */
	@Override
	public void deleteSnapshot(String snapshotId) {
		throw new UnsupportedOperationException("deleteSnapshot");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteImage(java.lang.String)
	 */
	@Override
	public void deleteImage(String imageId) {
		throw new UnsupportedOperationException("deleteImage");

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
		throw new UnsupportedOperationException("deleteElasticLoadBalancer");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#deleteDNSRecord(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteDNSRecord(String dnsName, String dnsType, String hostedZoneID) {
		throw new UnsupportedOperationException("deleteDNSRecord");

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
		throw new UnsupportedOperationException("createTagsForResources");

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
		throw new UnsupportedOperationException("listAttachedVolumes");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#detachVolume(java.lang.String,
	 * java.lang.String, boolean)
	 */
	@Override
	public void detachVolume(String instanceId, String volumeId, boolean force) {
		throw new UnsupportedOperationException("detachVolume");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#getJcloudsComputeService()
	 */
	@Override
	public ComputeService getJcloudsComputeService() {
		throw new UnsupportedOperationException("getJcloudsComputeService");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#getJcloudsId(java.lang.String)
	 */
	@Override
	public String getJcloudsId(String instanceId) {
		throw new UnsupportedOperationException("getJcloudsId");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netflix.simianarmy.CloudClient#connectSsh(java.lang.String,
	 * org.jclouds.domain.LoginCredentials)
	 */
	@Override
	public SshClient connectSsh(String instanceId, LoginCredentials credentials) {
		throw new UnsupportedOperationException("connectSsh");

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
		throw new UnsupportedOperationException("findSecurityGroup");

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
		throw new UnsupportedOperationException("createSecurityGroup");

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
		throw new UnsupportedOperationException("canChangeInstanceSecurityGroups");

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
		throw new UnsupportedOperationException("setInstanceSecurityGroups");
	}
}
