package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-09 21:20
 */
public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("spuId") Integer spuId, @Param("skuId") Integer skuId);

    List<SkuInfo> selectSkuSaleAttrValueListBySpu(@Param("spuId")Integer spuId);
}
