package com.wubai.summer.test.config;

/**
 * @Author：fs
 * @Date:2026/3/515:15
 */
// 模拟第三方组件：数据源
public class DataSource  {
    private String url = "jdbc:mysql://localhost:3306/system";
    private String username = "root";
    @Override
    public String toString() { return "DataSource{url='" + url + "', username='" + username + "'}"; }


}