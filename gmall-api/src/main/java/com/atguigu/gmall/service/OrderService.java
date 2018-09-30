package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.OrderInfo;

/**
 * @author shkstart
 * @create 2018-09-20 11:30
 */
public interface OrderService {
    /**
     * 保存订单信息
     * @param orderInfo
     */
    void saveOrder(OrderInfo orderInfo);

    /**
     * 生成交易码
     * @param userId
     * @return
     */
    String genTradeCode(String userId);

    /**
     * 检查是否有交易码
     * @param userId
     * @param trackCode
     * @return
     */
    boolean checkTradeCode(String userId, String trackCode);

    OrderInfo getOrderInfoBYOutTradeNo(String outTradeNo);

    /**
     * 将订单信息保存到消息队列
     * @param orderInfo
     */
    void sendOrderResultQueue(OrderInfo orderInfo);

    /**
     * 更新订单单号
     * @param orderInfo
     */
    OrderInfo updateOrder(OrderInfo orderInfo);
    //void saveOrder(OrderInfo orderInfo);
}
