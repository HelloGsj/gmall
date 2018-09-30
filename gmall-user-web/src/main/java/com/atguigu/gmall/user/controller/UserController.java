package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;


/**
 * @author shkstart
 * @create 2018-09-01 16:02
 */
@RequestMapping("/user")
@Controller
public class UserController {

    @Reference
    UserService userservice;

    @RequestMapping("/userList")
    @ResponseBody
    public List<UserInfo> getAllUsers(){
        List<UserInfo> userLists = userservice.getUsers();
        return userLists;
    }

    @RequestMapping("/deleteUser")
    @ResponseBody
    public String deleteUser(@RequestParam("userIds")String userIds){
        List<Integer> userIdList = new ArrayList<>();
        String[] split = userIds.split(",");
        for (String s : split) {

            try {
                int i = Integer.parseInt(s);
                userIdList.add(i);
            } catch (NumberFormatException e) {
            }
        }
        userservice.deleteUser(userIdList);
        return "delete";
    }
    @RequestMapping("/getUser")
    @ResponseBody
    public UserInfo getUser(@RequestParam("userId")String userId){
        UserInfo user = userservice.getUser(userId);
        return user;
    }
    @RequestMapping("/updateUser")
    @ResponseBody
    public String update(UserInfo userInfo){
        userservice.updateUser(userInfo);
        return "update";
    }
    @RequestMapping("/userAddress")
    @ResponseBody
    public List<UserAddress> getUserAddress(){
        List<UserAddress> userAllAddress = userservice.getUserAllAddress();
        return userAllAddress;
    }
}
