package com.atguigu.gmall.payment.payMQ;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-26 11:36
 */
@Component
public class PaymentCheckQueueListener {

    @Autowired
    PaymentService paymentService;

    /**
     * 监听消息队列中主题名为PAYMENT_CHECK_QUEUE
     * 信息消费端
     * @param mapMessage
     */
    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerCheckResult(MapMessage mapMessage) throws JMSException {
        //检查次数
        int count = mapMessage.getInt("count");
        String outTradeNo = mapMessage.getString("outTradeNo");
        //调用支付宝检查接口，得到支付状态

        Map<String, String> map = paymentService.checkAlipayPayment(outTradeNo);
        //根据支付情况决定是否调用支付成功队列，还是继续延迟检查
        //交易状态：
        // WAIT_BUYER_PAY（交易创建，等待买家付款）、
        // TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、
        // TRADE_SUCCESS（交易支付成功）、
        // TRADE_FINISHED（交易结束，不可退款）
        if("TRADE_SUCCESS".equals(map.get("status"))){
            //支付状态的幂等性判断
            boolean b = paymentService.checkStatus(outTradeNo);
            //支付成功的话，并且数据库还没更新的话，就需要更新支付信息
            if(!b){
                //交易成功，更新支付信息
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setAlipayTradeNo(map.get("alipayTradeNo"));
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(map.get("callback"));
                paymentInfo.setOutTradeNo(outTradeNo);
                System.out.println("进行第" + (6 - count) + "次检查订单的支付状态，已经支付，更新支付信息发送成功过的队列");
                paymentService.updatePaymentSuccess(paymentInfo);
            }else {
                System.out.println("检查到该比交易已经支付完毕，直接返回结果，消息队列任务结束");
            }
        }else {//如果从支付平台获取的支付信息并不是支付成功状态
            //则需要再进行延迟队列，继续访问支付平台
            if(count > 0){
                System.out.println("进行第"+(6-count)+"次检查订单的支付状态，未支付，继续发送延迟队列");
                //信息生产端
                paymentService.sendDelayPaymentResult(outTradeNo,count - 1);
            }else{
                System.out.println("检查次数上限，用户在规定时间内，没有支付");
            }
        }





    }
}
