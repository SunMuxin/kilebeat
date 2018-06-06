/**  
 * @Title: JobFailed.java  
 * @Package com.neusoft.aclome.kilebeat.akka.dto  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author Muxin Sun  
 * @date 2018年6月5日  
 * @version V1.0  
*/
package com.neusoft.aclome.kilebeat.akka.dto;

import java.io.Serializable;

/**
 * @ClassName: JobFailed
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author 萌
 * @date 2018年6月5日
 * 
*/
public class JobFailed implements Serializable {

	private static final long serialVersionUID = -7778761112766553505L;
	private final String reason;
	private final NewLineEvent job;

	public JobFailed(String reason, NewLineEvent job) {
		this.reason = reason;
		this.job = job;
	}

	public String getReason() {
		return reason;
	}

	public NewLineEvent getJob() {
		return job;
	}

	@Override
	public String toString() {
		return "JobFailed(" + reason + ", " + job + ")";
	}
}
