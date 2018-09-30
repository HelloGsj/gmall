package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;

import java.util.List;
import java.util.Set;

/**
 * @author shkstart
 * @create 2018-09-12 16:25
 */
public interface ListService {

    List<SkuLsInfo> search(SkuLsParam skuLsParam);



}
