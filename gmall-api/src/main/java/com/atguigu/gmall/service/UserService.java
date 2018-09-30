package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-01 16:04
 */

public interface UserService {
    List<UserInfo> getUsers();

    List<UserAddress> getUserAllAddress();

    void deleteUser(List<Integer> userId);

    UserInfo getUser(String userId);

    void updateUser(UserInfo userInfo);
    //用户登录
    UserInfo login(UserInfo userInfo);
    //通过用户id获取用户地址
    List<UserAddress> getUserAddressByUserId(String userId);
    //通过地址id，获取用户的地址信息
    UserAddress getAddressById(String addressId);
}
