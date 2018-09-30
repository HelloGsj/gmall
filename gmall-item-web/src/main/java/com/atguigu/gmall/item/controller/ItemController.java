package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.SkuService;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-10 9:42
 */
@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    Log log = LogFactory.getLog(ItemController.class);

    /**
     * demo测试类可忽视
     *
     * @param map
     * @return
     */
    @RequestMapping("/demo")
    public String demo(Map<String,Object> map){
            List<String> arrayList = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double random = Math.random();
                String s = String.valueOf(random);
                arrayList.add(s);
            }
            map.put("divText","div文本");
            map.put("inputValue","input值");
            map.put("lists",arrayList);
            map.put("num",1);

        return "demo";
    }

    /**
     * 三个功能点：
     *  1. 通过skuId找到skuInfo信息的spuId,之后通过spuId跟skuId找到相关的销售属性值，为了获取到他的兄弟表（显示在界面可供调用）
     *  3. 能通过skuId跳转到对应的html页面
     *  3. 主要是能通过选择销售属性值找到唯一的skuId
     *
     * @param skuId
     * @param map
     * @return
     */
    @RequestMapping("/{skuId}.html")
    public String index(@PathVariable String skuId, Map<String,Object> map){


        SkuInfo skuInfo = skuService.getSkuById(skuId);
        if(skuInfo != null){
            //获取skuInfo信息
            map.put("skuInfo",skuInfo);
            String spuId = skuInfo.getSpuId();
            //通过spuId,skuId获取spu销售属性bean对象里面的属性(界面需要显示销售属性，还有销售属性值)
            List<SpuSaleAttr> spuSaleAttrs =  skuService.getSpuSaleAttrListCheckBySku(spuId,skuId);
            map.put("spuSaleAttrListCheckBySku",spuSaleAttrs);

            //查询sku的兄弟的hash表hashMap
            //查询了一个spuId下所有的sku的销售属性及销售属性值，就可以通过选择销售属性值，定义到一个sku
            //通过skuId定义到一个界面
            Map<String,String> skuMap = new HashMap<>();
            //通过spuId查询所有sku的销售信息
            List<SkuInfo> skuInfoList =  skuService.getSkuSaleAttrValueListByspuId(spuId);

            log.debug("打印的结果为：{}。" + skuInfoList.size());

            for (SkuInfo info : skuInfoList) {
                //获取skuId
                String skuIdValue = info.getId();
                String skuSalesKey = "";
                //获取sku的销售信息
                //将一个skuId对应的销售属性值作为key,skuId作为value保存到map中
                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
                for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                    String saleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();
                    //一个skuId对应的销售属性值作为key,格式大概为：78|89
                    skuSalesKey = skuSalesKey + "|" + saleAttrValueId;
                }
                //保存到map中，格式大概为: 78|89 : 95
                skuMap.put(skuSalesKey,skuIdValue);
            }
            //用json工具将map转为json字符串
            String skuMapString = JSON.toJSONString(skuMap);
            map.put("skuMapJson",skuMapString);
            //查看json对象

        }

        return "item";
    }

}
