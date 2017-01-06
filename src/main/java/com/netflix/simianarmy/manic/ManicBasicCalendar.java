/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.util.TimeZone;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicCalendar;

/**
 * @author dxiong
 *
 */
public class ManicBasicCalendar extends BasicCalendar {

	private boolean monkeyIsRunning = false;

	/**
	 * @param cfg
	 */
	public ManicBasicCalendar(MonkeyConfiguration cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param open
	 * @param close
	 * @param timezone
	 */
	public ManicBasicCalendar(int open, int close, TimeZone timezone) {
		super(open, close, timezone);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cfg
	 * @param open
	 * @param close
	 * @param timezone
	 */
	public ManicBasicCalendar(MonkeyConfiguration cfg, int open, int close, TimeZone timezone) {
		super(cfg, open, close, timezone);
		// TODO Auto-generated constructor stub
	}

	public boolean isMonkeyTime(Monkey monkey) {
		boolean isMonkeyTime = super.isMonkeyTime(monkey);

		if (monkeyIsRunning != isMonkeyTime) {
			ManicChaosMonkey manicMonkey = (ManicChaosMonkey) monkey;

			manicMonkey.monkeyTimeChanged(isMonkeyTime,this.openHour(),this.closeHour());

			monkeyIsRunning = isMonkeyTime;
		}
		return isMonkeyTime;
	}
}
