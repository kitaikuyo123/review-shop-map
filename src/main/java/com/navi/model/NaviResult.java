package com.navi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NaviResult {
    private Double userLng;
    private Double userLat;
    private List<ShopInfo> shops;
    private String city;
}
