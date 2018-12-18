package com.chao.controller;

import com.chao.annoation.ChaoAutowired;
import com.chao.annoation.ChaoController;
import com.chao.annoation.ChaoRequestMapping;
import com.chao.annoation.ChaoRequestParam;
import com.chao.service.MainService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by LuZichao on  2018/12/17 11:39
 */
@ChaoController("mainController")
@ChaoRequestMapping("/main")
public class MainController {

    @ChaoAutowired("mainService")
    private MainService mainService;

    @ChaoRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @ChaoRequestParam("name") String name, @ChaoRequestParam("age") String age) {
        try {
            PrintWriter printWriter = response.getWriter();
            String result = mainService.query(name, age);
            printWriter.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
