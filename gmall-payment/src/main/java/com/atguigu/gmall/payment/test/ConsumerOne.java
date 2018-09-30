package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @author shkstart
 * @create 2018-09-25 11:38
 */
public class ConsumerOne {
    public static void main(String[] args){
        ActiveMQConnectionFactory mqConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_USER, ActiveMQConnectionFactory.DEFAULT_PASSWORD, "tcp://192.168.92.200:61616");
        Connection connection = null;
        Session session = null;
        try {
            connection = mqConnectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue heshui = session.createQueue("HESHUI");

            MessageConsumer consumer = session.createConsumer(heshui);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(message instanceof TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            System.out.println("-----consumerOne" + text);
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
