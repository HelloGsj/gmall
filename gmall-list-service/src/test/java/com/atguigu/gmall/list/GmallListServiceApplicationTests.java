package com.atguigu.gmall.list;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    private static final String indexName = "gmall0508";
    private static final String typeName = "SkuLsInfo";
    //需要通过skuService将数据保存到elastcSearch中
    @Reference
    SkuService skuService;

    //通过jestClient工具类操作elasticSearch语句
    @Autowired
    JestClient jestClient;
    @Test
    public void contextLoads() {
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        System.err.println(getMyDsl());
        //创建一个serach（如：GET gmall0508/SkuInfo/_search ）
        //getMyDsl()返回的是一个elasticSerach语句
        Search search = new Search.Builder(getMyDsl()).addIndex(indexName).addType(typeName).build();

        try {
            //利用工具jestClient执行语句
            SearchResult searchResult = jestClient.execute(search);
            //先获取hits
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
            //遍历hit，因为对象保存在hit里面的source里面
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                skuLsInfoList.add(skuLsInfo);
            }
            //打印添加到集合里面的个数
            System.out.println(skuLsInfoList.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //生成elasticSearch的查询语句
    public String getMyDsl(){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //要过滤必须创建一个boolquery
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id","1");
        //filter是boolquery里面的一个属性
        //termQuery是filter里面的一个属性
        boolQueryBuilder.filter(termQueryBuilder);
        //must是boolquery里面的一个属性
        //match是must里面的一个属性
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "小米");
        //must是搜索
        boolQueryBuilder.must(matchQueryBuilder);
        //创建一个查询结构：searchSource
        searchSourceBuilder.query(boolQueryBuilder);
        //分页属性，从0到100
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);

        return searchSourceBuilder.toString();
    }
    /**
     * 保存
     */
    @Test
    public void addData(){
        //查询出skuInfo的信息
        List<SkuInfo> skuInfoList = skuService.getSkuInfoByCatalog3Id(1);

        //将skuInfo转化成skuLsInfo
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();
        for (SkuInfo skuInfo : skuInfoList) {
            SkuLsInfo skuLsInfo = new SkuLsInfo();
            //将skkuInfo所有属性复制到skuLsInfo
            BeanUtils.copyProperties(skuInfo,skuLsInfo);
            //再将skuLsInfo添加到集合中
            skuLsInfos.add(skuLsInfo);
        }


        //将集合skuLsInfos插入到elasticSearch中
        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            Index index = new Index.Builder(skuLsInfo).index(indexName).type(typeName).build();
            try {
                jestClient.execute(index);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("text=================" + skuLsInfos.size());
        
    }

}
