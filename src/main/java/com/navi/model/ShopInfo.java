package com.navi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfo {
    private String name;
    private String address;
    private Double lng;
    private Double lat;
    private boolean verified;
    private String verifyNote;
    private Double distance;
    private String amapPoiId;
}
