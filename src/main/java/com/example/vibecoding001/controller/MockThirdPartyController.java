package com.example.vibecoding001.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 模拟第三方校验接口
 * 用于测试外部API配置
 */
@RestController
@RequestMapping("/mock-api")
public class MockThirdPartyController {

    /**
     * 模拟残疾人身份验证接口
     * @param idCard 身份证号
     * @param name 姓名
     * @return 验证结果
     */
    @PostMapping("/disability/verify")
    public Map<String, Object> verifyDisability(@RequestBody Map<String, String> request) {
        String idCard = request.get("idCard");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        // 模拟逻辑：特定身份证号段返回残疾人
        if (idCard != null && (idCard.startsWith("110101") || idCard.startsWith("999"))) {
            data.put("isDisabled", true);
            data.put("disabilityType", "肢体残疾");
            data.put("disabilityLevel", "二级");
        } else {
            data.put("isDisabled", false);
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 模拟信用评分接口
     * @param idCard 身份证号
     * @param name 姓名
     * @return 信用评分结果
     */
    @PostMapping("/credit/score")
    public Map<String, Object> getCreditScore(@RequestBody Map<String, String> request) {
        String idCard = request.get("idCard");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        // 模拟逻辑：根据身份证号生成信用评分
        if (idCard != null) {
            int lastDigits = Integer.parseInt(idCard.substring(idCard.length() - 4));
            int score = 500 + (lastDigits % 350); // 500-850
            
            data.put("creditScore", score);
            data.put("creditLevel", score >= 700 ? "优秀" : score >= 600 ? "良好" : "一般");
            data.put("riskLevel", score >= 700 ? "低" : score >= 600 ? "中" : "高");
        } else {
            data.put("creditScore", 0);
            data.put("creditLevel", "未知");
            data.put("riskLevel", "高");
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 模拟黑名单检查接口
     * @param idCard 身份证号
     * @param name 姓名
     * @return 黑名单检查结果
     */
    @PostMapping("/blacklist/check")
    public Map<String, Object> checkBlacklist(@RequestBody Map<String, String> request) {
        String idCard = request.get("idCard");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        // 模拟逻辑：特定身份证号在黑名单中
        Set<String> blackList = Set.of(
            "110101199001011234",
            "310101198501015678",
            "440101199003026789"
        );
        
        boolean isBlacklisted = blackList.contains(idCard);
        
        data.put("isBlacklisted", isBlacklisted);
        data.put("riskReason", isBlacklisted ? "涉嫌欺诈" : null);
        data.put("blacklistEntryDate", isBlacklisted ? "2024-01-15" : null);
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 模拟年龄验证接口
     * @param idCard 身份证号
     * @param name 姓名
     * @return 年龄验证结果
     */
    @PostMapping("/age/verify")
    public Map<String, Object> verifyAge(@RequestBody Map<String, String> request) {
        String idCard = request.get("idCard");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        if (idCard != null && idCard.length() >= 14) {
            try {
                String birthYearStr = idCard.substring(6, 10);
                String birthMonthStr = idCard.substring(10, 12);
                String birthDayStr = idCard.substring(12, 14);
                
                int birthYear = Integer.parseInt(birthYearStr);
                int age = 2026 - birthYear;
                
                data.put("birthDate", birthYearStr + "-" + birthMonthStr + "-" + birthDayStr);
                data.put("age", age);
                data.put("isAdult", age >= 18);
                data.put("isSenior", age >= 60);
            } catch (Exception e) {
                data.put("error", "身份证号格式错误");
            }
        } else {
            data.put("error", "身份证号长度不足");
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 模拟手机号验证接口
     * @param phone 手机号
     * @return 手机号验证结果
     */
    @PostMapping("/phone/verify")
    public Map<String, Object> verifyPhone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        if (phone != null && phone.length() == 11) {
            // 模拟逻辑：根据手机号段判断运营商
            String prefix = phone.substring(0, 3);
            String operator;
            String operatorType;
            
            if (Set.of("134", "135", "136", "137", "138", "139", "147", "150", "151", "152", "157", "158", "159", "172", "178", "182", "183", "184", "187", "188", "198").contains(prefix)) {
                operator = "中国移动";
                operatorType = "mobile";
            } else if (Set.of("130", "131", "132", "145", "155", "156", "166", "171", "175", "176", "185", "186").contains(prefix)) {
                operator = "中国联通";
                operatorType = "unicom";
            } else if (Set.of("133", "153", "173", "177", "180", "181", "189", "191", "193", "199").contains(prefix)) {
                operator = "中国电信";
                operatorType = "telecom";
            } else {
                operator = "未知";
                operatorType = "unknown";
            }
            
            data.put("phone", phone);
            data.put("operator", operator);
            data.put("operatorType", operatorType);
            data.put("isValid", true);
        } else {
            data.put("phone", phone);
            data.put("isValid", false);
            data.put("error", "手机号格式错误");
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 模拟银行四要素验证接口
     * @param name 姓名
     * @param idCard 身份证号
     * @param bankCard 银行卡号
     * @param phone 手机号
     * @return 验证结果
     */
    @PostMapping("/bank/verify")
    public Map<String, Object> verifyBankCard(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String idCard = request.get("idCard");
        String bankCard = request.get("bankCard");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        // 模拟逻辑：特定银行卡号段返回匹配
        boolean isMatch = bankCard != null && (bankCard.startsWith("6222") || bankCard.startsWith("6228") || bankCard.startsWith("9558"));
        
        data.put("nameMatch", isMatch);
        data.put("idCardMatch", isMatch);
        data.put("bankCardValid", true);
        data.put("isVerified", isMatch);
        
        if (isMatch) {
            data.put("bankName", "中国工商银行");
            data.put("cardType", "借记卡");
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", UUID.randomUUID().toString());
        
        return response;
    }

    /**
     * 获取可用的模拟接口列表
     * @return 接口列表
     */
    @GetMapping("/list")
    public Map<String, Object> listApis() {
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, String>> apis = new ArrayList<>();
        
        apis.add(Map.of(
            "name", "残疾人身份验证",
            "url", "/mock-api/disability/verify",
            "description", "验证身份证号是否为残疾人",
            "exampleRequest", "{\"idCard\": \"110101199001011234\", \"name\": \"张三\"}",
            "successCondition", "response.isDisabled == true"
        ));
        
        apis.add(Map.of(
            "name", "信用评分查询",
            "url", "/mock-api/credit/score",
            "description", "根据身份证号获取信用评分",
            "exampleRequest", "{\"idCard\": \"110101199001011234\", \"name\": \"张三\"}",
            "successCondition", "response.creditScore >= 700"
        ));
        
        apis.add(Map.of(
            "name", "黑名单检查",
            "url", "/mock-api/blacklist/check",
            "description", "检查身份证号是否在黑名单中",
            "exampleRequest", "{\"idCard\": \"110101199001011234\", \"name\": \"张三\"}",
            "successCondition", "response.isBlacklisted == false"
        ));
        
        apis.add(Map.of(
            "name", "年龄验证",
            "url", "/mock-api/age/verify",
            "description", "根据身份证号验证年龄",
            "exampleRequest", "{\"idCard\": \"110101199001011234\", \"name\": \"张三\"}",
            "successCondition", "response.isAdult == true"
        ));
        
        apis.add(Map.of(
            "name", "手机号验证",
            "url", "/mock-api/phone/verify",
            "description", "验证手机号格式和运营商",
            "exampleRequest", "{\"phone\": \"13800138000\"}",
            "successCondition", "response.isValid == true"
        ));
        
        apis.add(Map.of(
            "name", "银行四要素验证",
            "url", "/mock-api/bank/verify",
            "description", "验证姓名、身份证、银行卡、手机号是否匹配",
            "exampleRequest", "{\"name\": \"张三\", \"idCard\": \"110101199001011234\", \"bankCard\": \"6222021234567890\", \"phone\": \"13800138000\"}",
            "successCondition", "response.isVerified == true"
        ));
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", apis);
        
        return response;
    }
}
