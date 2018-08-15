/**  
 * @Title: ManagerActorTest.java  
 * @Package com.neusoft.rsapm.nilebeat.akka  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author Muxin Sun  
 * @date 2018年8月14日  
 * @version V1.0  
*/  
package com.neusoft.rsapm.nilebeat.akka;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.neusoft.rsapm.nilebeat.util.Util;
import com.neusoft.rsapm.tsp.api.OnlineLoggerProcessAPI;
import com.neusoft.rsapm.tsp.lib.text.SyslogReader;
import com.neusoft.rsapm.tsp.lib.util.LoggerUtil;
import com.sun.tools.javac.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**  
 * @ClassName: ManagerActorTest  
 * @Description: 测试ManagerActor的
 * @author Muxin Sun  
 * @date 2018年8月14日  
 *    
*/

@Slf4j
public class ManagerActorTest {
	
	private Path process_path = 
			Paths.get(System.getProperty("user.dir"), "data", "nilebeat.rsapm");

	@Before
	public void save() throws IOException {
		String path = Paths.get(System.getProperty("user.dir"), "logs", "metrics.log").toString();
		
		OnlineLoggerProcessAPI process = new OnlineLoggerProcessAPI();
		
		SyslogReader train = new SyslogReader(path);
		while(train.hasRecord()) {
			process.process(LoggerUtil.parseContent(train.nextRecord()).get());
		}
		train.close();
		Util.save(process_path, process);
	}
	
	@Test
	public void testOpen() throws IOException, ClassNotFoundException {
		OnlineLoggerProcessAPI process = Util.open(process_path);
		
		String path = Paths.get(System.getProperty("user.dir"), "logs", "metrics.log").toString();
		SyslogReader train = new SyslogReader(path);
		while(train.hasRecord()) {
			int match = process.process(LoggerUtil.parseContent(train.nextRecord()).get()).get(0).get("match").getAsInt();
			Assert.check(match > 1);
		}
		train.close();
		LOGGER.info(process.toString());
	}

}
