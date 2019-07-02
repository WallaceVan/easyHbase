package com.easy.hbase.util

import org.apache.hadoop.fs.shell.CopyCommands
import org.apache.hadoop.hbase.Cell
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.HTableInterface
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.ColumnPaginationFilter
import org.apache.hadoop.hbase.filter.ColumnRangeFilter
import org.apache.hadoop.hbase.filter.CompareFilter
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.filter.RowFilter
import org.apache.hadoop.hbase.util.Bytes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.hadoop.hbase.HbaseTemplate
import org.springframework.data.hadoop.hbase.RowMapper
import org.springframework.data.hadoop.hbase.TableCallback
import org.springframework.stereotype.Service
import com.alibaba.fastjson.JSONObject

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class HbaseSearchService {
    private @Autowired
    HbaseTemplate template

    Map<String, Object> getRowResult(String tableName,
                                     String reg,
                                     String sessionId,
                                     String size) throws Exception {
        def mapResult = new HashMap<String, Object>()
        def fl = new FilterList(FilterList.Operator.MUST_PASS_ALL)
        def scanCount = new Scan()
        def rc = new RegexStringComparator(reg)
        def rf = new RowFilter(CompareFilter.CompareOp.EQUAL, rc)
        def cf = new ColumnPaginationFilter(1,0)
        fl.addFilter(rf)
        fl.addFilter(cf)


        scanCount.setFilter(fl)
        scanCount.addFamily("data".getBytes())
        //scanCount.setMaxResultSize(1)
        List<JSONObject> results = template.find(
                tableName,
                scanCount,
                new RowMapper<String>() {
                    @Override
                    String mapRow(Result result, int rowNum) throws Exception {
                        JSONObject data = JSONObject.parseObject(Bytes.toString(result.value()))
                        data.put("rowkey",Bytes.toString(result.getRow()))
                        return data


                    }
                }
        )
        def rowMap = new HashMap<String,List<JSONObject>>()
        rowMap.put("rowResults",results)
        SessionAndDataMemoryMap.add(sessionId,rowMap)

        mapResult.put("results", Pager(Integer.parseInt(size),1,results))
        mapResult.put("size",results.size())
        return mapResult
    }


    Map<String, Object> getResultSizeChange(String sessionId,
                                            String size) throws Exception {
        def mapResult = new HashMap<String, Object>()
        def rowMap = SessionAndDataMemoryMap.get(sessionId)
        def results = rowMap.get("rowResults")
        mapResult.put("results", Pager(Integer.parseInt(size),1,results))
        mapResult.put("size",results.size())
        return mapResult
    }


    Map<String, Object> getResultCurrentChange(String sessionId,
                                               String currentPage,
                                               String size) throws Exception {

        def mapResult = new HashMap<String, Object>()
        def rowMap = SessionAndDataMemoryMap.get(sessionId)
        def results = rowMap.get("rowResults")
        mapResult.put("results", Pager(Integer.parseInt(size),
                Integer.parseInt(currentPage),results))

        mapResult.put("size",results.size())
        return mapResult
    }
    Map<String, Object> getRowDetail(String sessionId,
                                     String tableName,
                                     String rowKey,
                                     String size,
                                     String currentPage,
                                     String startDate,
                                     String endDate) throws Exception{
        def ftf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        def mapResult = new HashMap<String, Object>()
        def rowMap = SessionAndDataMemoryMap.get(sessionId)
        def rowResults = rowMap.get("rowResults")
        def resultsAll = rowMap.get("columnResults")
        List<JSONObject> results = null
        def flag = 0
        def startTime = startDate.replace("@"," ")
                .replace("-","/")
        def endTime = endDate.replace("@"," ")
                .replace("-","/")
        if (resultsAll != null){
            results = resultsAll.get(startTime + "-" + endTime)
            if(results == null){
                rowMap.remove("columnResults")
                flag = 1
            }else{
                def rowkey_session = results.get(0).get("rowkey")
                if(rowkey_session != rowKey) {
                    rowMap.remove("columnResults")
                    flag = 1
                }
            }
        }else{
            flag = 1
        }
        //需要重新加载数据到内存

        if (flag != 0){

            LocalDateTime start_parse = LocalDateTime.parse(startTime, ftf)
            LocalDateTime end_parse = LocalDateTime.parse(endTime, ftf)
            long start_timestamp = LocalDateTime.from(start_parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()/1000
            long end_timestamp = LocalDateTime.from(end_parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()/1000
            def rangeFilter =new ColumnRangeFilter(
                    Bytes.toBytes(start_timestamp.toString()),
                    false,
                    Bytes.toBytes(end_timestamp.toString()),
                    false)
            def get = new Get(Bytes.toBytes(rowKey))
            //get.setFilter(rangeFilter)
            def scan = new Scan(get)
            scan.setFilter(rangeFilter)

            def resultsTemplate = template.find(
                tableName,
                scan,
                new RowMapper<List<JSONObject>>() {
                    @Override
                    List<JSONObject> mapRow(Result result, int rowNum) throws Exception {
                        def list = new ArrayList<JSONObject>()
                        List<Cell> temps = result.listCells()
                        for(int i=0;i<temps.size();i++){
                            JSONObject data = JSONObject.parseObject(Bytes.toString(CellUtil.cloneValue(temps.get(i))))
                            data.put("rowkey",Bytes.toString(result.getRow()))
                            list.add(data)
                        }

                        return list
                    }
                }
            )

            if (resultsTemplate.size() != 0){
                results = resultsTemplate.get(0)
                def tempResults = new HashMap<String,List<JSONObject>>()
                tempResults.put("rowResults",rowResults)
                def map = new HashMap<String,List<JSONObject>>()
                map.put(startTime + "-" + endTime,results)
                tempResults.put("columnResults",map)
                SessionAndDataMemoryMap.add(sessionId,tempResults)
                def fixResults = Pager(Integer.parseInt(size),
                        Integer.parseInt(currentPage),
                        results)
                mapResult.put("results", fixResults)
                mapResult.put("size",results.size())
                return mapResult
            }else{
                mapResult.put("results", new ArrayList<JSONObject>())
                mapResult.put("size",0)
                return mapResult
            }

        }



    }
    Map<String, Object> getRowDetailSizeChange(String sessionId,
                                               String startDate,
                                               String endDate,
                                               String size) throws Exception {
        def startTime = startDate.replace("@"," ")
                .replace("-","/")
        def endTime = endDate.replace("@"," ")
                .replace("-","/")
        def mapResult = new HashMap<String, Object>()
        def rowMap = SessionAndDataMemoryMap.get(sessionId)

        List<JSONObject> results = rowMap.get("columnResults").get(startTime + "-" + endTime)
        mapResult.put("results", Pager(Integer.parseInt(size),1,results))

        mapResult.put("size",results.size())
        return mapResult
    }
    Map<String, Object> getRowDetailCurrentChange(String sessionId,
                                                  String startDate,
                                                  String endDate,
                                                  String currentPage,
                                                  String size) throws Exception {
        def startTime = startDate.replace("@"," ")
                .replace("-","/")
        def endTime = endDate.replace("@"," ")
                .replace("-","/")
        def mapResult = new HashMap<String, Object>()
        def rowMap = SessionAndDataMemoryMap.get(sessionId)
        List<JSONObject> results = rowMap.get("columnResults").get(startTime + "-" + endTime)

        mapResult.put("results", Pager(Integer.parseInt(size), Integer.parseInt(currentPage),results))

        mapResult.put("size",results.size())
        return mapResult
    }
    private static  List<JSONObject>  Pager(int pageSize,int pageIndex,List<JSONObject> list){
        List<JSONObject> dataList
        int currentPage
        int totalRecord = list.size()
        int totalPage = totalRecord % pageSize
        if (totalPage > 0) {
            totalPage = totalRecord / pageSize + 1
        } else {
            totalPage = totalRecord / pageSize
        }
        currentPage = totalPage < pageIndex ? totalPage : pageIndex
        int fromIndex = pageSize * (currentPage - 1)
        int toIndex = pageSize * currentPage > totalRecord ?
                totalRecord : pageSize * currentPage
        dataList = list.subList(fromIndex, toIndex)
        return dataList
    }

}

