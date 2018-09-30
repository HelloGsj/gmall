package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @author shkstart
 * @create 2018-09-25 12:02
 */
public class ConsumerThree {
    public static void main(String[] args){
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_BROKER_URL, ActiveMQConnectionFactory.DEFAULT_PASSWORD, "tcp://192.168.92.200:61616");
        try {
            Connection connection = activeMQConnectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("HESHUI");
            MessageConsumer consumer = session.createConsumer(queue);

            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(message instanceof  TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            System.out.println("------consumerThree" + text);
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
