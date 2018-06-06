package com.neusoft.aclome.kilebeat;

import com.google.inject.Guice;
import com.neusoft.aclome.kilebeat.app.KileBeatApplication;
import com.neusoft.aclome.kilebeat.guice.AkkaModule;

public class StartSystem {

	public static void main(String[] args) throws Exception {
		
		Guice
			.createInjector(new AkkaModule())
			.getInstance(KileBeatApplication.class)
			.run();
    }
}
