package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;
import com.wubai.summer.test.pojo.User;

/**
 * @Author：fs
 * @Date:2026/3/515:12
 */

@Component
public class UserService {
    @Autowired
    private User user;

    public void setUser(User user) { this.user = user; }
    public User getUser() { return user; }
    public void sayHello() { System.out.println("UserService: Hello " + user); }
}
