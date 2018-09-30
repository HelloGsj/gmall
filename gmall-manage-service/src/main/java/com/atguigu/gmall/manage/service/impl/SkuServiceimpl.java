package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author shkstart
 * @create 2018-09-08 11:32
 */
@Service
public class SkuServiceimpl implements SkuService {

    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;
    @Override
    public List<SkuInfo> getSkuInfoListBySpuId(String spuId) {
        Example example = new Example(SkuInfo.class);
        example.createCriteria().andEqualTo("spuId",spuId);
        List<SkuInfo> skuInfoList = skuInfoMapper.selectByExample(example);
        return skuInfoList;
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {

        //1.保存skuInfo的单个字段的表
        skuInfoMapper.insertSelective(skuInfo);
        //获取保存后的skuId
        String skuId = skuInfo.getId();
        if(skuInfo.getSkuImageList() != null && skuInfo.getSkuImageList().size() > 0){
            //2.保存skuImage的表
            List<SkuImage> skuImageList =skuInfo.getSkuImageList();
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
                skuImageMapper.insertSelective(skuImage);
            }
        }
        if(skuInfo.getSkuAttrValueList() != null && skuInfo.getSkuAttrValueList().size() > 0){
            //3.保存skuAttrValue的表
            List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuId);
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        if(skuInfo.getSkuSaleAttrValueList() != null && skuInfo.getSkuSaleAttrValueList().size() > 0){
            //4.保存skuSaleAttrValue的表
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }


    }


    /***
     * 从db中查询sku详情
     * @param skuId
     * @return
     */
    public SkuInfo getSkuByIdFromDB(String skuId){

        // 查询sku信息
        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setId(skuId);
        System.out.println(skuId);
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoParam);

        // 查询图片集合
        SkuImage skuImageParam = new SkuImage();
        skuImageParam.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImageParam);
        skuInfo.setSkuImageList(skuImages);

        return skuInfo;
    }

    /**
     * 通过skuId获取skuInfo以及skuImage的信息
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuById(String skuId) {
        /*//通过skuId获取skuInfo信息
        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setId(skuId);
        //获取整个skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoParam);
        if(skuInfo != null){
            Example example = new Example(SkuImage.class);
            example.createCriteria().andEqualTo("skuId",skuId);
            List<SkuImage> skuImages = skuImageMapper.selectByExample(example);
            //将skuImages列表封装到skuInfo里面
            skuInfo.setSkuImageList(skuImages);
        }
        return skuInfo;*/

        String custom = Thread.currentThread().getName();

        System.err.println(custom+"线程进入sku查询方法");

        SkuInfo skuInfo = null;
        String skuKey = "sku:"+skuId+":info";

        // 缓存redis查询
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get(skuKey);

        if(StringUtils.isNotBlank(s)&&s.equals("empty")){
            System.err.println(custom+"线程进发现数据库中没有数据，返回");
            return null;
        }

        if(StringUtils.isNotBlank(s)&&!"empty".equals(s)){
            System.err.println(custom+"线程能够从redis中获取数据");
            skuInfo = JSON.parseObject(s, SkuInfo.class);

        }else{
            System.err.println(custom+"线程没有从redis中取出数据库，申请访问数据库的分布式锁");
            // db查询(限制db的访问量)
            String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);
            if(StringUtils.isNotBlank(OK)){
                System.err.println(custom+"线程得到分布式锁，开始访问数据库");
                skuInfo = getSkuByIdFromDB(skuId);
                if(null!=skuInfo){
                    System.err.println(custom+"线程成功访问数据库，删除分布式锁");
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                    jedis.del("sku:" + skuId + ":lock");
                }
            }else{
                System.err.println(custom+"线程需要访问数据库，但是未得到分布式锁，开始自旋");
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                     e.printStackTrace();
//                }

                // 自旋
                jedis.close();
                return  getSkuById(skuId);
            }

            if(null==skuInfo){
                System.err.println(custom+"线程访问数据库后，发现数据库为空，将空值同步redis");
                jedis.set(skuKey,"empty");
            }

            // 同步redis
            System.err.println(custom+"线程将数据库中获取数据同步redis");
            if(null!=skuInfo&&!"empty".equals(s)){
                jedis.set(skuKey,JSON.toJSONString(skuInfo));
            }

        }
        System.err.println(custom+"线程结束访问返回");

        jedis.close();
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId, String skuId){
        Integer spuIdInteger = 1;
        Integer skuIdInteger = 1;
        if(StringUtils.isNotBlank(spuId)){
            spuIdInteger = Integer.parseInt(spuId);
        }else{
        }
        if(StringUtils.isNotBlank(skuId)){
            skuIdInteger = Integer.parseInt(skuId);
        }else {
        }
        List<SpuSaleAttr> spuSaleAttrs = skuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(spuIdInteger,skuIdInteger);
        return spuSaleAttrs;
    }

    @Override
    public List<SkuInfo> getSkuSaleAttrValueListByspuId(String spuId) {
        List<SkuInfo> skuInfos = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Integer.parseInt(spuId));
        return skuInfos;
    }

    //通过catalo3Id获取skuInfo信息

    @Override
    public List<SkuInfo> getSkuInfoByCatalog3Id(Integer catalog3Id){
        //获取到skuInfo信息
        Example example = new Example(SkuInfo.class);
        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
        List<SkuInfo> skuInfoList = skuInfoMapper.selectByExample(example);

        System.out.println(skuInfoList.size());

        //
        for (SkuInfo skuInfo : skuInfoList) {
            //获取skuINfo的id
            String skuId = skuInfo.getId();
            //通过skuInfo的id获取到skuImage的信息
            Example skuImageExample = new Example(SkuImage.class);
            skuImageExample.createCriteria().andEqualTo("skuId",skuId);
            List<SkuImage> skuImages = skuImageMapper.selectByExample(skuImageExample);
            //需要将图片信息保存到每一个skuInfo里面
            skuInfo.setSkuImageList(skuImages);
            //通过skuInfo的id获取到skuattrValue信息
            Example skuAttrValueExample = new Example(SkuAttrValue.class);
            skuAttrValueExample.createCriteria().andEqualTo("skuId",skuId);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.selectByExample(skuAttrValueExample);
            //需要将每一个skuAttrValues保存到每一个skuInfo里面
            skuInfo.setSkuAttrValueList(skuAttrValues);
        }

        return skuInfoList;
    }

}
