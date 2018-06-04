package com.neusoft.aclome.kilebeat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import com.google.inject.Guice;
import com.neusoft.aclome.kilebeat.guice.KileModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartSystem {

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new BasicParser( );
		Options options = new Options( );  
		options.addOption("h", "help", false, "Print this usage information");  
		options.addOption("c", "config.file", true, "Config File dir" ); 

		CommandLine commandLine = parser.parse( options, args ); 
		
		if( commandLine.hasOption('h') ) {
			System.out.println( "-h\t Print this usage information");
			System.out.println( "-c\t Config File Dir");

			System.exit(0); 
		}
		
		if( commandLine.hasOption('c') ) {
			System.getProperties().setProperty("config.file", commandLine.getOptionValue('c'));
		} else {
			Path root = Paths.get(System.getProperty("user.dir"));
			System.getProperties().setProperty("config.file", 
					Paths.get(root.toString(), "confs", "kilebeat.conf").toString());
		}
		
		LOGGER.info("config.file = " + System.getProperty("config.file"));
		
		Guice
			.createInjector(new KileModule(System.getProperty("config.file")))
			.getInstance(KileManager.class)
			.run();
    }
}
