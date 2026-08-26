# Maven Central 发布配额

## 当前配额快照

数据来源：Central Portal Usage Overview 截图，统计周期为当前月。

| 指标 | 月度限制 | 截图中的已用量 | 剩余额度 |
| --- | ---: | ---: | ---: |
| Release Size | 80 MB | 4.63 MB | 75.37 MB |
| File Count | 1,000 | 540 | 460 |
| Release Count | 7 | 2 | 5 |

这些配额按组织和 namespace 聚合统计，不是单个项目独立额度。文件数包含 AAR、POM、Gradle Module Metadata、Javadoc 和签名文件等发布附件。

## 发布前置流程

每次执行 Maven Central 发布前，必须先在 Central Portal 查看 Usage Overview，并向发布人报告：

1. 当前月已用 Release Size、File Count、Release Count。
2. 本次发布预计增加的文件数、大小和 release 次数。
3. 发布后预计剩余额度。

只有发布人明确确认后，才允许执行 `publishToMavenCentral`、`publishAndReleaseToMavenCentral` 或 `push`。未确认时不得执行真实上传。

超过限制通常会导致发布被拒绝或暂时限制，不会自动按超额量收费；额度一般在下个统计周期重置。如确需提高配额，应联系 Sonatype 支持确认政策和申请方式。

## 本项目发布注意事项

- 发布前不要把 Central Token、GPG 私钥或密码提交到 Git。
- 同一个 Maven 坐标和版本发布后不能覆盖；发布前必须核对 group、artifactId 和 version。
- 发布完成后等待 Central 同步，再用 Maven Central 查询坐标是否可解析。
- 每次发布后回填本文件的配额快照，避免仅依赖旧截图。
