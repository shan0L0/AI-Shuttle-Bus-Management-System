package com.smartshuttle.business;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/amap")
public class AmapSignController {

    private static final String SECURITY_JS_CODE = "b676058b87470697ece04821f0b6aec7";

    @GetMapping("/signature")
    public Map<String, Object> sign(@RequestParam("sig") String sig) throws Exception {
        System.out.println("sig:" + sig);
        String signature = hmacSha256(SECURITY_JS_CODE, sig);

        return Map.of(
                "status", "success",
                "data", signature
        );
    }

    private String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        mac.init(secretKey);

        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(raw);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}