package com.ruoyi.web.controller.basic.yinjiangbuhan.utils;


import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TuhuguancheUtil {

    private static final String openapi_url = "http://open.tuhugc.com";
    private static final String app_key = "5508166a129cf8e1bfe2943b14e6d1dc";
    private static final String app_secret = "b05cec7978367a830ab693c0a504e103";
    

    private String getToken() {
        RedisTemplate redisTemplate = RedisUtil.redis;
        Object carToken = redisTemplate.opsForValue().get("carToken");
        if(carToken!=null){
            return carToken.toString();
        }
        Map<String, String> paramMap = getCommonParam();
        // 私有参数_获取token
        paramMap.put("userId", "13521470746");
        paramMap.put("expiresIn", "7200");

        String sign = "";
        try {
            sign = SignUtils.signTopRequest(paramMap, app_secret, "md5");
            paramMap.put("sign", sign);
            System.out.println("签名："+sign);
        } catch (IOException e) {
            return null;
        }
        Map<String, Object> objectMap = paramMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
        String body = HttpUtil.createPost(openapi_url+"/v1/token/get")
                .form(objectMap).execute().body();
        Map tokenResult = JSON.parseObject(body, Map.class);
        Object code = tokenResult.get("code");
        if("0".equals(code)){
            return null;
        }
        JSONObject result = (JSONObject)tokenResult.get("result");
        Object accessToken = result.get("accessToken");
        redisTemplate.opsForValue().set("carToken",accessToken,7200, TimeUnit.SECONDS);
        return accessToken.toString();
    }


    private static Map<String, String> getCommonParam() {
        Map<String, String> paramMap = new HashMap<>();
        // 公共参数
        paramMap.put("appKey", app_key);
        paramMap.put("v", "1.0");
        paramMap.put("timestamp", DateUtil.now());
        paramMap.put("signMethod", "md5");
        paramMap.put("format", "json");
        return paramMap;
    }

    public static void main(String[] args) {
        List<String> cities = Arrays.asList("Milan",
                "London",
                "New York",
                "San Francisco");
        String citiesCommaSeparated = "'"+String.join("','", cities)+"'";
        System.out.println("'"+citiesCommaSeparated+"'");
    }

    public static Map getDeviceLocation(){
        String token = new TuhuguancheUtil().getToken();

        Map<String, String> paramMap2 = getCommonParam();
        paramMap2.put("userId", "13521470746");
        JSONObject jsonObject2 = getParam(token, paramMap2, "/v1/vehicle/list");
        JSONArray result = jsonObject2.getJSONArray("result");

        Map<String, String> paramMap = getCommonParam();
        paramMap.put("mapType","WGS84");
        paramMap.put("userId", "13521470746");
        JSONObject jsonObject = getParam(token, paramMap, "/v1/device/location/list");
        if(0==(Integer)jsonObject.get("code")){
            JSONArray array = jsonObject.getJSONArray("result");
            for (Object obj : array) {
                JSONObject item = (JSONObject) obj;
                for (Object o : result) {
                    JSONObject item2 = (JSONObject) o;
                    System.out.println(item.get("imei")+"..."+item2.get("imei"));
                    if(item2.get("imei").equals(item.get("imei"))){
                        item.put("licensePlate",item2.get("licensePlate"));
                        item.put("vehicleDriver",item2.get("vehicleDriver"));
                        item.put("vehicleType",item2.get("vehicleType"));
                        break;
                    }
                }
            }
        }
        return jsonObject.toJavaObject(Map.class);
    }

    private static JSONObject getParam (String token, Map<String, String>paramMap,String url){
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        // 通过接口获取的accessToken
        headerMap.put("X-Access-Token", token);
//        Map<String, String> paramMap = getCommonParam();
//        paramMap.put("mapType","BAIDU");
//        paramMap.put("userId", "13521470746");
        String sign = "";
        try {
            sign = SignUtils.signTopRequest(paramMap, app_secret, "md5");
            paramMap.put("sign", sign);
        } catch (IOException e) {
            System.err.println(e);
        }
        Map<String, Object> objectMap = paramMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
        url = openapi_url+url;
        String body = HttpUtil.createPost(url)
                .headerMap(headerMap,true)
                .form(objectMap).execute().body();
        JSONObject jsonObject = JSONObject.parseObject(body);
        return jsonObject;
    }


}
