package com.atguigu.gmall.list.service.imp;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2018-09-12 16:25
 */
@Service
public class ListServiceImpl implements ListService {
    public static final String indexNameGmall = "gmall0508";
    public static final String typeNameGmall = "SkuLsInfo";

    @Autowired
    JestClient jestClient;



    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {
        //创建这个对象时用来接收所有查询出来的SkuInfo
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();
        //getMyDsl()为kibana表达式，Index即库，type即表
        Search search = new Search.Builder(getMyDsl(skuLsParam)).addIndex(indexNameGmall).addType(typeNameGmall).build();
        SearchResult excute = null;
        try {
            //执行查询语句
            excute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(excute != null){
            //根据elastci的结构，数据藏在hits（击中）的source集合里面
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = excute.getHits(SkuLsInfo.class);
            if(hits != null&&hits.size() > 0){
                for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                    //遍历hit，添加到SkuInfo的集合中
                    SkuLsInfo source = hit.source;
                    Map<String, List<String>> highlight = hit.highlight;
                    if(highlight != null){
                        //获取skuName属性
                        List<String> skuName = highlight.get("skuName");

                        if(StringUtils.isNotBlank(skuName.get(0))){
                            //设置skuName的属性为高亮
                            source.setSkuName( skuName.get(0));
                        }

                    }
                    skuLsInfos.add(source);
                }

            }

        }

        return skuLsInfos;
    }




    /**
     * 该方法是返回一个json的elastic的查询语句
     * SkuLsParam是skuLsList的相关参数（关键字啊，匹配的字啊）
     * @param skuLsParam
     * @return
     */
    public String getMyDsl(SkuLsParam skuLsParam){

        String keyword = skuLsParam.getKeyword();
        String catalog3Id = skuLsParam.getCatalog3Id();
        String[] valueId = skuLsParam.getValueId();
        //创建一个查询语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //过滤查询需要用到bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //过滤
        if(StringUtils.isNotBlank(catalog3Id)){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            //过滤条件外层是bool,内层是term
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //加载分类属性
        if(valueId != null&&valueId.length > 0){
            for (int i = 0; i < valueId.length; i++) {
                TermQueryBuilder termQueryBuilderVaueId = new TermQueryBuilder("skuAttrValueList.valueId", valueId[i]);
                boolQueryBuilder.filter(termQueryBuilderVaueId);
            }
        }
        //搜索
        if(StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            //匹配条件的外层是must,内层是match
            boolQueryBuilder.must(matchQueryBuilder);
        }
        //将整个参数放到query中
        searchSourceBuilder.query(boolQueryBuilder);
        //分页属性，从from开始，显示size个
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
        //设置添加高亮属性
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //默认的效果是斜体，设置他为红色显色
        highlightBuilder.preTags("<span style='color:red;font-weight:bolder'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");

        searchSourceBuilder.highlight(highlightBuilder);
        System.out.println(searchSourceBuilder.toString());
        return searchSourceBuilder.toString();
    }
}
