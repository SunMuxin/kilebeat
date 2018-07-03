/**  
    * @Title: FileWatcherServiceTest.java  
    * @Package com.neusoft.aclome.kilebeat.service  
    * @Description: TODO(用一句话描述该文件做什么)  
    * @author Muxin Sun  
    * @date 2018年5月24日  
    * @version V1.0  
*/  
package com.neusoft.rsapm.nilebeat.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.neusoft.rsapm.nilebeat.util.file.FileMonitorService;

/**  
* @ClassName: FileWatcherServiceTest  
* @Description: TODO(这里用一句话描述这个类的作用)  
* @author Muxin Sun
* @date 2018年5月24日  
*    
*/
public class FileWatcherServiceTest {

	@Test
	public void test() {
		Path root = Paths.get(System.getProperty("user.dir"));
		System.getProperties().setProperty("config.file", 
				Paths.get(root.toString(), "confs").toString() + "/kilebeat.conf");
		
		FileMonitorService service = new FileMonitorService(new File(System.getProperty("config.file")));
		service.resolveActualFiles().forEach(f -> {
			System.out.println(f.getName());
		});
		service.close();
	}

}
