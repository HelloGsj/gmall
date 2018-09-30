package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.BaseAttrInfoService;
import com.atguigu.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * @author shkstart
 * @create 2018-09-12 16:27
 */
@Controller
public class ListController {

    @Reference
    ListService listService;

    @Reference
    BaseAttrInfoService baseAttrInfoService;

    @RequestMapping("/list.html")
    public String list(SkuLsParam skuLsParam, ModelMap map) {
        List<SkuLsInfo> skuLsInfos = listService.search(skuLsParam);
        //创建一个set集合String类型，主要是去重复
        Set<String> strings = new HashSet<>();
        for (SkuLsInfo skuLsInfo : skuLsInfos) {

            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
            //遍历通过参数获取到的skuLsInfos里面的平台属性列表
            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                //将他的id保存到set集合可去重复
                String valueId = skuLsAttrValue.getValueId();
                strings.add(valueId);
            }
        }
        //如果匹配不到则跳转到错误页面
        if(strings.size() > 0&&null != strings&&skuLsParam.getKeyword() != null&&skuLsInfos.size() <= 0) {
            return "error";
        }
        //集合间按照逗号隔开，拼接成字符串
        String joins = StringUtils.join(strings, ",");
        //通过sku的平台属性列表，查出所有平台属性信息
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoService.getAttrListByValueId(joins);

        //获取参数的valueId们，遍历所有平台属性值id(baseAttrInfos里面的BaseAttrValue对象)
        //如果有跟参数的valueId一样，则需要删除该平台属性对象，baseAttrInfos对象
        //制作当前请求的面包屑（）
        //创建一个面包屑集合(面包屑的原理：本身点击可跳转地址，url少一个属性，这样点击就会跳回到之前的地址)
        List<Crumb> crumbs = new ArrayList<>();
        //获取valueId
        String[] valueId = skuLsParam.getValueId();
        if (null != valueId && valueId.length > 0) {
            for (String sid : valueId) {
                //创建一个面包屑
                Crumb crumb = new Crumb();
                //排除属性
                Iterator<BaseAttrInfo> iterator = baseAttrInfos.iterator();
                //遍历所有平台属性的属性值，如果有跟参数传过来的值一样，则删除该值的平台属性则需要删除该BaseAttrInfo对象对象
                while (iterator.hasNext()) {
                    BaseAttrInfo baseAttrInfo = iterator.next();
                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

                    //删除当前的valueId所关联的属性对象
                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        String id = baseAttrValue.getId();
                        //如果遍历所有的平台属性值id有跟参数的valueId一致，则需要删除该BaseAttrInfo对象
                        if (id.equals(sid)) {
                            //设置面包屑名称
                            crumb.setValueName(baseAttrValue.getValueName());
                            //删除该平台属性对象
                            iterator.remove();
                        }

                    }
                }
                //每遍历一个valueId就需要增加一个面包屑
                //制作面包屑url
                String crumbsUrlParam = getUrlParam(skuLsParam, sid);
                crumb.setUrlParam(crumbsUrlParam);
                crumbs.add(crumb);

            }
        }


        //显示整个页面sku信息
        map.put("skuLsInfoList", skuLsInfos);
        //平台属性及属性值显示
        map.put("attrList", baseAttrInfos);
        String urlParam = getUrlParam(skuLsParam);
        //对参数添加并返回的url
        map.put("urlParam", urlParam);
        //返回面包屑的名称
        map.put("attrValueSelectedList", crumbs);
        return "list";
    }

    //获取urlParam
    public String getUrlParam(SkuLsParam skuLsParam, String... obj) {
        //查看obj长度
        System.out.println(obj.length);
        String catalog3Id = skuLsParam.getCatalog3Id();
        String[] valueId = skuLsParam.getValueId();
        String keyword = skuLsParam.getKeyword();

        String urlParam = "";

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (null != valueId && valueId.length > 0) {

            for (String id : valueId) {
                if (obj.length <= 0) {
                    urlParam = urlParam + "&" + "valueId=" + id;
                }else {
                    if(!id.equals(obj[0])){
                        urlParam = urlParam + "&" + "valueId=" + id;
                    }
                }

            }

        }


        return urlParam;
    }

}
