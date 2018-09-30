package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.service.BaseAttrInfoService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-04 18:02
 */
@Controller
public class AttrController {

    @Reference
    BaseAttrInfoService baseAttrInfoService;

    /**
     * 通过三级分类获取某个一属性的所有属性值
     *
     * @param map
     * @return
     */
    @RequestMapping("/getAttrValuesBycatalog3Id")
    @ResponseBody
    public List<BaseAttrValue> getAttrValuesBycatalog3Id(@RequestParam Map<String, String> map) {
        String attrId = map.get("attrId");
        List<BaseAttrValue> attrValues = baseAttrInfoService.getAttrValuesByCata3Id(attrId);
        return attrValues;
    }


    /**
     * 目前不用
     * @param map
     * @return
     */
    @RequestMapping("/getAttrListByCtg3")
    @ResponseBody
    public List<BaseAttrInfo> getAttrInfo(@RequestParam Map<String, String> map) {
        String catalog3Id = map.get("catalog3Id");
        List<BaseAttrInfo> lists = baseAttrInfoService.getAttrListsByCata3Id(catalog3Id);
        return lists;
    }

    /**
     * 分页
     * @param map
     * @return
     */
    @RequestMapping("/getAttrListByCtg3Page")
    @ResponseBody
    public Map<String, Object> getAttrListByCtg3Page(@RequestParam Map<String, String> map,
                                                     @RequestParam(value = "page",defaultValue = "1")String page,
                                                     @RequestParam(value = "rows",defaultValue = "5")String rows) {
        String catalog3Id = map.get("catalog3Id");


        /*Integer pageNum = 0;
        Integer pageSize = 5;*/
        PageInfo<BaseAttrInfo> lists = baseAttrInfoService.getAttrListByCtg3Page(catalog3Id,page,rows);
        List<BaseAttrInfo> list = lists.getList();

        long total = lists.getTotal();
        Map<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("total",total);
        stringObjectHashMap.put("rows",list);
        return stringObjectHashMap;
    }

    /**
     * 对话框添加保存
     * @param baseAttrInfo
     * @return
     */
    @RequestMapping("/saveAttr")
    @ResponseBody
    public String saveAttr(BaseAttrInfo baseAttrInfo) {
        baseAttrInfoService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

    @RequestMapping("/deleteAttr")
    @ResponseBody
    public String deleteAttr(@RequestParam Map<String,String> map){
        String attrId = map.get("attrId");

        baseAttrInfoService.deleteAttrInfo(attrId);
        return "success";
    }

}
