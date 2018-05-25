package com.neusoft.aclome.kilebeat.retry;

public class MaxRetryException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public MaxRetryException(String msg) {
		super(msg);
	}
}