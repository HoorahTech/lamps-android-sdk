# RewardAdAbility

## Method

`hra.ad.showRewardedVideo`

H5 一次调用，客户端完成 load → 竞价 → show。不传 `slotId`，不选联盟。同一 WebView 同时只允许一条激励流程。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `forward_source` | string | 否 | 场景来源，写入监测 `__FORWARD_SOURCE__` |

```json
{
  "forward_source": "h5_game"
}
```

## 返回值

全部通过 `hoorah.ad.rewardedVideoStatus` 回调，用 `callbackName` 区分。不要等 invoke 业务回包。

### `onLoadSuccess`

加载 + 竞价成功，客户端即将 show。

```json
{
  "callbackName": "onLoadSuccess"
}
```

### `onLoadError`

加载失败，流程结束，可再次调用 method。调用当下失败（SDK 未 Ready、无广告位、流程进行中等）也走这条。

```json
{
  "callbackName": "onLoadError",
  "data": {
    "errCode": 2009,
    "errMsg": "all reward ad SDKs failed to load"
  }
}
```

### `onShowSuccess`

广告已展示。

```json
{
  "callbackName": "onShowSuccess"
}
```

### `onShowError`

展示失败，流程结束。

```json
{
  "callbackName": "onShowError",
  "data": {
    "errCode": 2007,
    "errMsg": "YLH reward ad is invalid or has already been shown"
  }
}
```

### `onRewardArrived`

用户完成激励任务。`rewardStatus` 恒为 `true`。

```json
{
  "callbackName": "onRewardArrived",
  "rewardStatus": true
}
```

### `onClose`

广告关闭，流程结束。是否发奖看 `rewardStatus`（本轮是否到过 `onRewardArrived`）。

```json
{
  "callbackName": "onClose",
  "rewardStatus": true
}
```

未完成任务关闭：

```json
{
  "callbackName": "onClose",
  "rewardStatus": false
}
```

发奖：`onRewardArrived` 或 `onClose.rewardStatus === true`。不要用 `onShowSuccess` 发奖。

### `errCode`

| errCode | 含义 |
| --- | --- |
| 2001 | SDK 未 Ready |
| 2002 | 无 Activity |
| 2003 | 无激励广告位 |
| 2004 | 无可用 Provider |
| 2005 | 流程进行中 |
| 2006 | 广告位非法 |
| 2007 | 平台 SDK 错误 |
| 2008 | 单 SDK 加载超时 |
| 2009 | 全部 SDK 加载失败 |
| 其它 | 三方 SDK 原始错误码 |
