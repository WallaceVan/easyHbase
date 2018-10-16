package com.easy.hbase.controller

import com.alibaba.fastjson.JSONArray
import com.easy.hbase.util.HbaseSearchService
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.hadoop.hbase.HbaseTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpSession


@RestController
class SearchController {
    @Autowired
    HbaseTemplate hbaseTemplate
    @Autowired
    HbaseSearchService service
    @Autowired
    private HttpSession session
    @RequestMapping("/hbase/searchTables")
    JSONArray searchTables(){

        Connection conn = ConnectionFactory.createConnection(hbaseTemplate.getConfiguration())
        def admin = conn.getAdmin()
        def arr = new JSONArray()
        for (i in 0..admin.listTableNames().length - 1){
            arr.add(admin.listTableNames()[i].getNameAsString())
        }
        return arr

    }

    @RequestMapping("/hbase/searchData/{tablename}/{regular}/{size}")
    Map<String, Object> searchData(@PathVariable("tablename") String tablename,
                                   @PathVariable("regular") String regular,
                                   @PathVariable("size") String size){
        return service.getRowResult(tablename,regular,session.id,size)
    }

    @RequestMapping("/hbase/searchData/{size}")
    Map<String, Object> handleSizeChange(@PathVariable("size") String size){
        return service.getResultSizeChange(session.id,size)
    }

    @RequestMapping("/hbase/searchData/{currentPage}/{size}")
    Map<String, Object> handleCurrentChange(@PathVariable("currentPage") String currentPage,
                                            @PathVariable("size") String size){
        return service.getResultCurrentChange(session.id,currentPage,size)
    }

    @RequestMapping("/hbase/searchData/{tablename}/{rowkey}/{size}/{currentPage}/{startDate}/{endDate}")
    Map<String, Object> getRowDetail(@PathVariable("tablename") String tablename,
                                            @PathVariable("rowkey") String rowkey,
                                            @PathVariable("size") String size,
                                            @PathVariable("currentPage") String currentPage,
                                            @PathVariable("startDate") String startDate,
                                            @PathVariable("endDate") String endDate){
        return service.getRowDetail(session.id,tablename,rowkey,size,currentPage,startDate,endDate)
    }

    @RequestMapping("/hbase/searchDetailSize/{size}/{startDate}/{endDate}")
    Map<String, Object> getRowDetailSize(@PathVariable("size") String size,
                                     @PathVariable("startDate") String startDate,
                                     @PathVariable("endDate") String endDate){
        return service.getRowDetailSizeChange(session.id,startDate,endDate,size,startDate,endDate)
    }

    @RequestMapping("/hbase/searchDetailSize/{size}/{startDate}/{endDate}/{currentPage}")
    Map<String, Object> getRowDetailSize(@PathVariable("size") String size,
                                         @PathVariable("startDate") String startDate,
                                         @PathVariable("endDate") String endDate,
                                         @PathVariable("currentPage") String currentPage){
        return service.getRowDetailCurrentChange(session.id,startDate,endDate,currentPage,size)
    }
}
