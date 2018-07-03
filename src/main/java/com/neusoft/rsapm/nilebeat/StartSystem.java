package com.neusoft.rsapm.nilebeat;

import com.google.inject.Guice;
import com.neusoft.rsapm.nilebeat.app.NileBeatApplication;
import com.neusoft.rsapm.nilebeat.guice.AkkaModule;

public class StartSystem {
	public static void main(String[] args) throws Exception {
		Guice
			.createInjector(new AkkaModule())
			.getInstance(NileBeatApplication.class)
			.run();
    }
}
