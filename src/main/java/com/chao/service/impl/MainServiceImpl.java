package com.chao.service.impl;

import com.chao.annoation.ChaoService;
import com.chao.service.MainService;

/**
 * Created by LuZichao on  2018/12/17 11:40
 */
@ChaoService("mainService")
public class MainServiceImpl implements MainService {
    public String query(String name, String age) {
        return "name===>"+name+";,age===>"+age;
    }
}
