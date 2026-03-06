package com.wubai.summer.test.config;

import com.wubai.summer.annotation.Bean;
import com.wubai.summer.annotation.ComponentScan;
import com.wubai.summer.annotation.Configuration;

/**
 * @Author：fs
 * @Date:2026/3/515:15
 */
// 配置类：包扫描 + 第三方Bean注册
@Configuration
@ComponentScan({"com.wubai.summer.test","com.wubai.summer.core"}) // 扫描包（com.wubai.summer）
public class AppConfig {
    // 注册第三方Bean：DataSource
    @Bean
    public DataSource dataSource() {
        return new DataSource();
    }
}