package com.atguigu.gmall.manage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author shkstart
 * @create 2018-09-04 10:05
 */
@Controller
public class IndexController {

    @RequestMapping("/spuListPage")
    public String spuListPage(){
    
        return "spuListPage";
    }
    /**
     * 平台属性界面
     * @return
     */
   @RequestMapping("/attrListPage")
   public String getattrList(){
   
       return "attrListPage";
   }
    @RequestMapping("/index")
    public String index(){
        return "index";
    }
}
