package com.easy.hbase.groovy.cfg

import org.apache.hadoop.hbase.HBaseConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.hadoop.hbase.HbaseTemplate

@Configuration
class HbaseGroovyCfg {
    @Bean
    HbaseTemplate hbaseTemplate(@Value('${hbase.zookeeper.quorum}') String quorum,
                                @Value('${hbase.zookeeper.property.clientPort}') String clientPort){
        def hbaseTemplate = new HbaseTemplate()
        def conf = HBaseConfiguration.create()
        conf.set("hbase.zookeeper.quorum", quorum)
        conf.set("hbase.zookeeper.property.clientPort", clientPort)
        conf.setInt("hbase.rpc.timeout",20000)
        conf.setInt("hbase.client.operation.timeout",30000)
        conf.setInt("hbase.client.scanner.timeout.period",200000)
        System.err.println "hbase配置quorum：" + quorum
        System.err.println "hbase配置clientPort：" + clientPort
        hbaseTemplate.setConfiguration(conf)
        hbaseTemplate.setAutoFlush(true)
        return hbaseTemplate
    }

}
