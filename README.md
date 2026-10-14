# Vertex2OpenAI Android 本地桥（SillyTavern 专用）

> **状态：未实机验证的 Android 工程 / 构建方案，不是已经编译的 APK。**

目标是把 [bad-woman/vertex2openai](https://github.com/bad-woman/vertex2openai) 原始 FastAPI 服务（固定在 2026-10-13 的 commit `53a45b63bfef39e783edf8ea9c1e06800fc17496`）嵌到独立 Android APK：

- 安装后无需 Termux、Docker；APP 内一键启动、停止本机服务；
- 后台以前台通知服务保持活动；
- 代理仅监听 `127.0.0.1:8050`；
- 内置 WebView 打开原项目管理后台，设置 Express API Key 或 Cookie 与 Project ID；
- 首次安装随机生成本地 API Key，并提供复制按钮；
- Cookie / Google Key **不在 APK 源码中**，写入应用私有目录；
- SillyTavern 使用 URL `http://127.0.0.1:8050/v1`，API Key 使用本 APP 内复制的密钥。

## 重要限制

1. **当前会话无法构建/签名 APK，也没有实机调试。** 这是源工程，并不保证 Gradle 依赖安装或 Android 运行时可立即成功。
2. 构建过程依赖网络下载 Android SDK、Gradle、Chaquopy、Python Wheels、原项目源码。
3. Python 版使用 Chaquopy 17.1.0 / Python 3.12 / `pydantic==2.13.4` 与 `pydantic-core==2.46.4`；该 core 依赖 Android wheel 来自 `https://pypi.flet.dev/`，实际 ABI 兼容性需编译并测试。
4. `curl_cffi` 出现在原仓库 requirements，但所检查的锁定源代码未引用它，因此为减少原生依赖从 Android 安装列表去除；若后续上游增加引用，会在 CI 阻止构建。
5. Google Cookie 模式依赖 Google Cloud **非公开接口**，存在失效、服务条款/账户风险，并要求你有相应的项目权限/计费；绝不要公开分享 Cookie 或放开本代理端口。
6. Android 可能终止后台应用。不可保证 24 小时运行；仅支持本机的 SillyTavern，不能跨设备使用 `127.0.0.1`。
7. APK 是 debug 签名，仅供个人测试；不适合未经审查的正式分发。

## 获取 APK：GitHub Actions（不需要电脑上的 Android SDK）

1. 在 GitHub 创建一个 **private repository**（私有空仓库，名称随意）。
2. 把本 ZIP **解压后的整个目录内容**提交到仓库根目录，保留 `.github/workflows/build-apk.yml` 文件夹路径。
3. GitHub 仓库上方打开 **Actions** → **Build Android APK** → **Run workflow**。推送到 `main` 分支也会触发。
4. 如果工作流成功，在对应运行页面底部 **Artifacts** 下载 `Vertex2OpenAI-Android-arm64-debug.zip`，解压得到 `app-debug.apk`。
5. 如果工作流失败，把错误日志交给编写者修订工程；**失败意味着还没有可安装 APK**。

> 仅手机操作上传整个项目比较繁琐。可以在手机上使用 GitHub Codespaces 的网页版终端或任意支持 git 的客户端，也可以使用电脑上传 ZIP 解压后的文件夹。

## 安装后的使用

- 开启应用 → **启动代理** → 等待状态显示“运行中”。
- 点 **打开代理管理后台**，用应用的“复制 API Key”获取后台登录密码。
- 在“通道与凭证”中添加你的 Google Vertex Express API Key 或 Cookie + Project ID，保存。
- 打开手机上的 SillyTavern，选 OpenAI / Chat Completion 兼容接口：
  - API Base URL: `http://127.0.0.1:8050/v1`
  - API Key: 在本应用里复制
  - Model: 在管理后台实际可用的模型名称，如 `gemini-3.7-flash`（仅举例，以 Google 实际访问权限为准）
- 如果 SillyTavern 的 API URL 编辑框已经自动附带 `/v1`，避免重复填写 `/v1/v1`。

## 调试

本方案尚未编译验证。实测时可用 ADB 查看：

```bash
adb logcat -s VertexBridge AndroidRuntime python.stdout python.stderr
```

若 APK 能安装但打不开管理后台，检查应用状态文字是否为启动失败。构建问题通常发生在 Android 版 Python wheel；运行问题通常是 Python 模块导入或 Android 后台限制。

## 上游版权与来源

Android 包装代码为新建样例，原项目归 [bad-woman/vertex2openai](https://github.com/bad-woman/vertex2openai) 及其 MIT License 所有；云端构建时只按固定 commit 获取原仓库文件，不预装任何密钥。
