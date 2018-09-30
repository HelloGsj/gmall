package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.github.pagehelper.PageInfo;


import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-05 19:04
 */
public interface SpuService {
    List<SpuInfo> getSpuByCtg3(String catalog3Id);

    List<BaseSaleAttr> getbaseSaleAttr();

    void saveSpu(SpuInfo spuInfo);

    List<SpuImage> getspuImageList(String spuId);

    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    void deleteSpuInfoById(String id);

    SpuInfo getspuinfoById(String id);

    PageInfo<SpuInfo> getspuInfosByCatalog3Id(String catalog3Id, String page, String rows);
}
