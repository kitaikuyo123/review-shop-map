package com.navi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaviRequest {
    private String content;
    private Double userLng;
    private Double userLat;
}
