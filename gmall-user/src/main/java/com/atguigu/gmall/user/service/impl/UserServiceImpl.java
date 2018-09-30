package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;


/**
 * @author shkstart
 * @create 2018-09-01 16:07
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserAddressMapper userAddressMapper;
    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    RedisUtil redisUtil;

    public List<UserInfo> getUsers() {
        List<UserInfo> userLists = userInfoMapper.selectAll();
        return userLists;
    }

    public List<UserAddress> getUserAllAddress() {
        List<UserAddress> userAddresses = userAddressMapper.selectAll();
        return userAddresses;
    }

    @Override
    public void deleteUser(List<Integer> userIds) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andIn("id",userIds);
        userInfoMapper.deleteByExample(example);
    }

    @Override
    public UserInfo getUser(String userId) {
        UserInfo userInfo = userInfoMapper.selectByPrimaryKey(userId);
        return userInfo;
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKeySelective(userInfo);
    }

    /**
     * 登录功能
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        UserInfo userInfoOne = userInfoMapper.selectOne(userInfo);
        //如果用户不为空，则将用户信息保存到redis中，并且设置超时时间为24小时
        if(userInfoOne != null){
            Jedis jedis = redisUtil.getJedis();
            jedis.setex("user:" + userInfoOne.getId() + ":info",1000*60*60*24, JSON.toJSONString(userInfoOne));
            jedis.close();

        }
        return userInfoOne;
    }

    /**
     * 通过userID返回一个Address的集合
     * @param userId
     * @return
     */
    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddresses = userAddressMapper.select(userAddress);
        return userAddresses;
    }

    /**
     * 通过地址编号，获取用户收货的地址信息
     * @param addressId
     * @return
     */
    @Override
    public UserAddress getAddressById(String addressId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(addressId);
        UserAddress address = userAddressMapper.selectOne(userAddress);
        return address;
    }


}
