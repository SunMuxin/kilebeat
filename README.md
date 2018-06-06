# nilebeat
[filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-overview.html) in java using [AKKA](http://akka.io)

For the first release with support only three connector 
- generic http POST
- kafka 
- solr

We also support stop and resume of endpoint connector (losing all messages in the period when server connector's was down).
Before considering a failed connection, up to 3 tests are performed (it will become a configuration). 

Example configuration and usage:
```
exports = [
    {
        path = "/Users/power/Tmp/a" 		
        
    }
    {
        path = "/Users/power/Tmp/*.log"
    }
    {
        path = "/Users/power/Tmp/q"        
    }
]

http {
    url = "http://localhost:55555/testA"
}
kafka {
    host = "localhost:44444"
    queue = "testQ"
}
solr {
    url = "http://localhost:8080/solr"
    core = "log"
}    
akka {  
  actor {  
    provider = "akka.cluster.ClusterActorRefProvider"  
  }  
  remote {  
    log-remote-lifecycle-events = off  
    netty.tcp {  
      hostname = "127.0.0.1"  
      port = 2551  
    }  
  }  
  
  cluster {  
    seed-nodes = [  
      "akka.tcp://NilebeatSystem@127.0.0.1:2551",  
      "akka.tcp://NilebeatSystem@127.0.0.1:2551"]  
  
    #//#snippet  
    # excluded from snippet  
    # auto-down-unreachable-after = 10s  
    #//#snippet  
    # auto downing is NOT safe for production deployments.  
    # you may want to use it during development, read more about it in the docs.  
    #  
    # auto-down-unreachable-after = 10s  
    roles = [wisp, ancient]
    # Disable legacy metrics in akka-cluster.  
    metrics.enabled=off  
  }  
}  

```

Any export Object should contain some behaviour config

```
bulk {
	size = X (number of in memory lines) (mandatory)
	timeout = Y (number of in seconds before is forced to send messages to connectors) (optional)
}

send-if-match = "^\\d.*" (it's clear)

send-if-not-match = ".*[1-9].*"	(it's clear)

```

An example of json sent to the connector is
```
{
	"result_s":"logger",
	"ip_s":"localhost",
	"host_s":"BC-VM-1418df5b51e34dfabcc357c96cce26b5",
	"content_s":"Jun  6 11:23:13 localhost sshd[32738]: Connection closed by 127.0.0.1",
	"rs_timestamp":"2018-06-06T03:23:13Z",
	"path_s":"/var/log/secure"
}
```
