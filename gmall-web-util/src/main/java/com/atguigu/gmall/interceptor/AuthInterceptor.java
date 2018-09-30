package com.atguigu.gmall.interceptor;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-18 15:09
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HandlerMethod handlerMethod = (HandlerMethod)handler;
        //获取被拦截方法的注解
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        //如果方法没有LoginRequire的注解，则放行，不需要拦截
        if(null == loginRequire){
            return true;
        }
        //通过方法的该注解，判断是否需要登录
        boolean b = loginRequire.needSuccess();
        String token = "";
        //从浏览器地址栏中获取token,说明用户第一次登录
        String newToken = request.getParameter("token");
        //从用户cookie中获得token,说明用户登录过一次
        String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);

        //oldToken空，newToken空，从未登录
        if(StringUtils.isBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
            //如要将newToken赋值给token（即token设置为空）
            token = newToken;
            //并且设置值到cookie中（cookie也需要设置为空）
            CookieUtil.setCookie(request,response,"oldToken",newToken,1000*60*60*24,true);
        }
        // oldToken不空，newToken空，之前登陆过
        if(StringUtils.isNotBlank(oldToken)&&StringUtils.isBlank(newToken)){
            token = oldToken;
        }
        // oldToken不空，newToken不空，证明cookie中的token过期（过期就需要一个新的token）
        if(StringUtils.isNotBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
            token = newToken;
        }

        //验证 如果token不为空
        if(StringUtils.isNotBlank(token)){
            // 进行验证
            //获取客户端ip
            String ip = getMyIpFromRequest(request);
            //远程调用verify认证中心，验证token  currentIp参数即客户端ip地址
            String url = "http://passport.gmall.com:8085/verify?token=" + token + "&currentIp="+getMyIpFromRequest(request);// 一个基于http的rest风格的webservice请求
            //会异步访问url地址，并返回该方法的返回值
            String success = HttpClientUtil.doGet(url);

            //如果验证成功
            if(success.equals("success")){
                // 将token重新写入浏览器cookie，需要刷新用户token的过期时间
                CookieUtil.setCookie(request,response,"oldToken",token,1000*60*60*24,true);

                // 将用户信息放入请求中

                Map atguigugmall0508 = JwtUtil.decode("atguigugmall0508", token, ip);
                request.setAttribute("userId",atguigugmall0508.get("userId"));
                request.setAttribute("nickName",atguigugmall0508.get("nickName"));
                return true;
            }

        }
        if(b){
            //token为空，并且要求登录，不给放行
            //重定向登录界面，参数是为了返回前一个页面所需
            response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + request.getRequestURL());
            return false;
        }else {
            //token为空，并且不要求登录，可以放行
            return true;
        }

    }
    /***
     * 获得客户端ip
     * @param request
     * @return
     */
    private String getMyIpFromRequest(HttpServletRequest request) {
        String ip = "";
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
