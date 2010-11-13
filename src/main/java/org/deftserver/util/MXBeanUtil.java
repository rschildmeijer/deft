package org.deftserver.util;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MXBeanUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(MXBeanUtil.class);
	
	private MXBeanUtil() {}

	public static void registerMXBean(Object self, String mbeanName) {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(self, new ObjectName(mbeanName));
		}
		catch (Exception e) {
			logger.error("Unable to register {} MXBean: {}", self.getClass().getCanonicalName(), e);
		}
	}

}
