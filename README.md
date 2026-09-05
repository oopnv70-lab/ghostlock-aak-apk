# GhostLock AAK Launcher

> CVE-2026-43499 · Linux kernel rtmutex stack Use-After-Free 本地提权（LPE）加载器/触发器

**本仓库是一个安全研究与漏洞验证（PoC）项目**，用于在**受控测试环境**中演示、验证 CVE-2026-43499（GhostLock）漏洞的触发流程。

---

## 项目简介

这是一个配合外部 preload.so 使用的 Android 应用（APK）「加载器/触发器」。

- **目标漏洞**：CVE-2026-43499（Linux kernel kernel/locking/rtmutex.c，stack UAF）
- **作用**：通过 Shizuku 获取 shell 权限（uid 2000），将外部 preload.so 写入 /data/local/tmp/preload.so，并以 LD_PRELOAD 注入 /system/bin/sh，触发 preload.so 的构造函数 run_exploit() 执行完整利用链。
- **验证对象**：荣耀（Honor）AAK-AN00 · MagicOS 10 / Android 15 · 内核 6.6.89-android15-8

### 技术要点

- 正确选择 **shell 权限**（uid 2000）触发环境，规避 untrusted_app 沙箱 Seccomp=2 对 futex PI 操作的拦截
- LD_PRELOAD 注入，无需修改宿主进程代码
- 双线程并行读取 stdout/stderr，防管道死锁
- 遵循官方 Shizuku UserService 模式，实时日志回调

> 更详细的漏洞原理与利用链说明，见 [EXPLOIT_CN.md](EXPLOIT_CN.md)。

---

## 当前状态（重要）

> **⚠️ 本仓库目前尚处于「未充分验证」状态。**

- **本项目未经过系统的、广泛的设备实测。** 漏洞触发是否能在特定设备/固件/内核版本上**稳定成功，目前未知（Unknown / Unverified）**。
- 触发成功率受多种工程因素影响，例如：
  - **KASLR slide 爆破**：地址随机化枚举可能失败
  - **硬编码偏移**：偏移表依赖特定固件版本（6.6.89-android15-8），不同厂商补丁、不同内核构建可能存在差异
  - **厂商安全补丁**：设备若已应用相关修复，漏洞可能已被缓解
- **因此，本仓库不保证任何设备上都能复现或利用成功。**
- 请在受控、隔离的测试设备上进行验证，切勿在生产环境或他人的设备上使用。

### 状态速览

| 项目 | 状态 |
|------|------|
| 漏洞真实性（CVE-2026-43499） | 已由 NVD 权威确认 |
| 源码设计原理 | 逻辑自洽，工程实现规范 |
| 设备实测成功率 | **未知 / 未验证** |
| 多固件兼容性 | 未验证（依赖硬编码偏移） |
| 是否可在任意设备提权 | 不能保证 |

---

## 使用说明

> **仅限授权、受控的研究环境使用。**

1. 安装并启动 **Shizuku**（v11+）
2. 安装本 APK，点击「申请 Shizuku 权限」
3. 选择要注入的 preload.so 文件
4. 点击「运行 Exploit」，观察日志输出

详见 [EXPLOIT_CN.md](EXPLOIT_CN.md) 的调试观察点表格。

---

## 免责声明（Disclaimer）

**本软件按「原样」（AS IS）提供，不作任何明示或暗示的保证，包括但不限于适销性、特定用途适用性及不侵权的保证。**

- 本仓库仅用于**安全研究、漏洞分析与教育目的**。
- 使用者应确保其在**合法授权**的范围内使用本软件，并自行承担使用本软件产生的**全部风险与法律责任**。
- 作者对因使用本软件而导致的任何直接、间接、附带或后果性损害（包括但不限于设备损坏、数据丢失、系统不稳定、账户或权限被非法篡改、法律纠纷等）**概不负责**。
- 本软件不得用于任何**非法用途**，包括但不限于未经授权访问、恶意攻击、数据窃取或破坏他人系统。
- 使用者须遵守其所在地及目标系统所属辖区的**全部适用法律与法规**。

**通过下载、使用、分发本软件，即表示你已阅读、理解并同意本免责声明的全部条款。**

---

## 法律声明（Legal Notice）

### 1. 双用途技术（Dual-Use）

本仓库包含漏洞利用相关代码，属于**双用途（dual-use）**技术，既可服务于合法的安全研究与防御，也可能被误用于攻击。分发此类技术应遵循**负责任披露（Responsible Disclosure）**原则。

### 2. 漏洞披露协调

- 本漏洞已分配编号 **CVE-2026-43499**，修复提交：rtmutex: Use waiter::task instead of current in remove_waiter()。
- 建议优先通过官方渠道向厂商/内核社区报告，并关注**官方安全公告**，及时更新受影响系统。

### 3. 合规与司法辖区

- 不同国家和地区对漏洞利用代码、渗透测试工具的分发与使用有不同法律规定（例如部分辖区的《网络安全法》《数据安全法》《计算机信息系统安全保护条例》，以及各国关于未授权访问的刑事/民事条款）。
- **使用或分发本软件前，请务必确认你所在司法辖区的相关法规。** 非法使用或传播可能构成违法犯罪。

### 4. 授权前提

- 仅允许在**你拥有合法所有权或已获书面授权的系统/设备**上进行测试。
- 未经授权的测试可能违反法律，并构成对他人权益的侵害。

---

## 许可证

本项目基于 **GNU General Public License v3.0（GPLv3）** 开源发布。详见 [LICENSE](LICENSE)。

```
Copyright (C) 2026 oopnv70-lab

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.
```

---

## 安全与合规提醒

- 公开此类漏洞利用相关代码存在被误用、举报或下架的风险，请评估后再决定分发范围。
- 建议持续关注 **GitHub 社区准则** 与安全研究相关政策。
- 请勿将此仓库用于任何恶意活动。

---

*本仓库仅用于漏洞研究与教育目的。请合规、负责任地使用。*
