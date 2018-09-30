package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.manage.mapper.AttrInfoMapper;
import com.atguigu.gmall.manage.mapper.AttrValeMapper;
import com.atguigu.gmall.manage.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.service.BaseAttrInfoService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;


/**
 * @author shkstart
 * @create 2018-09-04 18:19
 */
@Service
public class AttrServiceImpl implements BaseAttrInfoService {

    @Autowired
    AttrInfoMapper attrInfoMapper;

    @Autowired
    AttrValeMapper attrValeMapper;

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;




    @Override
    public List<BaseAttrInfo> getAttrListsByCata3Id(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> baseAttrInfos = attrInfoMapper.select(baseAttrInfo);
        if(baseAttrInfo != null && baseAttrInfos.size()>0){
            //需要循环遍历，将销售属性值添加到销售属性里面
            for (BaseAttrInfo attrInfo : baseAttrInfos) {
                String attrInfoId = attrInfo.getId();
                BaseAttrValue baseAttrValue = new BaseAttrValue();
                baseAttrValue.setAttrId(attrInfoId);
                List<BaseAttrValue> baseAttrValues = attrValeMapper.select(baseAttrValue);
                attrInfo.setAttrValueList(baseAttrValues);
            }
        }
        return baseAttrInfos;
    }

    /**
     *如果BaseAttrInfo里面没有id，则保存属性信息，
     * 如果BaseAttrInfo里面有id，则更新数据（先删除再保存）
     *
     */
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        String id = baseAttrInfo.getId();
        //如果id为空，则做保存操作
        if(StringUtils.isBlank(id)){
            //需要将增加的新的属性信息保存到数据库
            attrInfoMapper.insertSelective(baseAttrInfo);
            //获取新增的属性的id(需要在javabean对id字段设置为可获取id,因为事务还没提交完，数据库没有id信息)
            String attid = baseAttrInfo.getId();

//            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
//            for (BaseAttrValue attrValue : attrValueList) {
//                //循环遍历获取的每个attrValue对象还没有id,需要给他设置一个id,
//                attrValue.setAttrId(attid);
//                //往baseattrValue插入值
//                attrValeMapper.insert(attrValue);
//            }
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            if(attrValueList != null){
                //将所有attrValue值遍历并插入到数据库
                insertAttrValues(baseAttrInfo,attid);
            }
            //如果id不为空，则做更新操作
        }else{
            //更新attrINfo的数据
            attrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
            //根据attrid,删除对应id的所有信息，再进行添加操作
            Example example = new Example(BaseAttrValue.class);
            example.createCriteria().andEqualTo("attrId",id);
            attrValeMapper.deleteByExample(example);

            String attid = baseAttrInfo.getId();
            //将所有attrValue值遍历并插入到数据库
            insertAttrValues(baseAttrInfo,attid);
        }


    }

    @Override
    public List<BaseAttrValue> getAttrValuesByCata3Id(String attrId) {
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",attrId);
        List<BaseAttrValue> attrValues = attrValeMapper.selectByExample(example);
        return attrValues;
    }

    /**
     * 分页
     * @param catalog3Id
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<BaseAttrInfo> getAttrListByCtg3Page(String catalog3Id, String pageNum, String pageSize) {

        PageHelper.startPage(Integer.parseInt(pageNum) ,Integer.parseInt(pageSize) );
        Example example = new Example(BaseAttrInfo.class);
        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
        List<BaseAttrInfo> baseAttrInfos = attrInfoMapper.selectByExample(example);
        PageInfo<BaseAttrInfo> baseAttrInfoPageInfo = new PageInfo<>(baseAttrInfos);
        return baseAttrInfoPageInfo;
    }

    @Override
    public void deleteAttrInfo(String attrId) {
        attrInfoMapper.deleteByPrimaryKey(attrId);
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",attrId);
        attrValeMapper.deleteByExample(example);


    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueId(String join) {
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoMapper.getAttrListByValueId(join);
        return baseAttrInfos;
    }

    /**
     * 抽取的公共的attrValue插入到数据库的方法
     * @param baseAttrInfo
     * @param attid
     */
    public void insertAttrValues(BaseAttrInfo baseAttrInfo,String attid){
        //在bean里面已经增加了value值的信息
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue attrValue : attrValueList) {
            //循环遍历获取的每个attrValue对象还没有id,需要给他设置一个id,
            attrValue.setAttrId(attid);
            //往baseattrValue插入值
            attrValeMapper.insertSelective(attrValue);
        }
    }
}
