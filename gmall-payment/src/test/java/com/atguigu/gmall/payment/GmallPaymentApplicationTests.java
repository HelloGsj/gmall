package com.atguigu.gmall.payment;

import com.atguigu.gmall.utils.ActiveMQUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.Connection;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    ActiveMQUtil activeMQUtil;
    @Test
    public void contextLoads() {
        Connection connection = activeMQUtil.getConnection();
        //------------PooledConnection { ConnectionPool[ActiveMQConnection {id=ID:GSJ-65287-1537845906915-1:1,clientId=null,started=false}] }
        System.out.println("------------" + connection);
    }

}
