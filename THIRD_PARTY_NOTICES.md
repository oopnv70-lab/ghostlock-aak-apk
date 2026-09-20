# 第三方资源声明 / Third-Party Notices

本仓库包含来自第三方开源项目的内容。以下列出其来源、许可证与完整许可文本。

---

## 1. 应用图标（启动器图标）

| 项目 | 说明 |
|---|---|
| **资源名称** | `shield-lock`（盾牌-锁） |
| **来源项目** | [Tabler Icons](https://github.com/tabler/tabler-icons) |
| **原始文件** | [`icons/outline/shield-lock.svg`](https://github.com/tabler/tabler-icons/blob/main/icons/outline/shield-lock.svg) |
| **作者** | Paweł Kuna |
| **许可证** | MIT License |
| **Copyright** | Copyright (c) 2020-2026 Paweł Kuna |

### 在本仓库中的使用方式

原始素材为 24×24 视口的 SVG。本仓库对其做了**纯格式转换**（未改动任何图形内容）：

- `app/src/main/res/drawable/ic_launcher_foreground.xml`
  转换为 Android VectorDrawable 格式，仅将路径数据复制为 `<path android:pathData="...">`，并设置了前景绘制所需的缩放与描边颜色（白色）。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  组合前景与背景色，声明为 Android 自适应图标（Adaptive Icon）。
- `app/src/main/res/values/ic_launcher_background.xml`
  背景色 `#004D40`（本仓库自定义，非第三方素材）。

> 说明：SVG → VectorDrawable 的转换属于对原素材的**再格式化**，图形轮廓数据保持原样。按 MIT 许可证要求，本文件保留了原作者的版权声明与许可证文本。

### MIT License 全文

```
MIT License

Copyright (c) 2020-2026 Paweł Kuna

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 2. 许可证兼容性说明

- 本仓库主体以 **GNU General Public License v3.0 (GPLv3)** 发布，见 [`LICENSE`](LICENSE)。
- 上表中的图标素材为 **MIT 许可证**。
- MIT 与 GPLv3 兼容：MIT 许可的代码/素材可以被纳入 GPLv3 项目。合并分发时，整体仍按 GPLv3 授权，同时保留 MIT 部分原有的版权与许可声明（即本文件）。

---

## 3. 其它依赖

Android 构建时引用的第三方库（如 `dev.rikka.shizuku:api`、`dev.rikka.shizuku:provider`、`androidx.annotation`）各自遵循其原项目的许可证，详见对应上游仓库。本文件仅覆盖本仓库**直接内嵌**的第三方资源（当前即上述图标素材）。

---

*若你认为本仓库的某项内容侵犯了你的权益，请通过 Issue 联系，我们会及时处理。*
