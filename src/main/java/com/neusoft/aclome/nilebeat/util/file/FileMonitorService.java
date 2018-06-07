/**  
    * @Title: ConfigFileListener.java  
    * @Package com.neusoft.aclome.kilebeat.application  
    * @Description: TODO(用一句话描述该文件做什么)  
    * @author Muxin Sun  
    * @date 2018年5月23日  
    * @version V1.0  
*/  
package com.neusoft.aclome.nilebeat.util.file;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**  
* @ClassName: ConfigFileListener  
* @Description: 文件变化监听器 在Apache的Commons-IO中有关于文件的监控功能的代码  
* @author Muxin Sun
* @date 2018年5月23日  
*    
*/

@Slf4j
public class FileMonitorService implements AutoCloseable {	
	
	//XXX do the same work with one structure
	private WatchService watcher;
	private WatchKey key;
	private File file;

	@Inject
	public FileMonitorService(File file) {
		
		this.file = file;
		
		final Path path = Paths.get(file.getParent());
		
		try {
			this.watcher = path.getFileSystem().newWatchService();
			this.key = path.register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}.", e.getMessage());
		}		
	}

	@Override
	public void close() {
		try {
			watcher.close();
		} catch (IOException e) {
			LOGGER.error("", e);				
		}	
	}

	public List<File> resolveEvents() {
		final List<File> result = new ArrayList<>();
		
		//XXX every WatchKey should poll the same file ... but i can process it only one times 
		final Set<Path> consumedResource = new HashSet<>();
		
		final WatchKey wKey = key;
		
		for (WatchEvent<?> we : wKey.pollEvents()) {
			final Optional<File> isRelated = related(file, (Path)we.context());
			final Path path = (Path)we.context();
			
			if (isRelated.isPresent() && !consumedResource.contains(path)) {
				LOGGER.info("on path {} ... activated event", isRelated.get().getPath());
				consumedResource.add(path);							
				result.add(isRelated.get());
			} else {
				final String parentPath = file.getParent();
				LOGGER.info("on path {} ... is dir", parentPath + "/" + path);
			}
		};				
						
		//XXX remove elements from keys Map when file without regExp is viewed
		
		return result;
	}
	
	public List<File> resolveActualFiles() {		
		try {
			final Path path = Paths.get(file.getParent());	
			
			return 
			Files
				.list(path)
				.map(p -> related(file, p))
				.filter(o -> o.isPresent())
				.map(o -> o.get())
				.collect(Collectors.toList());
			
		} catch (IOException e) {
			LOGGER.error("", e);
			
			return Lists.newArrayList();
		}		
	}	

	private Optional<File> related(File file, final Path path) {
		final File initialResource = file;				
		final String currentName = path.toFile().getName();
		
		final File newSc;
		if (match(initialResource.getName(), currentName)) {
			newSc = new File(initialResource.getParent() + "/" + currentName);
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


