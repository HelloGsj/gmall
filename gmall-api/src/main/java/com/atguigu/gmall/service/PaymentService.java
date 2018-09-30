package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-21 16:46
 */
public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendPaymentSuccessQueue(String out_trade_no, String alipayTradeNo);

    void sendDelayPaymentResult(String outTradeNo, int i);

    Map<String,String> checkAlipayPayment(String outTradeNo);

    boolean checkStatus(String outTradeNo);

    void updatePaymentSuccess(PaymentInfo paymentInfo);
}
