package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.Component;

/**
 * @Author：fs
 * @Date:2026/3/614:29
 */
@Component
public class PeopleImpl implements People {
    @Override
    public String addPeople(String name) {
        return name + " has be added !" ;
    }

    @Override
    public String deletePeople(Integer id) {
        return "the people of  = " + id  + "  = has be deleted !" ;
    }
}
