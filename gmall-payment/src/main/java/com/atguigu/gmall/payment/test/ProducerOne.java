package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * @author shkstart
 * @create 2018-09-25 10:11
 */
public class ProducerOne {
    public static void main(String[] args) {
        //创建一个消息连接工厂
        ActiveMQConnectionFactory mqConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.92.200:61616");
        try {
            //通过工厂创建一个连接
            Connection connection = mqConnectionFactory.createConnection();
            //连接启动
            connection.start();
            //创建一个session事务
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建一个队列
            Queue queue = session.createQueue("HESHUI");
            //创建一个提供者，（参数为创建的队列信息）
            MessageProducer producer = session.createProducer(queue);
            //创建一个消息文本信息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            //设置文本内容
            activeMQTextMessage.setText("口渴，需要一杯水");
            //设置交货模式为持久（也就是该提供者持久化）
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            //发送该消息队列
            producer.send(activeMQTextMessage);
            //需要提交该session，并且关闭连接
            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
