package com.atguigu.gmall.passport.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.service.PassportService;
import com.atguigu.gmall.utils.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.*;

/**
 * @author shkstart
 * @create 2018-09-27 16:13
 */
@Service
public class PassportServiceImpl implements PassportService {

    @Autowired
    ActiveMQUtil activeMQUtil;

    /**
     * 将成功登陆信息发送到队列
     * 此时需要将cookie里的购物车信息合并到数据库
     * @param id
     * @param cartInfosCookie
     */
    @Override
    public void sendPassportSuccess(String id, String cartInfosCookie) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("PASSPORT_SUCCESS");
            MessageProducer producer = session.createProducer(queue);

            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("userId",id);
            activeMQMapMessage.setObject("cartInfosCookie",cartInfosCookie);

           new ActiveMQTextMessage();

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(activeMQMapMessage);

            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


}
