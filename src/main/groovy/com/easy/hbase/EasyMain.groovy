package com.easy.hbase

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@EnableAutoConfiguration
class EasyMain {
    static main(args){
        SpringApplication.run(EasyMain.class, args)
    }
}
