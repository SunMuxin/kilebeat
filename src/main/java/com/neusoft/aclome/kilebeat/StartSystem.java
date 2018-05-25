package com.neusoft.aclome.kilebeat;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Guice;
import com.neusoft.aclome.kilebeat.guice.KileModule;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class StartSystem {
		
	public static void main(String[] args) throws Exception {
				
		Path root = Paths.get(System.getProperty("user.dir"));
		System.getProperties().setProperty("config.file", 
				Paths.get(root.toString(), "confs").toString()+"\\*");
		
		LOGGER.info("config.file = " + System.getProperty("config.file"));
		
		Guice
			.createInjector(new KileModule())
			.getInstance(KileManager.class)
			.run();
    }
}
