package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-08 11:31
 */
public interface SkuService {

    List<SkuInfo> getSkuInfoListBySpuId(String spuId);

    void saveSku(SkuInfo skuInfo);

    SkuInfo getSkuById(String skuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId, String skuId);

    List<SkuInfo> getSkuSaleAttrValueListByspuId(String spuId);

    List<SkuInfo> getSkuInfoByCatalog3Id(Integer catalog3Id);
}
