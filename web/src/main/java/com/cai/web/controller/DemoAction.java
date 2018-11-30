package com.cai.web.controller;


import com.cai.di.annotation.Autowired;
import com.cai.ioc.annotation.Controller;
import com.cai.mvc.annotation.RequestMapping;
import com.cai.mvc.annotation.RequestParam;
import com.cai.web.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequestMapping("/demo")
public class DemoAction {
    @Autowired
    DemoService demoService;

    @RequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @RequestParam("name") String name){
        String result = "hello "+name+"!";
        try {
            resp.getWriter().write(result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequestMapping("/add.json")
    public void add(HttpServletRequest req, HttpServletResponse resp, @RequestParam("a") Integer a, @RequestParam("b") Integer b){
        try {
            resp.getWriter().write(a+"+"+b+"="+(a+b));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
