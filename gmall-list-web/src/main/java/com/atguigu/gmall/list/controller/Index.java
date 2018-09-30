package com.atguigu.gmall.list.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author shkstart
 * @create 2018-09-12 16:58
 */
@Controller
public class Index {
    @RequestMapping("/index")
    public String index(){
    
        return "index";
    }
}
