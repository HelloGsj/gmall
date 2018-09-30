package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-05 19:08
 */
@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SpuImageMapper spuImageMapper;

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Override
    public List<SpuInfo> getSpuByCtg3(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfos = spuInfoMapper.select(spuInfo);
        return spuInfos;
    }

    @Override
    public List<BaseSaleAttr> getbaseSaleAttr() {
        List<BaseSaleAttr> baseSaleAttrs = baseSaleAttrMapper.selectAll();
        return baseSaleAttrs;
    }

    @Override
    public void saveSpu(SpuInfo spuInfo) {
        //1.保存spuInfo的基础信息
        spuInfoMapper.insertSelective(spuInfo);
        //2.需要获取保存后的spuInfo的id
        String spuInfoId = spuInfo.getId();
            //遍历所有spuimg
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage spuImage : spuImageList) {
            //设置spuInfoId为保存完的id
            spuImage.setSpuId(spuInfoId);
            //保存spuImg的表
            spuImageMapper.insertSelective(spuImage);
        }
        //3.保存SpuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            //设置spuInfoId为保存完的id
            spuSaleAttr.setSpuId(spuInfoId);
            spuSaleAttrMapper.insertSelective(spuSaleAttr);
            //4.保存销售属性值
            //1)获取spuSaleAttrValueList的列表属性
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            //2)遍历属性列表，对属性值的各个属性赋值
            for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                spuSaleAttrValue.setSpuId(spuInfoId);
                spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
            }

        }



    }

    @Override
    public List<SpuImage> getspuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> select = spuImageMapper.select(spuImage);
        return select;
    }

    /**
     * 获取销售属性跟销售属性值，并封装到销售属性对象里面
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuId);
        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.select(spuSaleAttr);
        if(spuSaleAttrs != null&& spuSaleAttrs.size()>0){
            for (SpuSaleAttr saleAttr : spuSaleAttrs) {
                //通过spuid跟销售属性id才能获取对应的销售属性值
                SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
                spuSaleAttrValue.setSpuId(spuId);
                spuSaleAttrValue.setSaleAttrId(saleAttr.getSaleAttrId());
                List<SpuSaleAttrValue> spuSaleAttrValues = spuSaleAttrValueMapper.select(spuSaleAttrValue);
                saleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);
            }
        }
        return spuSaleAttrs;
    }

    /**
     * 通过id删除spuInfo信息
     * @param id
     */
    @Override
    public void deleteSpuInfoById(String id) {
        //1.删除spuInfo信息
        spuInfoMapper.deleteByPrimaryKey(id);
        //2.删除spuImage的信息
        Example spuImageExample = new Example(SpuImage.class);
        spuImageExample.createCriteria().andEqualTo("spuId",id);
        spuImageMapper.deleteByExample(spuImageExample);
        //3.删除spuSaleAttr的信息
        Example spuSaleAttrExample = new Example(SpuSaleAttr.class);
        spuSaleAttrExample.createCriteria().andEqualTo("spuId",id);
        spuSaleAttrMapper.deleteByExample(spuSaleAttrExample);
        //4.删除spuSalueAttrValue的信息
        Example spuSaleAttrValueExample = new Example(SpuSaleAttrValue.class);
        spuSaleAttrValueExample.createCriteria().andEqualTo("spuId",id);
        spuSaleAttrValueMapper.deleteByExample(spuSaleAttrValueExample);

    }

    @Override
    public SpuInfo getspuinfoById(String id) {
        SpuInfo spuInfoParam = new SpuInfo();
        spuInfoParam.setId(id);
        SpuInfo spuInfo = spuInfoMapper.selectOne(spuInfoParam);
        return spuInfo;
    }

    @Override
    public PageInfo<SpuInfo> getspuInfosByCatalog3Id(String catalog3Id, String page, String rows) {
        PageHelper.startPage(Integer.parseInt(page) ,Integer.parseInt(rows));
        Example example = new Example(SpuInfo.class);
        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
        List<SpuInfo> spuInfos = spuInfoMapper.selectByExample(example);
        PageInfo<SpuInfo> spuInfoPageInfo = new PageInfo<>(spuInfos);
        return spuInfoPageInfo;
    }
}
