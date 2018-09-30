package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-04 18:04
 */
public interface BaseAttrInfoService {

    List<BaseAttrInfo> getAttrListsByCata3Id(String catalog3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValuesByCata3Id(String attrId);

    /**
     * 用于分页
     * @param catalog3Id
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageInfo<BaseAttrInfo> getAttrListByCtg3Page(String catalog3Id, String pageNum, String pageSize);

    void deleteAttrInfo(String attrId);

    List<BaseAttrInfo> getAttrListByValueId(String join);
}
