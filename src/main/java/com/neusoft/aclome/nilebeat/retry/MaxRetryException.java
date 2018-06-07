package com.neusoft.aclome.nilebeat.retry;

public class MaxRetryException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public MaxRetryException(String msg) {
		super(msg);
	}
}