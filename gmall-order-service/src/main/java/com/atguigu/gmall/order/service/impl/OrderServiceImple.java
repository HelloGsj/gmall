package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.utils.ActiveMQUtil;
import com.atguigu.gmall.utils.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

/**
 * @author shkstart
 * @create 2018-09-20 12:18
 */
@Service
public class OrderServiceImple implements OrderService {

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;
    /**
     * 保存订单信息
     * @param orderInfo
     */
    @Override
    public void saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);
        //获取插入完的主键id
        String orderInfoId = orderInfo.getId();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //也需要出入数据库的order_detail表
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfoId);
            orderDetailMapper.insertSelective(orderDetail);
        }

    }

    /**
     * 生成交易码
     * @param userId
     * @return
     */
    @Override
    public String genTradeCode(String userId) {
        String uuid = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        //设置一个时间为15分钟的交易码
        jedis.setex("user:" + userId + ":tradeCode",1000*60*15,uuid);
        jedis.close();
        return uuid;
    }

    /**
     * 检查是否有交易码
     * @param userId
     * @param trackCode
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String trackCode) {
        boolean b = false;
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get("user:" + userId + ":tradeCode");
        if(s != null&&s.equals(trackCode)){
            b = true;

            jedis.del("user:" + userId + ":tradeCode");
        }
        jedis.close();
        return b;
    }

    /**
     * 通过外部交易单号，查询订单信息及订单详情信息
     * @param outTradeNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfoBYOutTradeNo(String outTradeNo) {
        //查询订单信息
        Example orderInfoExample = new Example(OrderInfo.class);
        orderInfoExample.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        OrderInfo orderInfo = orderInfoMapper.selectOneByExample(orderInfoExample);
        //获取订单id
        String orderInfoId = orderInfo.getId();
        //通过订单id获取订单详情信息
        Example orderDetailExample = new Example(OrderDetail.class);
        orderDetailExample.createCriteria().andEqualTo("orderId",orderInfoId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectByExample(orderDetailExample);
        //再将订单详情信息保存到订单里面
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    /**
     * 发送消息队列（将订单信息保存发送出去）
     * 用于给库存服务消费
     * @param orderInfo
     */
    @Override
    public void sendOrderResultQueue(OrderInfo orderInfo) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(queue);

            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            //将对象打包成json字符串
            activeMQTextMessage.setText(JSON.toJSONString(orderInfo));

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(activeMQTextMessage);
            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据outTradeNo更新订单信息
     * @param orderInfo
     */
    @Override
    public OrderInfo updateOrder(OrderInfo orderInfo) {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",orderInfo.getOutTradeNo());
        orderInfoMapper.updateByExampleSelective(orderInfo,example);

        return  orderInfoMapper.selectOne(orderInfo);
    }
}
