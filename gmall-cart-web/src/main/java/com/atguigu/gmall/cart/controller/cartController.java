package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-14 20:43
 */
@Controller
public class cartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;



    /**
     * 点击购物车的勾选按钮，
     * 会异步请求该方法
     * @param map
     * @return
     */
    @LoginRequire(needSuccess = false) //不要求一定要登录才能放行
    @RequestMapping("/checkCart")
    public String checkCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo, Map<String, Object> map) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        String skuId = cartInfo.getSkuId();
        String userId = "";  //设置用户未登录
        //需要先判断用户是否登录
        //如果用户已登录
        if (StringUtils.isNotBlank(userId)) {
            //修改数据库(service层需要同步缓存)
            //需要设置用户id
            cartInfo.setUserId(userId);
            cartService.updateCartByUserId(cartInfo);
            //查询购物车
            cartInfoList = cartService.getCartInfoFromCacheByUserId(userId);
            //用户没有登录
        } else {
            //获取cooKie的对象（数组格式）
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            //将对象转为CartInfo
            cartInfoList = JSON.parseArray(listCartCookie, CartInfo.class);
            //遍历cookie的对象
            for (CartInfo info : cartInfoList) {
                String infoSkuId = info.getSkuId();
                //如果遍历的skuId跟传过来的skuId一致的话，需要将isChecked字段修改为最新的
                if (infoSkuId.equals(skuId)) {
                    info.setIsChecked(cartInfo.getIsChecked());
                }
            }
            //修改完之后，需要更新到cookie中
            CookieUtil.setCookie(request, response, "listCartCookie", JSON.toJSONString(cartInfoList), 1000 * 60 * 60 * 24, true);

        }
        map.put("cartList", cartInfoList);

        //对象修改完之后，需要对总价进行修改
        BigDecimal totalPrice = getTotalPrice(cartInfoList);
        map.put("totalPrice", totalPrice);
        return "cartListinner";
    }

    /**
     * 从cookie或者redis中获取购物车信息
     *
     * @param request
     * @param map
     * @return
     */
    @LoginRequire(needSuccess = false) //不要求一定要登录才能放行
    @RequestMapping("/cartList")
    public String cartList(HttpServletRequest request, ModelMap map) {
        //声明一个购物车
        List<CartInfo> cartInfoList = new ArrayList<>();
        String userId = "";
        //获取购物车集合
        //如果用户id为空,从cookie获取
        if (StringUtils.isBlank(userId)) {
            //cookieValue是数组类型保存的
            String cookieValue = CookieUtil.getCookieValue(request, "listCartCookie", true);
            if (StringUtils.isNotBlank(cookieValue)) {
                cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);

            }
        } else {
            //从redis获取
            cartInfoList = cartService.getCartInfoFromCacheByUserId(userId);
        }
        map.put("cartList", cartInfoList);
        BigDecimal totalPrice = getTotalPrice(cartInfoList);
        map.put("totalPrice", totalPrice);
        return "cartList";
    }

    /**
     * 将所有选中的cartInfo所有价格加起来
     *
     * @param cartInfoList
     * @return
     */
    private BigDecimal getTotalPrice(List<CartInfo> cartInfoList) {
        BigDecimal totalPrice = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            //只计算被选上的购物车价格
            if (cartInfo.getIsChecked().equals("1")) {
                totalPrice = totalPrice.add(cartInfo.getCartPrice());
            }
        }
        return totalPrice;
    }

    /**
     * 添加购物车需要重定向
     * （如果是异步，会导致用户添加过快，导致服务器出问题几率大）
     * 添加购物车到cookie或者到数据库（根据是否有用户id）
     *
     * @return
     */
    @LoginRequire(needSuccess = false) //不要求一定要登录才能放行
    @RequestMapping("/addToCart")
    public String addToCart(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> map,RedirectAttributes redirectAttributes) {
        //需要创建一个购物车
        List<CartInfo> cartInfoList = new ArrayList<>();
        //用户是否登录
        String userId = "";

        String skuId = map.get("skuId");
        String numString = map.get("num");
        Integer num = Integer.parseInt(numString);
        //获取一个skuInfo
        SkuInfo skuInfo = skuService.getSkuById(skuId);
        //创建一个购物车
        CartInfo cartInfo = new CartInfo();

        cartInfo.setCartPrice(skuInfo.getPrice().multiply(new BigDecimal(num)));
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setIsChecked("1");
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuNum(num);
        cartInfo.setSkuPrice(skuInfo.getPrice());

        //判断用户是否登录

        //如果用户没登录
        if (StringUtils.isBlank(userId)) {
            //添加购物车业务逻辑
            //cookie没有用户id
            cartInfo.setUserId("");
            //用户未登录时添加购物车
            //获取cookieValue（是json格式的数组
            //   [
            //     {
            //          "cartPrice":11111,"imgUrl":"http://192.168.92.200/group1/M00/00/00/wKhcyFuTHR2AKqVoAAGMftwW1r0708.jpg",
            //          "isChecked":"1",
            //          "skuId":"99",
            //          "skuName":"牛X电子书手机",
            //          "skuNum":1,
            //          "skuPrice":11111,
            //          "userId":""
            //     }
            //   ]
            // ）   isdecoder是否解码

            //cookieValue 即cookie的Id为：listCartCookie
            String cookieValue = CookieUtil.getCookieValue(request, "listCartCookie", true);
            //如果获取的cookieValue是空的
            if (StringUtils.isBlank(cookieValue)) {
                cartInfoList.add(cartInfo);
                //如果有
            } else {
                //获取购物车信息（因为json对象，所以用JSON来获取）
                cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
                //判断时更新还是添加
                boolean b = ifNewCart(cartInfoList, cartInfo);
                //如果是true，则添加，否则就更新
                if (b) {
                    //添加
                    cartInfoList.add(cartInfo);
                } else {
                    //更新
                    //遍历购物车
                    for (CartInfo info : cartInfoList) {
                        //如果是同件商品的话，需要修改数量跟金额
                        if (info.getSkuId().equals(cartInfo.getSkuId())) {
                            //购物车的数量加上新增一个相同商品的数量
                            info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                            //购物车相同商品的金额为 sku数量乘以sku单价
                            info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        }
                    }
                }
            }
            //之后将购物车数据放入到cookie(不管是保存还是添加（添加的话会覆盖）)
            CookieUtil.setCookie(request, response, "listCartCookie", JSON.toJSONString(cartInfoList), 1000 * 60 * 60 * 24, true);
            //用户登录
        } else {
            //数据库有用户id
            cartInfo.setUserId(userId);
            //添加购物车业务逻辑
            //是否存在该购物车
            CartInfo cartInfDb = cartService.ifCartExits(cartInfo);

            //如果存在，则更新，否则添加
            if (null != cartInfDb) {
                //设置数量，设置购物车总价格，就可以直接用该对象当插入对象
                cartInfDb.setSkuNum(cartInfDb.getSkuNum() + cartInfo.getSkuNum());
                cartInfDb.setCartPrice(cartInfDb.getSkuPrice().multiply(new BigDecimal(cartInfDb.getSkuNum())));
                cartService.updateCart(cartInfDb);
            } else {
                //添加
                cartService.insertCart(cartInfo);
            }


            //（redis只负责数据库曾层面）不管添加还是修改都需要同步redis缓存
            cartService.flushCartCacheByUserId(userId);
        }

        redirectAttributes.addAttribute("skuId",skuId);
        //重定向到cartSuccess，用户重复刷新也不会添加了
        return "redirect:/cartSuccess";
    }

    /**
     * 如果添加过来的商品的skuId跟购物车里面的skuId一致，则是已存在
     *
     * @param cartInfoList
     * @param cartInfo
     * @return
     */
    private boolean ifNewCart(List<CartInfo> cartInfoList, CartInfo cartInfo) {
        boolean b = true;
        for (CartInfo info : cartInfoList) {
            if (info.getSkuId().equals(cartInfo.getSkuId())) {
                b = false;
            }
        }
        return b;
    }

    @LoginRequire(needSuccess = false) //不要求一定要登录才能放行
    @RequestMapping("/cartSuccess")
    public String cartSuccess() {

        return "success";
    }

}
