package com.neusoft.aclome.kilebeat.akka.dto;

import java.nio.file.Path;

import com.neusoft.aclome.kilebeat.util.TimeUtil;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NewLineEvent {
	private final String content;
	private final String rs_timestamp;
	private final String path;

	public NewLineEvent(String content, Path path) {
		this.content = content;
		this.path = path.toString();
		this.rs_timestamp = TimeUtil.formatUnixtime2(System.currentTimeMillis());
	}
}
