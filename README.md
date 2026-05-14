# 评论店铺地图导航

解析用户评论文本，通过 LLM 提取店铺名称和地址，调用高德地图 API 验证 POI 并获取坐标，最终在地图上可视化展示店铺位置和距离。

## 技术栈

- **Java 21** / **Spring Boot 3.4.5**
- **Maven** 构建
- **高德地图 JS SDK v2.0** 前端地图
- **高德 Web API** POI 搜索、地址编码、距离计算
- **DeepSeek LLM** 评论文本解析
- **Tailwind CSS** (CDN) 前端样式

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+

### 配置

在 `application.yml` 中填入自己的 API Key：

```yaml
llm:
  apiKey: "your-llm-api-key"
  baseUrl: "your-llm-base-url"
  modelName: "your-llm-model-name"

amap:
  apiKey: "your-amap-web-api-key"
  jsApiKey: "your-amap-js-api-key"
```

- `llm.apiKey` — 兼容 OpenAI 接口的 API Key
- `amap.apiKey` — [高德 Web 服务 Key](https://lbs.amap.com/api/webservice/create-project-and-key)
- `amap.jsApiKey` — [高德 JS API Key](https://lbs.amap.com/api/javascript-api-v2/prerequisites)

### 运行

```bash
mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
```

启动后访问 **http://localhost:12000**。

## API

### POST `/api/navi/parse`

解析评论文本，返回店铺列表及坐标。

**请求体：**

```json
{
  "content": "1. 某某餐厅 杭州市西湖区XX路123号\n2. 另一家店 杭州市拱墅区YY路456号",
  "province": "浙江省",
  "userLng": 120.15,
  "userLat": 30.28
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 含店铺名称和地址的评论文本 |
| `province` | String | 否 | 限定搜索省份，不填则由 LLM 自动推断 |
| `userLng` | Double | 否 | 用户经度，用于计算距离 |
| `userLat` | Double | 否 | 用户纬度 |

**响应：**

```json
{
  "userLng": 120.15,
  "userLat": 30.28,
  "city": "浙江省",
  "shops": [
    {
      "name": "某某餐厅",
      "address": "西湖区XX路123号",
      "lng": 120.1234,
      "lat": 30.2345,
      "verified": true,
      "verifyNote": "POI已验证",
      "distance": 1250.0,
      "amapPoiId": "B0FFFXXXXX"
    }
  ]
}
```

### GET `/api/config/amap-js-key`

前端获取高德 JS API Key。

```json
{ "jsApiKey": "4e10d92a..." }
```

## 处理流程

```
评论文本 → LLM 提取店铺名+地址+城市
         → 高德 POI 搜索（限定省份/城市范围）
         → 地址相似度验证
         → 批量距离计算
         → 前端地图 + 店铺列表
```

## 项目结构

```
src/main/java/com/navi/
├── NaviApplication.java          # 启动入口
├── config/
│   ├── AmapConfig.java           # 高德 API Key 配置
│   └── LlmConfig.java            # LLM 配置
├── controller/
│   └── NaviController.java       # REST 接口
├── model/
│   ├── NaviRequest.java          # 请求 DTO
│   ├── NaviResult.java           # 响应 DTO
│   └── ShopInfo.java             # 店铺信息
├── service/
│   ├── NaviService.java          # 核心编排逻辑
│   ├── ReviewParseService.java   # LLM 解析服务
│   └── AmapService.java          # 高德 API 封装
└── util/
    └── HttpClientUtil.java       # HTTP 工具类
```
