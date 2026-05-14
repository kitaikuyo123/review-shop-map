package com.navi.service;

import com.navi.model.NaviRequest;
import com.navi.model.NaviResult;
import com.navi.model.ShopInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaviService {

    private final ReviewParseService reviewParseService;
    private final AmapService amapService;

    public NaviResult process(NaviRequest request) {
        // 1. LLM 解析评论文本
        List<ReviewParseService.ParsedShop> parsedShops = reviewParseService.parse(request.getContent());
        log.info("Parsed {} shops from review", parsedShops.size());

        if (parsedShops.isEmpty()) {
            throw new RuntimeException("未能从评论文本中解析出任何店铺信息");
        }

        // 推断城市：取第一个有城市信息的条目
        String city = parsedShops.stream()
                .filter(s -> s.getCity() != null && !s.getCity().isEmpty())
                .map(ReviewParseService.ParsedShop::getCity)
                .findFirst()
                .orElse("");

        // 2. 逐个验证并获取坐标
        List<ShopInfo> shops = new ArrayList<>();
        for (ReviewParseService.ParsedShop parsed : parsedShops) {
            ShopInfo shop = ShopInfo.builder()
                    .name(parsed.getName())
                    .address(parsed.getAddress())
                    .build();

            // 先用店名搜索 POI
            String searchCity = parsed.getCity() != null && !parsed.getCity().isEmpty()
                    ? parsed.getCity() : city;
            ShopInfo found = amapService.textSearch(parsed.getName(), searchCity);

            // 间隔 250ms 避免触发高德 QPS 限制
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}

            if (found != null) {
                shop.setLng(found.getLng());
                shop.setLat(found.getLat());
                shop.setAmapPoiId(found.getAmapPoiId());
                shop.setName(found.getName()); // 用高德返回的官方名称

                // 地址相似性验证
                boolean addressMatch = isAddressSimilar(parsed.getAddress(), found.getAddress());
                shop.setVerified(addressMatch);
                shop.setVerifyNote(addressMatch
                        ? "POI已验证"
                        : "POI存在但地址不完全匹配，高德地址: " + found.getAddress());
            } else {
                // POI 搜索失败，尝试地址编码
                double[] coords = amapService.geocode(parsed.getAddress(), searchCity);
                if (coords != null) {
                    shop.setLng(coords[0]);
                    shop.setLat(coords[1]);
                    shop.setVerified(false);
                    shop.setVerifyNote("未找到POI，通过地址编码定位");
                } else {
                    shop.setVerified(false);
                    shop.setVerifyNote("无法定位该店铺");
                }
            }

            shops.add(shop);
        }

        // 3. 计算距离（如果用户提供了位置）
        if (request.getUserLng() != null && request.getUserLat() != null) {
            computeDistances(shops, request.getUserLng(), request.getUserLat());
        }

        return NaviResult.builder()
                .userLng(request.getUserLng())
                .userLat(request.getUserLat())
                .city(city)
                .shops(shops)
                .build();
    }

    private void computeDistances(List<ShopInfo> shops, double userLng, double userLat) {
        // 筛选出有坐标的店铺
        List<ShopInfo> located = shops.stream()
                .filter(s -> s.getLng() != null && s.getLat() != null)
                .collect(Collectors.toList());

        if (located.isEmpty()) return;

        String origins = located.stream()
                .map(s -> s.getLng() + "," + s.getLat())
                .collect(Collectors.joining("|"));
        String destination = userLng + "," + userLat;

        List<Long> distances = amapService.batchDistance(origins, destination);
        for (int i = 0; i < located.size() && i < distances.size(); i++) {
            long dist = distances.get(i);
            located.get(i).setDistance(dist >= 0 ? (double) dist : null);
        }
    }

    /**
     * 简单的地址相似性判断：检查关键部分是否匹配
     */
    private boolean isAddressSimilar(String addr1, String addr2) {
        if (addr1 == null || addr2 == null) return false;
        String a1 = addr1.replaceAll("[\\s\\u00A0]", "");
        String a2 = addr2.replaceAll("[\\s\\u00A0]", "");
        // 如果完全相同
        if (a1.contains(a2) || a2.contains(a1)) return true;
        // 提取路名/地标进行模糊匹配
        String road1 = extractRoad(a1);
        String road2 = extractRoad(a2);
        return road1 != null && road2 != null && (road1.contains(road2) || road2.contains(road1));
    }

    private String extractRoad(String addr) {
        // 简单提取"XX路XX号"中的路名
        int roadIdx = addr.indexOf("路");
        if (roadIdx <= 0) return null;
        int start = Math.max(0, roadIdx - 5);
        return addr.substring(start, roadIdx + 1);
    }
}
