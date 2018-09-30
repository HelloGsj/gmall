package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-19 16:16
 */
@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    SkuService skuService;

    @Reference
    OrderService orderService;

    /**
     * 提交订单，会进到支付界面
     * 提交订单之后不能回退到原来的订单界面再进行提交
     * 检查是否有交易码（tradeCode）,因为第一次生成订单会产生一个15分钟的交易码，如果超过改时间未支付，未提交则会失效，或者已经提交也会失效
     * 检查完交易码之后，会删除该交易码，（该交易码保存在trade页面的隐藏文本中）
     * @param request
     * @param addressId
     * @param map
     * @param tradeCode
     * @return
     */
    @RequestMapping("/submitOrder")
    @LoginRequire(needSuccess = true)
    public String submitOrder(HttpServletRequest request,String addressId,ModelMap map,String tradeCode){
        //获取用户登录信息
        String userId = (String) request.getAttribute("userId");
        //为了防止用户点击结算之后，再退回原来界面，必须阻止可再点击结算功能
        //检查是否有tradeCode(交易码)
        boolean b = orderService.checkTradeCode(userId,tradeCode);
        if(b){
            //获取用户的收货信息
            UserAddress userAddress = userService.getAddressById(addressId);
            //获取购物车信息
            List<CartInfo> cartInfos = cartService.getCartInfoFromCacheByUserId(userId);

            //声明订单对象(并设置订单对象的值)
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setProcessStatus("订单提交");
            orderInfo.setOrderStatus("订单未支付");
            //当前日期加一天
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE,1);
            orderInfo.setExpireTime(calendar.getTime());
            //外部订单号(以下操作用来定制订单号)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = dateFormat.format(new Date());

            String outTradeNo = "atguigugmall" + format + System.currentTimeMillis();
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setConsigneeTel(userAddress.getPhoneNum());
            orderInfo.setCreateTime(new Date());
            orderInfo.setDeliveryAddress(userAddress.getUserAddress());
            orderInfo.setOrderComment("硅谷快递，即时送达");
            orderInfo.setTotalAmount(getTotalPrice(cartInfos));
            orderInfo.setUserId(userId);
            orderInfo.setConsignee(userAddress.getConsignee());
            //使用枚举类对象作为参数
            orderInfo.setPaymentWay(PaymentWay.ONLINE);


            //用来收集购物车编号
            List<String> cartIds = new ArrayList<>();
            //通过选中的购物车，进行封装订单详情对象
            List<OrderDetail> orderDetailList = new ArrayList<>();
            for (CartInfo cartInfo : cartInfos) {
                //需要获取选中的购物车信息
                if(cartInfo.getIsChecked().equals("1")){
                    //将购物车的id保存到集合中（主要提供用来删除购物车）
                    String cartInfoId = cartInfo.getId();
                    cartIds.add(cartInfoId);

                    OrderDetail orderDetail = new OrderDetail();
                    //对订单详情的对象进行封装
                    orderDetail.setSkuName(cartInfo.getSkuName());
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setSkuNum(cartInfo.getSkuNum());

                    //需要验证库存
                    orderDetail.setHasStock("1");

                    //需要验证价格（因为购物对象是从redis中取出）
                    SkuInfo skuInfo = skuService.getSkuById(cartInfo.getSkuId());
                    //如果从数据库获取的相同sku的价格一样
                    if(skuInfo.getPrice().compareTo(cartInfo.getSkuPrice()) == 0 ){
                        //就把购物车的价格赋值给订单详情价格
                        orderDetail.setOrderPrice(cartInfo.getCartPrice());
                    }else {
                        return "orderErr";
                    }

                    //将orderDetail保存到集合中
                    orderDetailList.add(orderDetail);

                }


            }
            //订单详情内容保存到orderInfo里面
            orderInfo.setOrderDetailList(orderDetailList);
            //保存订单到数据库
            orderService.saveOrder(orderInfo);
            //删除购物车数据
            //cartService.deleteCart(StringUtils.join(cartIds,","));
            cartService.deleteCart(cartIds,userId);
            // 提交订单后重定向到支付系统(需要传入外部订单号跟购物车总价)
            return "redirect:http://payment.gmall.com:8087/index?outTradeNo=" + outTradeNo + "&totalAmount=" + getTotalPrice(cartInfos);
        }else {
            return "orderErr";
        }



    }

    /**
     * 用户购物信息核算页面，还不需要将数据保持到数据库
     * 需要生成一个tradeCode交易码，通过保存到界面跟redis中
     * @param request
     * @param map
     * @return
     */
    @RequestMapping("/toTrade")
    @LoginRequire(needSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap map){

        //获取用户信息
        String userId = (String)request.getAttribute("userId");
        //获取用户收货地址
        List<UserAddress> userAddresses = userService.getUserAddressByUserId(userId);
        //接收用户订单信息
        List<OrderDetail> orderDetails = new ArrayList<>();
        //通过用户id获取该用户的购物车信息
        List<CartInfo> cartInfos = cartService.getCartInfoFromCacheByUserId(userId);
        for (CartInfo cartInfo : cartInfos) {
            //只获取被选中的sku商品
            if(cartInfo.getIsChecked().equals("1")){
                //创建一个订单详情，并将购物车相关信息保存进去
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(new BigDecimal(0));
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                //并保存到集合中
                orderDetails.add(orderDetail);
            }

        }
        //保存用户地址跟订单信息到域中
        map.put("userAddressList",userAddresses);
        map.put("orderDetailList",orderDetails);
        map.put("totalAmount",getTotalPrice(cartInfos));

        // 生成一个唯一的交易码
        String tradeCode = orderService.genTradeCode(userId);

        map.put("tradeCode",tradeCode);
        return "trade";
    }

    /***
     * 计算购物车选中的总价格
     * @param cartInfos
     * @return
     */
    private BigDecimal getTotalPrice(List<CartInfo> cartInfos) {

        BigDecimal totalPrice = new BigDecimal("0");

        for (CartInfo cartInfo : cartInfos) {
            String isChecked = cartInfo.getIsChecked();

            if(isChecked.equals("1")){
                totalPrice = totalPrice.add(cartInfo.getCartPrice());
            }
        }

        return totalPrice;
    }
}
