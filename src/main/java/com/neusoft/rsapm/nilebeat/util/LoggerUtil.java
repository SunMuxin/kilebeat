package com.neusoft.rsapm.nilebeat.util;

import java.util.Optional;

public class LoggerUtil {
		
	public static enum Level {
		INFO, ERROR, WARN, DEBUG, SEVERE, POST, HEAD, GET
	}
	
	public static Optional<String> parseLevel(String syslog) {
		String level = null;
		if (syslog.contains(Level.INFO.toString())) {
			level = Level.INFO.toString();
		} else if (syslog.contains(Level.ERROR.toString())) {
			level = Level.ERROR.toString();
		} else if (syslog.contains(Level.WARN.toString())) {
			level = Level.WARN.toString();
		} else if (syslog.contains(Level.DEBUG.toString())) {
			level = Level.DEBUG.toString();
		} else if (syslog.contains(Level.POST.toString())) {
			level = Level.POST.toString();
		} else if (syslog.contains(Level.HEAD.toString())) {
			level = Level.HEAD.toString();
		} else if (syslog.contains(Level.GET.toString())) {
			level = Level.GET.toString();
		} else if (syslog.contains(Level.SEVERE.toString())) {
			level = Level.SEVERE.toString();
		}
		return Optional.ofNullable(level);
	}

	public static Optional<String> parseContent(String syslog) {
		String context = syslog;
		StringMaker stringMaker = new StringMaker(syslog);
		if (syslog.contains(Level.INFO.toString())) {
			context = stringMaker.after(Level.INFO.toString()).value().trim();
		} else if (syslog.contains(Level.ERROR.toString())) {
			context = stringMaker.after(Level.ERROR.toString()).value().trim();
		} else if (syslog.contains(Level.WARN.toString())) {
			context = stringMaker.after(Level.WARN.toString()).value().trim();
		} else if (syslog.contains(Level.DEBUG.toString())) {
			context = stringMaker.after(Level.DEBUG.toString()).value().trim();
		} else if (syslog.contains(Level.POST.toString())) {
			context = stringMaker.after(Level.POST.toString()).value().trim();
		} else if (syslog.contains(Level.HEAD.toString())) {
			context = stringMaker.after(Level.HEAD.toString()).value().trim();
		} else if (syslog.contains(Level.GET.toString())) {
			context = stringMaker.after(Level.GET.toString()).value().trim();
		} else if (syslog.contains(Level.SEVERE.toString())) {
			context = stringMaker.after(Level.SEVERE.toString()).value().trim();
		}
		return Optional.ofNullable(context);
	}
}
