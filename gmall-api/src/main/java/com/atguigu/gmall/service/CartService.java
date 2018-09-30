package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-15 11:37
 */
public interface CartService {

    //是否存在该购物车
    CartInfo ifCartExits(CartInfo cartInfo);
    //插入到购物车
    void insertCart(CartInfo cartInfo);
    //同步缓存
    void flushCartCacheByUserId(String userId);
    //更新
    void updateCart(CartInfo cartInfDb);
    //从redis中获取cartInfo信息
    List<CartInfo> getCartInfoFromCacheByUserId(String userId);
    //通过用户id跟skuId修改购物车
    void updateCartByUserId(CartInfo cartInfo);
    //将cookie的购物车保存到数据库
    void combin(String id, List<CartInfo> cartInfos);
    //删除购物车
    //void deleteCart(String join);
    void deleteCart(List<String> list,String userId);
}
