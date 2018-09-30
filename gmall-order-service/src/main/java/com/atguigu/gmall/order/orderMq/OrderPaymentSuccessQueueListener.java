package com.atguigu.gmall.order.orderMq;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Calendar;

/**
 * @author shkstart
 * @create 2018-09-25 15:03
 */
@Component
public class OrderPaymentSuccessQueueListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException{
        String outTradeNo = mapMessage.getString("outTradeNo");
        String tradeNo = mapMessage.getString("tradeNo");
        System.err.println(outTradeNo+"该订单已经支付成功，根据这个消息，进行订单的后续业务");
        // 订单消费支付消息的业务
        // 订单状态、支付方式、预计送达时间、支付宝交易号、整体状态
        OrderInfo orderInfo = new OrderInfo();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,3);

        orderInfo.setOrderStatus("订单已支付");
        orderInfo.setProcessStatus("订单已支付");
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setTrackingNo(tradeNo);
        orderInfo.setExpectDeliveryTime(calendar.getTime());
        //更新订单
        orderService.updateOrder(orderInfo);

        //需要发送订单状态通知到消息队列
        orderService.sendOrderResultQueue(orderInfo);
    }
}
