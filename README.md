# nilebeat
利用 [AKKA](http://akka.io) 实现[filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-overview.html) 的日志数据采集和推送功能，并增加了自动化生成日志正则表达式功能。

目前支持的推送有： 
- generic http POST
- kafka 
- solr

日志采集端的性能：1000条/秒的采集速度下，4核CPU的消耗小于5%。
如果需要自动生成：日志的正则表达式需要在pom.xml中加入如下配置（这个配置在我本地maven库中，联系我）:

```
<dependency>
	<artifactId>timeseries-for-apm</artifactId>
	<groupId>com.XXXX.rsapm</groupId>
	<version>${XXXX.XXX.version}</version>
</dependency>
```
在日志采集的过程中，网络中断不会影响程序程序运行，但是会导致断网过程中可能会导致日志丢失。

配置的例子如下：

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

同样对于推送包含以下个性化的配置，如采集规则等：

```
bulk {
	size = X (number of in memory lines) (mandatory)
	timeout = Y (number of in seconds before is forced to send messages to connectors) (optional)
}

send-if-match = "^\\d.*" (it's clear)

send-if-not-match = ".*[1-9].*"	(it's clear)

```
solr中获取的日志信息及日志模板信息的例子如下：
```
{
	"result_s":"logger",
	"ip_s":"192.168.1.9",
	"host_s":"BC-VM-1418df5b51e34dfabcc357c96cce26b5",
	"level_s":"INFO",
	"content_s":"Metrics - group=thruput, name=thruput, instantaneous_kbps=1.2333010129753927",
	"rs_timestamp":"2018-06-06T03:23:13Z",
	"path_s":"/var/log/splunk",
	"template_s":"Metrics - group=${1}, name=${2}, instantaneous_kbps=${3}",
	"param_1":"thruput",
	"param_2":"thruput",
	"param_3":"1.2333010129753927"
}
```
