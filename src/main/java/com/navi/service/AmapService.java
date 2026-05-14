package com.navi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navi.config.AmapConfig;
import com.navi.model.ShopInfo;
import com.navi.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapService {

    private final AmapConfig amapConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * POI关键词搜索，返回第一个匹配结果
     */
    public ShopInfo textSearch(String keywords, String city) {
        try {
            String url = String.format(
                    "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s&citylimit=true&offset=5",
                    amapConfig.getApiKey(),
                    encode(keywords),
                    encode(city)
            );
            String body = HttpClientUtil.get(url);
            JsonNode root = objectMapper.readTree(body);

            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap textSearch failed for [{}] in [{}]: {}", keywords, city, root.path("info").asText());
                return null;
            }

            JsonNode pois = root.path("pois");
            if (pois.isEmpty()) {
                log.warn("No POI found for [{}] in [{}]", keywords, city);
                return null;
            }

            // 取第一个结果
            JsonNode poi = pois.get(0);
            String location = poi.path("location").asText();
            String[] parts = location.split(",");
            if (parts.length != 2) return null;

            return ShopInfo.builder()
                    .name(poi.path("name").asText())
                    .address(poi.path("address").asText())
                    .lng(Double.parseDouble(parts[0]))
                    .lat(Double.parseDouble(parts[1]))
                    .amapPoiId(poi.path("id").asText())
                    .build();
        } catch (Exception e) {
            log.error("textSearch error for [{}]: {}", keywords, e.getMessage());
            return null;
        }
    }

    /**
     * 地理编码：地址转坐标
     */
    public double[] geocode(String address, String city) {
        try {
            String url = String.format(
                    "https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s&city=%s",
                    amapConfig.getApiKey(),
                    encode(address),
                    encode(city)
            );
            String body = HttpClientUtil.get(url);
            JsonNode root = objectMapper.readTree(body);

            if (!"1".equals(root.path("status").asText())) return null;

            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isEmpty()) return null;

            String location = geocodes.get(0).path("location").asText();
            String[] parts = location.split(",");
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        } catch (Exception e) {
            log.error("geocode error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量计算驾车距离：多个起点到一个终点
     */
    public List<Long> batchDistance(String originsPipeSeparated, String destination) {
        try {
            String url = String.format(
                    "https://restapi.amap.com/v3/distance?key=%s&origins=%s&destination=%s&type=1",
                    amapConfig.getApiKey(),
                    encode(originsPipeSeparated),
                    encode(destination)
            );
            String body = HttpClientUtil.get(url);
            JsonNode root = objectMapper.readTree(body);

            if (!"1".equals(root.path("status").asText())) return List.of();

            List<Long> distances = new ArrayList<>();
            for (JsonNode r : root.path("results")) {
                distances.add(r.path("distance").asLong(-1));
            }
            return distances;
        } catch (Exception e) {
            log.error("batchDistance error: {}", e.getMessage());
            return List.of();
        }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
