package com.navi.controller;

import com.navi.config.AmapConfig;
import com.navi.model.NaviRequest;
import com.navi.model.NaviResult;
import com.navi.service.NaviService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NaviController {

    private final NaviService naviService;
    private final AmapConfig amapConfig;

    @PostMapping("/api/navi/parse")
    public NaviResult parse(@RequestBody NaviRequest request) {
        return naviService.process(request);
    }

    @GetMapping("/api/config/amap-js-key")
    public Map<String, String> getAmapJsKey() {
        return Map.of("jsApiKey", amapConfig.getJsApiKey());
    }
}
