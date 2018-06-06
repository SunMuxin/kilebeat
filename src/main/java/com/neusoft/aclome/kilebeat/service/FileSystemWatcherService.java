package com.neusoft.aclome.kilebeat.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.configuration.ExportsConfigurationValidator.SingleConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileSystemWatcherService implements AutoCloseable {	
	
	//XXX do the same work with one structure
	private final Map<SingleConfiguration, WatchService> watchers;
	private final Map<SingleConfiguration, WatchKey> keys;

	@Inject
	public FileSystemWatcherService() {
		this.watchers = new HashMap<>();
		this.keys = new HashMap<>();
	}

	@Override
	public void close() {
		watchers.values().forEach(ws -> {
			try {
				ws.close();
			} catch (IOException e) {
				LOGGER.error("", e);				
			}
		});		
	}

	public List<SingleConfiguration> resolveEvents() {
		final List<SingleConfiguration> result = new ArrayList<>();
		
		//XXX every WatchKey should poll the same file ... but i can process it only one times 
		final Set<Path> consumedResource = new HashSet<>();
		
		keys.entrySet().forEach(e -> {					
			final WatchKey wKey = e.getValue();
			
			for (WatchEvent<?> we : wKey.pollEvents()) {
				final Optional<SingleConfiguration> isRelated = related(e.getKey(), (Path)we.context());
				final Path path = (Path)we.context();
				
				if (isRelated.isPresent() && !consumedResource.contains(path)) {
					LOGGER.info("on path {} ... run tail", isRelated.get().getPath());
					consumedResource.add(path);							
					result.add(isRelated.get());
				} else {
					final String parentPath = e.getKey().getPath().getParent();
					LOGGER.info("on path {} ... can't run tail", parentPath + "/" + path);
				}
			}
		});				
						
		//XXX remove elements from keys Map when file without regExp is viewed
		
		return result;
	}
	
	public List<SingleConfiguration> resolveActualFiles(SingleConfiguration conf) {		
		try {
			final Path path = conf.getPath().getParentFile().toPath();
			final WatchService wService = path.getFileSystem().newWatchService();
			
			keys.put(conf, path.register(wService, ENTRY_CREATE));
			watchers.put(conf, wService);
			
			return Files
				.list(path)
				.map(p -> related(conf, p))
				.filter(o -> o.isPresent())
				.map(o -> o.get())
				.collect(Collectors.toList());
			
		} catch (IOException e) {
			LOGGER.error("", e);
			
			return Lists.newArrayList();
		}
	}

	private Optional<SingleConfiguration> related(SingleConfiguration conf, final Path path) {
		final File initialResource = conf.getPath();				
		final String currentName = path.toFile().getName();
		
		final SingleConfiguration newSc;		
		if (match(initialResource.getName(), currentName)) {
			newSc = conf.makeCopy(initialResource.getParent() + "/" + currentName);
		} else {
			newSc = null;
		}
		
		return Optional.ofNullable(newSc);
	}

	//XXX until new idea, we only support '?' and '*' placeholders
	//see https://stackoverflow.com/questions/34514650/wildcard-search-using-replace-function
	private boolean match(String configName, String fileName) {	
		final String regex = configName.replace("?", ".?").replace("*", ".*?");		
		final Pattern pattern = Pattern.compile(regex);
		
		return pattern.matcher(fileName).matches();
	}
}
