# TrackSdk 上报协议

本文档描述 `lamps-android-sdk` 中 `TrackSdk.sendData` 的上报格式，供服务端接收和解析。

## 请求地址

SDK 使用固定路径 `REPORT_PATH=/api/v1/event/report`，域名根据 SDK 当前环境选择：

| 环境 | 请求地址 |
| --- | --- |
| 测试环境 | `https://lamps-api-sit.hoorahgo.com/api/v1/event/report` |
| 线上环境 | `https://lamps-api.hoorahgo.com/api/v1/event/report` |

请求方法为 `POST`，请求头为：

```http
Content-Type: application/json
```

连接超时和读取超时均为 10 秒。

## 请求体编码

请求体不是明文 JSON，而是按以下顺序处理后的字符串：

```text
JSON UTF-8 字节
  -> GZIP 压缩
  -> Base64 编码（无换行）
```

服务端处理顺序应为：

```text
Base64 解码
  -> GZIP 解压
  -> UTF-8 解码
  -> JSON 解析
```

## 解压后的 JSON

```json
{
  "ts": "1787638359",
  "ua": "Mozilla/5.0 ...",
  "ip": "61.173.157.243",
  "mac": "02:00:00:00:00:00",
  "imei": "",
  "os": "Android",
  "androidId": "...",
  "oaid": "...",
  "appid": "10002",
  "sdkVersion": "0.0.4",
  "phoneBrand": "Xiaomi",
  "network": "wifi",
  "type": "event_type",
  "pdata": {
    "key": "value",
    "duration": 123
  }
}
```

## 外层字段

所有设备公共字段和 `type` 都位于 JSON 外层。除 `pdata` 中调用方自行传入的字段外，SDK 默认字段值均按字符串发送。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ts` | string | 当前 Unix 时间戳，单位为秒 |
| `ua` | string | 设备 User-Agent |
| `ip` | string | 初始化接口返回的客户端 IP；没有时为空字符串 |
| `mac` | string | 设备 MAC；不可用时为空字符串 |
| `imei` | string | 设备 IMEI；不可用时为空字符串 |
| `os` | string | 固定为 `Android` |
| `androidId` | string | Android ID；获取失败时使用并持久化 UUID |
| `oaid` | string | OAID；不可用时为空字符串 |
| `appid` | string | 宿主应用 App ID |
| `sdkVersion` | string | SDK 版本号 |
| `phoneBrand` | string | 手机品牌 |
| `network` | string | 当前网络类型：`wifi`、`2g`、`3g`、`4g`、`5g` 或 `unknown` |
| `type` | string | 调用 `sendData` 时传入的事件类型 |
| `pdata` | object | 调用方传入的业务数据，字段和值保持 JSON 结构 |

`pdata` 没有固定字段，服务端应按 `type` 解释其内容。没有业务字段时，`pdata` 为 `{}`。

## 响应约定

SDK 只根据 HTTP 状态码判断是否成功：

- `2xx`：视为上报成功；
- 非 `2xx` 或网络异常：视为上报失败并写入 SDK 日志；
- SDK 不解析响应体，也不会因为服务端响应内容改变业务回调结果。

## 代码入口

- `TrackSdk.sendData`：`core/src/main/java/com/lamps/sdk/utils/TrackSdk.kt`
- 请求发送：`TrackSdk.doReport`
- 公共字段生成：`TrackSdk.buildDefaultValues`
