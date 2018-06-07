# nilebeat
[filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-overview.html) in java using [AKKA](http://akka.io)

For the release with support three connector 
- generic http POST
- kafka 
- solr

We support text regex match. but, you need to add config in pom.xml(contact me):

```
<dependency>
	<artifactId>timeseries-for-apm</artifactId>
	<groupId>com.neusoft.aclome</groupId>
	<version>${neusoft.aclome.version}</version>
</dependency>
```
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
	"ip_s":"192.168.1.9",
	"host_s":"BC-VM-1418df5b51e34dfabcc357c96cce26b5",
	"content_s":"Metrics - group=thruput, name=thruput, instantaneous_kbps=1.2333010129753927",
	"rs_timestamp":"2018-06-06T03:23:13Z",
	"path_s":"/var/log/splunk",
	"template_s":"Metrics - group=${1}, name=${2}, instantaneous_kbps=${3}",
	"${1}":"thruput",
	"${2}":"thruput",
	"${3}":"1.2333010129753927"
}
```
