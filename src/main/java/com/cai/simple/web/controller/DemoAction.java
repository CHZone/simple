package com.cai.simple.web.controller;

import com.cai.simple.di.annotataion.Autowired;
import com.cai.simple.ioc.annotaion.Controller;
import com.cai.simple.mvc.annotation.RequestMapping;
import com.cai.simple.mvc.annotation.RequestParam;
import com.cai.simple.web.service.DemoService;

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
