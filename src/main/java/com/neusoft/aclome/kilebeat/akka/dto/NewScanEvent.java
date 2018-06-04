package com.neusoft.aclome.kilebeat.akka.dto;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import com.neusoft.aclome.kilebeat.util.TimeUtil;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NewScanEvent {
	private final String result_s = "scanner";
	
	private String ip_s;
	private String host_s;
	private String rs_timestamp;
	private List<String> path_ss;
	private String scan_path_s;

	public NewScanEvent(String scan_path_s, List<String> path_ss) {

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
		
		this.scan_path_s = scan_path_s;
		this.path_ss = path_ss;
		this.rs_timestamp = TimeUtil.formatUnixtime2(System.currentTimeMillis());
	}
	
	public void addPath(String path) {
		path_ss.add(path);
	}
}
