/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import com.netflix.simianarmy.basic.LocalDbRecorder;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;
import com.netflix.simianarmy.manic.ManicBasicCalendar;

/**
 * @author dxiong
 *
 */
public interface Definitions {

	public interface GCloud {
		String[] CONFIG = new String[] { "simianarmy.properties", "client.properties", "chaos.properties" };
		String CRENDENTIAL = "simianarmy.client.gcloud.credential";
		String PROJECT = "simianarmy.client.gcloud.project";
		String ZONE = "simianarmy.client.gcloud.zone";
		String ZONE_DEFAULT = "us-east-1";
	}

	public interface Client {
		String CLASS = "simianarmy.client.class";
		String CLASS_DEFAULT = com.netflix.simianarmy.client.gcloud.BasicClient.class.getName();
	}

	public interface Scheduler {
		String FREQUEUE = "simianarmy.scheduler.frequency";
		int FREQUEUE_DEFAULT = 1;
		String FREQUEUE_UNIT = "simianarmy.scheduler.frequencyUnit";
		String FREQUEUE_UNIT_DEFAULT = "HOURS";
		String THREADS = "simianarmy.scheduler.threads";
		int THREADS_DEFAULT = 1;
	}

	public interface Recorder {
		String CLASS = "simianarmy.client.recorder.class";
		String CLASS_DEFAULT = LocalDbRecorder.class.getName();

		public interface RDS {
			String DRIVER = "simianarmy.recorder.db.driver";
			String URL = "simianarmy.recorder.db.url";
			String USER = "simianarmy.recorder.db.user";
			String PASSWORD = "simianarmy.recorder.db.pass";
			String TABLE = "simianarmy.recorder.db.table";
		}
	}

	public interface Calendar {
		String CLASS = "simianarmy.calendar.class";
		String CLASS_DEFAULT = ManicBasicCalendar.class.getName();
	}

	public interface InstanceSelector {
		String CLASS = "simianarmy.client.instance.selector.class";
		String CLASS_DEFAULT = BasicChaosInstanceSelector.class.getName();
	}

	public interface Notifier {
		public interface Email {
			String CLASS = "simianarmy.client.notifier.email.class";
			String CLASS_DEFAULT = DummyMonkyEmailNotifier.class.getName();
		}

		public interface SLACK {
			String URL = "simianarmy.client.notifier.slack.url";
			String CHANNEL = "simianarmy.client.notifier.slack.channel";
			String CHANNEL_DEFAULT = "#wfe-test";
			String AGENT = "simianarmy.client.notifier.slack.agent";
			String AGENT_DEFAULT = "Manic Monkey";
		}

	}

	public interface Crawler {
		String CLASS = "simianarmy.client.crawler.class";
		String CLASS_DEFAULT = BasicChaosCrawler.class.getName();
	}
}
