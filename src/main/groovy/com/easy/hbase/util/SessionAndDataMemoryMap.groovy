package com.easy.hbase.util

import com.alibaba.fastjson.JSONObject


import java.util.concurrent.ConcurrentHashMap

class SessionAndDataMemoryMap {
    private static Map<String,HashMap<String,List<JSONObject>>> map =
            new ConcurrentHashMap<String, HashMap<String,List<JSONObject>>>()
    static void add(String session, HashMap<String,List<JSONObject>> results){
        map.put(session,results)
    }
    static HashMap<String,List<JSONObject>> get(String session){
        return map.get(session)
    }
    static void remove(String session){
        map.remove(session)
    }
}
