package com.neusoft.rsapm.nilebeat.service;

import java.util.UUID;

public class ActorNamesFactory {
	
	public static String tailer(){
		return "tailer" + rand();
	}
	
	public static String kilebeat(){
		return "kile" + rand();
	}
	
	public static String scanner(){
		return "scanner" + rand();
	}
	
	private static String rand() {
		return UUID.randomUUID().toString();
	}

}
