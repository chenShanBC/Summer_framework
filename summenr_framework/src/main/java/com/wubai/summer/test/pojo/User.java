package com.wubai.summer.test.pojo;

import com.wubai.summer.annotation.Component;

/**
 * @Author：fs
 * @Date:2026/3/515:12
 */

@Component
public class User {
    private String name = "testUser";
    private int age = 20;
    @Override
    public String toString() { return "User{name='" + name + "', age=" + age + "}"; }
}

