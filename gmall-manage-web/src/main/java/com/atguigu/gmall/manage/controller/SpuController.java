package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.manage.utils.FileUploadUtil;
import com.atguigu.gmall.service.SpuService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-05 19:02
 */
@Controller
public class SpuController {

    @Reference
    SpuService spuService;

    @RequestMapping("/spuListByCatalog3IdPage")
    @ResponseBody
    public Map<String, Object> spuListPage(@RequestParam("catalog3Id")String catalog3Id,
                                     @RequestParam(value = "page",defaultValue = "1")String page,
                                     @RequestParam(value = "rows",defaultValue = "5")String rows){

        PageInfo<SpuInfo> spuInfoPageInfo = spuService.getspuInfosByCatalog3Id(catalog3Id, page, rows);
        List<SpuInfo> list = spuInfoPageInfo.getList();

        long total = spuInfoPageInfo.getTotal();
        Map<String,Object> map =  new HashMap<>();
        map.put("total",total);
        map.put("rows",list);
        return map;
    }
    /**
     * 通过id删除spuInfo
     * @param id
     * @return
     */
    @RequestMapping("/deletSupInfo")
    @ResponseBody
    public String deletSupInfo(@RequestParam("id")String id){
        spuService.deleteSpuInfoById(id);
        return "删除成功";
    }

    /**
     * 通过supId获取销售属性列表
     * @param spuId
     * @return
     */
    @RequestMapping("/spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        List<SpuSaleAttr> lists = spuService.getSpuSaleAttrList(spuId);
        return lists;
    }

    /**
     * 通过spuId获取spu的图片列表
     * @param spuId
     * @return
     */
    @RequestMapping("/spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){
        List<SpuImage> lists =  spuService.getspuImageList(spuId);
        return lists;
    }

    /**
     * 保存spuInfo信息
     * @param spuInfo
     * @return
     */
    @RequestMapping("/saveSpu")
    @ResponseBody
    public String saveSpu(SpuInfo spuInfo){
        spuService.saveSpu(spuInfo);
        return null;
    }

    /**
     * 文件上传
     * @param file
     * @return
     */
    @RequestMapping("/fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file")MultipartFile file){
        String imgUrl = FileUploadUtil.uploadFile(file);
        return imgUrl;
    }
    /**
     * 查询spu_info表的信息
     * @param catalog3Id
     * @return
     */
    @RequestMapping("/spuList")
    @ResponseBody
    public List<SpuInfo> getSpuByCtg3(@RequestParam("catalog3Id")String catalog3Id){
        List<SpuInfo> spuInfoList = spuService.getSpuByCtg3(catalog3Id);
        return spuInfoList;
    }

    /**
     * 查询base_sale_attr表的信息
     * @return
     */
    @RequestMapping("/baseSaleAttr")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttr(){
        List<BaseSaleAttr> baseSaleAttr = spuService.getbaseSaleAttr();
        return baseSaleAttr;
    }
}
