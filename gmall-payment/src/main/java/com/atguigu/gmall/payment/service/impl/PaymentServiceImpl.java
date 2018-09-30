package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.utils.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-21 16:47
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentMapper paymentMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;
    /**
     * 保存支付信息
     * @param paymentInfo
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentMapper.insertSelective(paymentInfo);
    }

    /**
     * 更新支付信息
     * @param paymentInfo
     */
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentMapper.updateByExampleSelective(paymentInfo,example);
    }

    /**
     * 将信息发送到消息队列
     * @param out_trade_no
     */
    @Override
    public void sendPaymentSuccessQueue(String out_trade_no,String tradeNo) {
        Connection connection = activeMQUtil.getConnection();
        try {
            //true表示使用事务，如果第一个值是true,则第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建一个队列对象
            Queue queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            //通过session创建一个提供者
            MessageProducer producer = session.createProducer(queue);

            //创建一个map的消息队列（用来保存信息到队列里）
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",out_trade_no);
            activeMQMapMessage.setString("tradeNo",tradeNo);

            //设置交货模式为持久化
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            //发送消息到消息队列
            producer.send(activeMQMapMessage);
            //提交session
            session.commit();
            //关闭连接
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 定时检查支付平台支付状况
     * @param outTradeNo
     * @param count
     */
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int count) {
        System.out.println("----------------开始发送延迟检查支付状态的队列---------------");
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //设置放入队列的主题名称
            Queue queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            //保存外部订单号跟此时已执行的次数
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("count",count);
            //（ScheduledMessage.AMQ_SCHEDULED_DELAY）即设置延迟投递的时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*10);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            //发出消息
            producer.send(activeMQMapMessage);

            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     * 通过外部订单号检查支付状态
     * @param outTradeNo
     * @return
     */
    @Override
    public Map<String,String> checkAlipayPayment(String outTradeNo) {
        Map<String,String> returnMap = new HashMap<String,String>();

        System.out.println("开始检查支付宝支付状态");
        //创建一个交易的请求，将交易订单号传过去
        AlipayTradeQueryRequest tradeQueryRequest = new AlipayTradeQueryRequest();
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        //将数据格式以json对象保存
        tradeQueryRequest.setBizContent(JSON.toJSONString(map));

        AlipayTradeQueryResponse execute = null;
        try {
            //执行该请求，得到交易订单号相关的一些信息
            execute = alipayClient.execute(tradeQueryRequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //如果执行成功,则证明调用成功
        if(execute.isSuccess()){
            System.out.println("调用成功");
            //获取该订单的交易状态
            //交易状态：
            // WAIT_BUYER_PAY（交易创建，等待买家付款）、
            // TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、
            // TRADE_SUCCESS（交易支付成功）、
            // TRADE_FINISHED（交易结束，不可退款）
            String tradeStatus = execute.getTradeStatus();
            //获取支付宝交易单号
            String tradeNo = execute.getTradeNo();

            //如果交易状态不为空
            if(StringUtils.isNotBlank(tradeStatus)){
                System.out.println("---------------------支付成功----------------");
                //则通过已定义一个map，将需要的数据保存到map里面
                returnMap.put("status",tradeStatus);
                returnMap.put("alipayTradeNo",execute.getTradeNo());
                returnMap.put("callback",execute.getMsg());
                return returnMap;
            }else {
                //如果没有交易信息，则返回一个fail的状态
                returnMap.put("status","fail");
                return returnMap;
            }


        } else {
            System.out.println("用户未创建交易");
            //如果执行不成功，也需要返回一个fail的状态
            returnMap.put("status","fail");
            return returnMap;

        }
    }

    /**
     * 通过outTradeNo查看该支付信息的状态
     * @param outTradeNo
     * @return
     */
    @Override
    public boolean checkStatus(String outTradeNo) {
        boolean b = false;
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentMapper.selectOne(paymentInfo);
        if("已支付".equals(paymentInfo.getPaymentStatus())){
            b = true;
        }
        return b;
    }

    /**
     * 更新支付信息，并通知订单系统，更新订单信息
     * @param paymentInfo
     */
    @Override
    public void updatePaymentSuccess(PaymentInfo paymentInfo) {
        updatePayment(paymentInfo);
        //通知订单系统，让其更新订单信息
        sendPaymentSuccessQueue(paymentInfo.getOutTradeNo(),paymentInfo.getAlipayTradeNo());
    }
}
