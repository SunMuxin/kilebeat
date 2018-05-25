package com.neusoft.aclome.kilebeat.retry;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class RetryCommand {
	
	private final int maxRetries;	
	private final String path;
	private int retryCounter;

	public RetryCommand(int maxRetries, String path) {
		this.maxRetries = maxRetries;
		this.path = path;
	}

    public void run(Callable<Void> function) {
        try {
        	function.call();
        } catch (RuntimeException re) {
            if(isRetriable(re.getCause())) {
                retry(function);
             } else {
             	throw notRetriableException(re);
             }        	
        } catch (Exception re) {        
        }
    }

    @SuppressWarnings("unchecked")
	public <T> T run(Supplier<?> function) {
        try {
        	return (T) function.get();
        } catch (RuntimeException re) {
            if(isRetriable(re.getCause())) {
                return retry(function);
             } else {
             	throw notRetriableException(re);
             }        	        
        }
    }

    private void retry(Callable<Void> function) {
        retryCounter = 0;
        
        while (retryCounter < maxRetries) {
            try {
                function.call();
            } catch (Exception ex) {
                if(isRetriable(ex.getCause())) {
                    retryCounter++;

                    try {
                        Thread.sleep(1500); //TODO usa il backoff esponenziale invece della sleep
                    } catch (InterruptedException e) {}

                    if (retryCounter >= maxRetries) {
                        break;
                    }
                } else {
                    throw notRetriableException(ex);
                }
            }
        }
        
        throw maxRetriableException();
    }
    
    @SuppressWarnings("unchecked")
	private <T> T retry(Supplier<?> function) {
        retryCounter = 0;
        
        while (retryCounter < maxRetries) {
            try {
                return (T) function.get();
            } catch (Exception ex) {
                if(isRetriable(ex.getCause())) {
                    retryCounter++;

                    try {
                        Thread.sleep(1500); //TODO usa il backoff esponenziale invece della sleep
                    } catch (InterruptedException e) {}

                    if (retryCounter >= maxRetries) {
                        break;
                    }
                } else {
                    throw notRetriableException(ex);
                }
            }
        }
        
        throw maxRetriableException();
    }
    
	private boolean isRetriable(Throwable cause) {
		return true;
	}

	private NotRetriableException notRetriableException(Exception re) {
		return new NotRetriableException(re.getMessage());	
	}

	private MaxRetryException maxRetriableException() {
		return new MaxRetryException("Retry failed on " + path);
	}

}
