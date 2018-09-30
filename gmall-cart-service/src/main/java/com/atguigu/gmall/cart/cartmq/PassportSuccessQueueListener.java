package com.atguigu.gmall.cart.cartmq;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-27 16:04
 */
@Component
public class PassportSuccessQueueListener {

    @Autowired
    CartService cartService;

    /**
     * 通过监听到有登陆信息，就将cookie的购物车信息保存到数据库
     *
     * @param message
     * @throws JMSException
     */
    @JmsListener(destination = "PASSPORT_SUCCESS", containerFactory = "jmsQueueListener")
    public void counsumercombinCart(MapMessage message) throws JMSException {
        String userId = message.getString("userId");
        String cartInfosCookie = message.getString("cartInfosCookie");
        List<CartInfo> cartInfoList = JSON.parseArray(cartInfosCookie, CartInfo.class);

        cartService.combin(userId, cartInfoList);

    }


}
