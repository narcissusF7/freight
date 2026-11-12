package com.lukachangtong.freight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 唯一启动类。其余模块都是库模块，禁止出现 main 方法或第二个 SpringBootApplication。
 * Mapper 仅注册标注 @Mapper 的接口，避免把 BankClient/CA 等非 Mapper 接口误注册。
 *
 * @EnableScheduling 只为轨迹分区运维任务开启；具体任务类由
 * `freight.track.job.enabled` 开关控制，默认关闭，避免测试环境改表结构。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.lukachangtong.freight", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class FreightApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreightApplication.class, args);
    }
}
