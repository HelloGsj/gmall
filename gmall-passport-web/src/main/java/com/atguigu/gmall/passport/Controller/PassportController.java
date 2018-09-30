package com.atguigu.gmall.passport.Controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.PassportService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * @author shkstart
 * @create 2018-09-18 17:19
 */
@Controller
public class PassportController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    PassportService passportService;
    /**
     * 拦截需要登录的界面之后，获取拦截的地址，把他当做参数放到index界面中
     * 例如：http://order.gmall.com:8086/toTrade?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6Ik1ySiIsInVzZXJJZCI6IjEifQ.Wu1FIYX9SDOpPnBDScg50fjvfB__upowzieXSi8oER8
     * @param map
     * @param returnUrl
     * @return
     */
    @RequestMapping("/index")
    public String index( ModelMap map,String returnUrl){
        map.put("originUrl",returnUrl);
        return "index";
    }


    /**
     * 用户点击登录后，会跳转到之前未登录的页面，并把token当做参数放在url地址中
     * @return
     */
    @RequestMapping("/login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request, ModelMap map, HttpServletResponse response){

        // 验证用户名和密码
        UserInfo user = userService.login(userInfo);
        if(user == null){
            //提示用户名不存在或者密码错误
            return "err";
        }else{
            // 通过用户信息跟用户客户端的ip地址生成token
            // 然后将该用户的用户信息从db中提取到redis，设置该用户的过期时间(该步骤在uer的service层login方法设置)
            Map<String,String> userMap = new HashMap<>();
            userMap.put("userId",user.getId());
            userMap.put("nickName",user.getNickName());
            //获取客户端的ip地址
            String ip = getMyIpFromRequest(request);
            //token的生成
            String token = JwtUtil.encode("atguigugmall0508", userMap, ip);
            //获取购物车cookie的值
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);

            //合并购物车

            //将cookie的数据保存到数据库  (已使用消息队列，所以不需要该步骤)
            // cartService.combin(user.getId(), JSON.parseArray(listCartCookie,CartInfo.class));(已使用消息队列，所以不需要该步骤)

            //将cookie的数据发送到消息队列
            //passportService.sendPassportSuccess(user.getId(), JSON.parseArray(listCartCookie,CartInfo.class));
            passportService.sendPassportSuccess(user.getId(), listCartCookie);
            //整合到购物车数据库之后，需要删除cookie的值
            CookieUtil.deleteCookie(request,response,"listCartCookie");
            return token;
        }

    }

    /**
     * 验证用户token
     * @param request
     * @param token
     * @param currentIp
     * @param map
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request,String token,String currentIp,ModelMap map){
        //验证用户token

        try {
            //token的解码
            Map decode = JwtUtil.decode("atguigugmall0508", token, currentIp);
            if(decode != null){
                //验证token对应的用户信息的过期时间
                return "success";
            }else{
                return "fail";
            }
        } catch (Exception e) {
            return "fail";
        }
    }

    /**
     * 获取客户端ip
     * @param request
     * @return
     */
    public String getMyIpFromRequest(HttpServletRequest request){
        String ip = "";
        //获取客户端的ip地址
        ip = request.getRemoteAddr();
        if(StringUtils.isBlank(ip)){
            ip = request.getHeader("x-forwarder-for");
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }
        return ip;
    }
}
