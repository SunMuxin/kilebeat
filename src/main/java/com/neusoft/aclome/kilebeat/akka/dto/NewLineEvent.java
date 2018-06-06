package com.neusoft.aclome.kilebeat.akka.dto;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import com.neusoft.aclome.kilebeat.util.LoggerUtil;
import com.neusoft.aclome.kilebeat.util.TimeUtil;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NewLineEvent implements Serializable {
	/**  
     * @Fields {todo}(用一句话描述这个变量表示什么)  
	 * @author Muxin Sun
	 * @date 2018年6月6日
	*/  
	private static final long serialVersionUID = 4328234078076514941L;

	private final String result_s = "logger";

	private String ip_s;
	private String host_s;
	private String content_s;
	private String level_s;
	private String rs_timestamp;
	private String path_s;
	
	public NewLineEvent(String line, Path path) {
		
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr!=null 
							&& InetAddressUtils.isIPv4Address(addr.getHostAddress())
							&& !addr.getHostAddress().equals("127.0.0.1")) {
						this.ip_s = addr.getHostAddress();
					}
				}
			}
			this.host_s = InetAddress.getLocalHost().getHostName();

		} catch (SocketException | UnknownHostException e) {
			this.ip_s = null;
			this.host_s = null;
		}

		this.content_s = LoggerUtil.parseContent(line).map(c -> c).orElse(null);
		this.level_s = LoggerUtil.parseLevel(line).map(c -> c).orElse(null);
		this.path_s = path.toString().replace('\\', '/');
		this.rs_timestamp = TimeUtil.formatUnixtime2(System.currentTimeMillis());
	}
}
