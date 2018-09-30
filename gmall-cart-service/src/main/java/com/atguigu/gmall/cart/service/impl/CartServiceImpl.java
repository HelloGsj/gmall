package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author shkstart
 * @create 2018-09-15 11:43
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    /**
     * 通过skuId跟userId判断是否存在该数据
     * @param cartInfo
     * @return
     */
    @Override
    public CartInfo ifCartExits(CartInfo cartInfo) {
        String skuId = cartInfo.getSkuId();
        String userId = cartInfo.getUserId();
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        CartInfo cartInfoOne = cartInfoMapper.selectOneByExample(example);


        return cartInfoOne;
    }


    @Override
    public void insertCart(CartInfo cartInfo) {
        cartInfoMapper.insertSelective(cartInfo);
        //同步缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    /**
     * 更新及添加的数据缓冲到redis中，便于查询时，可访问到redis的库中
     * @param userId
     */
    @Override
    public void flushCartCacheByUserId(String userId) {


        //通过用户id查找该用户的购物车信息
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        //会从购物车查找到该用户信息，在保存到redis中
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);
        //将购物车集合转为map
        //如果购物车集合不为空
        if(null != cartInfos&&cartInfos.size() >0){
            Map<String,String> map = new HashMap<>();
            for (CartInfo info : cartInfos) {
                //将每一个购物车信息保存到map中，key为每一个购物车的id
                map.put(info.getId(), JSON.toJSONString(info));
            }
            Jedis jedis = redisUtil.getJedis();
            jedis.del("cart:" + userId + ":list");
            jedis.hmset("cart:" + userId + ":list",map);
            jedis.close();
        }else {
            Jedis jedis = redisUtil.getJedis();
            jedis.del("cart:" + userId + ":list");
            jedis.close();
        }


    }

    /**
     * 通过主键更新
     * @param cartInfDb
     */
    @Override
    public void updateCart(CartInfo cartInfDb) {
        cartInfoMapper.updateByPrimaryKeySelective(cartInfDb);
        //同步缓存
        flushCartCacheByUserId(cartInfDb.getUserId());
    }

    /**
     * 从redis中获取的购物车对象
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartInfoFromCacheByUserId(String userId) {
        //声明一个购物车
        List<CartInfo> cartInfoList = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        List<String> hvals = jedis.hvals("cart:" + userId + ":list");
        if(hvals.size() > 0&&hvals != null){
            for (String hval : hvals) {
                //需要从redis中获取的值（json字符串）转为对象
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     * 通过用户id跟skuid修改
     * @param cartInfo
     */
    @Override
    public void updateCartByUserId(CartInfo cartInfo) {

        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());
        cartInfoMapper.updateByExampleSelective(cartInfo,example);
        //同步缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    /**
     * 将cookie的购物车中相同的sku商品信息保存到数据库中
     * @param userId
     * @param listCartCookie
     */
    @Override
    public void combin(String userId, List<CartInfo> listCartCookie) {

        //通过userId查询用户的购物车
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfoListDb = cartInfoMapper.select(cartInfo);

        //如果cookie的购物车不为空
        if(listCartCookie != null&&listCartCookie.size()>0){
            for (CartInfo cartCookie : listCartCookie) {
                String skuIdCookie = cartCookie.getSkuId();
                boolean b = true;
                //如果数据库查出数据
                if(cartInfoListDb != null&&cartInfoListDb.size()>0){
                    //遍历数据库的购物车，如果当前cookie的购物车有相关的sku商品，则需要更新
                    //如果存在，则返回false
                    b = ifNewCart(cartInfoListDb,cartCookie);
                }
                //不等于false，则证明有相同的购物车信息，则需要更新
                if(!b){
                    CartInfo cartInfoDb = new CartInfo();
                    //遍历数据库的购物车，进行更新
                    for (CartInfo info : cartInfoListDb) {
                        //创建一个购物车

                        //如果数据库的购物车的skuId与cookied的skuId相同
                        if(info.getSkuId().equals(cartCookie.getSkuId())){
                            //将匹配的购物车赋值给创建的购物车
                            cartInfoDb = info;
                            //将与cookie匹配的购物车赋值对应cartInfoDb的各个字段
                            cartInfoDb.setSkuNum(cartCookie.getSkuNum());
                            cartInfoDb.setIsChecked(cartCookie.getIsChecked());
                            cartInfoDb.setCartPrice(cartInfoDb.getSkuPrice().multiply(new BigDecimal(cartInfoDb.getSkuNum())));
                        }

                    }
                }else{
                    //如果数据库没有匹配的购物车，则需要将cookie的购物车添加到数据库
                    cartCookie.setUserId(userId);
                    cartInfoMapper.insertSelective(cartCookie);

                }
            }
        }

        //同步刷新缓存
        flushCartCacheByUserId(userId);

    }
    /**
     *   通过购物车id删除购物车信息
     */
    @Override
    public void deleteCart(List<String> join,String userId) {
        Example example = new Example(CartInfo.class);
        example.createCriteria().andIn("id",join);
        cartInfoMapper.deleteByExample(example);
        //同步缓存
        flushCartCacheByUserId(userId);
       // cartInfoMapper.deleteCarts(join);
    }

    /***
     * 判断购物车数据更新还是新增
     * @param listCartDb
     * @param cartInfo
     * @return
     */
    private boolean ifNewCart(List<CartInfo> listCartDb, CartInfo cartInfo) {

        boolean b = true;

        for (CartInfo info : listCartDb) {
            if (info.getSkuId().equals(cartInfo.getSkuId())) {
                b = false;
            }
        }

        return b;
    }
}
