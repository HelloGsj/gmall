package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.conf.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-20 16:29
 */
@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    
    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;

    /**
     * 转发转到选择支付方式页面
     * @param request
     * @param outTradeNo
     * @param totalAmount
     * @param map
     * @return
     */
    @RequestMapping("/index")
    public String index(HttpServletRequest request, String outTradeNo, String totalAmount, ModelMap map){
        map.put("outTradeNo",outTradeNo);
        map.put("totalAmount",totalAmount);
        return "index";
    }

    /**
     * 支付
     */
    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String alipay(String outTradeNo){
        //通过外部交易单号查询该订单信息
        OrderInfo orderInfo = orderService.getOrderInfoBYOutTradeNo(outTradeNo);
        //设置支付宝page.pay的请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        //用于生成一个表单html代码
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",orderInfo.getTotalAmount());
        map.put("subject",orderInfo.getOrderDetailList().get(0).getSkuName());
        map.put("body","产品支付测试");

        String s = JSON.toJSONString(map);
        //填充业务参数
        alipayRequest.setBizContent(s);
        String form = "";
        try {
            //调用SDK生成表单html代码
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        System.out.println(form);
        //支付信息的保存
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        //paymentInfo.setTotalAmount(new BigDecimal(0.01));
        paymentService.savePaymentInfo(paymentInfo);

        //需要使用延迟队列定时检查（目的是，主动访问支付平台，确认支付情况）
        //5代表反复主动询问支付平台5次
        //该方法是延迟队列入口
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),5);


        return form;
    }

    /**
     * 阿里回调的方法
     */
    @RequestMapping("/alipay/callback/return")
    public String alipayReturn(HttpServletRequest request){
        //回调接口首先需要验证的阿里的签名
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(null, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
            if(signVerified){
                //TODO验签成功后，按照支付结果异步通知中的描述，
                // 对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            }else {
                //TODO验签失败则记录异常日志，并在response中返回failure.
            }
        } catch (Exception e) {
            System.out.println("验证阿里的签名");
        }


        // 签名验证通过后，继续执行支付成功的业务
        String alipayTradeNo = (String)request.getParameter("trade_no");
        String callback = request.getQueryString();
        String out_trade_no = (String)request.getParameter("out_trade_no");
        String sign = (String)request.getParameter("sign");
        System.err.println("sign签名="+sign);
        // (支付成功之后)更新支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setAlipayTradeNo(alipayTradeNo);
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(callback);
        paymentInfo.setOutTradeNo(out_trade_no);

        // 更新支付信息,并通知订单系统，更新订单信息
        paymentService.updatePaymentSuccess(paymentInfo);

        return "finish";
    }
}
