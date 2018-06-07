package com.neusoft.aclome.nilebeat;

import com.google.inject.Guice;
import com.neusoft.aclome.nilebeat.app.NileBeatApplication;
import com.neusoft.aclome.nilebeat.guice.AkkaModule;

public class StartSystem {

	public static void main(String[] args) throws Exception {
		
		Guice
			.createInjector(new AkkaModule())
			.getInstance(NileBeatApplication.class)
			.run();
    }
}
