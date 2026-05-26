# 内置 WebView H5 桥接 JS API 开发文档

为了方便前端开发者与 App 实现深度融合，Clash Meta for Android 提供了一套原生的 JavaScript 桥接接口（Bridge Interface），注册在全局的 `window.Android` 命名空间下。

通过本接口，您可以动态修改手机系统状态栏和底部导航栏的颜色、调用系统浏览器打开特定链接，或是控制 WebView 容器的退出。

---

## 目录
1. [系统状态栏与底部导航栏变色](#1-系统状态栏与底部导航栏变色)
   - [方法 A：使用标准 HTML Meta 标签（推荐）](#方法-a使用标准-html-meta-标签推荐)
   - [方法 B：使用 JS 桥接 API 主动修改](#方法-b使用-js-桥接-api-主动修改)
   - [色彩解析器支持的格式](#色彩解析器支持的格式)
   - [智能通知栏图标反色机制](#智能通知栏图标反色机制)
2. [唤起系统默认浏览器](#2-唤起系统默认浏览器)
3. [关闭/退出内置 WebView](#3-关闭退出内置-webview)
4. [完整前端测试 H5 代码示例](#4-完整前端测试-h5-代码示例)

---

## 1. 系统状态栏与底部导航栏变色

App 会自动将系统顶部的状态栏与底部的虚拟按键/手势操作栏设为完全透明，使底色与 H5 页面完美契合。您可以通过以下两种方式来改变它们的背景颜色：

### 方法 A：使用标准 HTML Meta 标签（推荐）
在您的网页 `<head>` 标签中声明 `theme-color`。当页面加载时，App 会自动提取此颜色并渲染为系统状态栏和导航栏背景：
```html
<meta name="theme-color" content="#1A1A22">
```
*提示：App 内置了全局 `MutationObserver`。如果您在前端通过 JS 动态更新了该 meta 标签的 `content` 属性，系统栏颜色也会**实时同步变更**。*

### 方法 B：使用 JS 桥接 API 主动修改
您可以直接在 JavaScript 代码中调用 `setStatusBarColor` 方法：
```javascript
if (window.Android && typeof window.Android.setStatusBarColor === 'function') {
    window.Android.setStatusBarColor("#1A1A22");
}
```

### 色彩解析器支持的格式
App 的原生解析器极为健壮，支持以下所有标准的 CSS 颜色定义方式：
1. **精简 3 位十六进制**：`#F00` (等同于 `#FF0000`)
2. **精简带透明度 4 位十六进制**：`#F00F`
3. **标准 6 位十六进制**：`#1A1A22`
4. **带透明度 8 位十六进制**：`#1A1A22FF`
5. **CSS RGB 格式**：`rgb(26, 26, 34)`
6. **CSS RGBA 格式**：`rgba(26, 26, 34, 1.0)`
7. **预设 CSS 颜色名称**：`transparent`、`black`、`white`、`red`、`green`、`blue`、`yellow`、`cyan`、`magenta`、`gray`、`lightgray`、`darkgray`。

### 智能通知栏图标反色机制
App 会实时计算您传入的颜色亮度（Relative Luminance）：
*   若传入**浅色**背景（如 `white` / `#FFFFFF`），顶部的系统通知图标、电量、时钟以及底部的系统导航键会自动反色为**暗黑色**，保证清晰可读。
*   若传入**深色**背景（如 `black` / `#1A1A22`），系统图标会自动保持为**亮白色**。

---

## 2. 唤起系统默认浏览器

内置 WebView 属于应用内的沙盒环境。若您希望某些特定的外部链接（如外部支付链接、客服、社交媒体等）脱离 App 在手机系统默认浏览器（如 Chrome、Edge、Safari 等）中打开，可以使用此接口：

### 语法
```javascript
window.Android.openSystemBrowser(url);
```

### 示例
```javascript
if (window.Android && typeof window.Android.openSystemBrowser === 'function') {
    // 唤起外部默认浏览器并访问链接
    window.Android.openSystemBrowser("https://www.google.com");
}
```

---

## 3. 关闭/退出内置 WebView

当用户完成了某些流程（例如在 H5 充值页面支付成功、在帮助页点击了返回按钮等），您可以使用此接口直接关闭当前内置 WebView 容器，无缝返回到 VPN App 的首页：

### 语法
```javascript
window.Android.exitWebView();
```

### 示例
```javascript
if (window.Android && typeof window.Android.exitWebView === 'function') {
    // 销毁当前 WebViewActivity，返回上一页（VPN 首页）
    window.Android.exitWebView();
}
```

---

## 4. 完整前端测试 H5 代码示例

您可以直接将以下代码保存为一个 `test.html` 文件，上传至服务器或部署在网页中进行测试：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <!-- 默认设置状态栏和导航栏底色为高级暗夜灰 -->
    <meta name="theme-color" content="#1A1A22">
    <title>App WebView API 测试页面</title>
    <style>
        body {
            margin: 0;
            padding: 24px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: #1A1A22;
            color: #E2E8F0;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-start;
            min-height: 100vh;
            box-sizing: border-box;
        }

        h1 {
            font-size: 20px;
            margin-bottom: 24px;
            color: #FFFFFF;
            text-align: center;
        }

        .card {
            background-color: #2D3748;
            border-radius: 16px;
            padding: 20px;
            width: 100%;
            max-width: 400px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            margin-bottom: 20px;
            box-sizing: border-box;
        }

        .card-title {
            font-size: 16px;
            font-weight: bold;
            margin-top: 0;
            margin-bottom: 16px;
            border-bottom: 1px solid #4A5568;
            padding-bottom: 8px;
            color: #63B3ED;
        }

        button {
            width: 100%;
            padding: 12px 20px;
            margin-bottom: 10px;
            border: none;
            border-radius: 12px;
            font-size: 14px;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.2s ease;
            box-sizing: border-box;
        }

        button:last-child {
            margin-bottom: 0;
        }

        /* 颜色测试按钮样式 */
        .btn-dark { background-color: #1A1A22; color: #FFFFFF; border: 1px solid #4A5568; }
        .btn-light { background-color: #FFFFFF; color: #1A1A22; border: 1px solid #CBD5E0; }
        .btn-green { background-color: #2E7D32; color: #FFFFFF; }
        .btn-blue { background-color: #1A237E; color: #FFFFFF; }
        
        /* 功能测试按钮样式 */
        .btn-action { background-color: #3182CE; color: #FFFFFF; }
        .btn-exit { background-color: #E53E3E; color: #FFFFFF; }

        button:active {
            transform: scale(0.98);
            opacity: 0.9;
        }

        .status {
            font-size: 12px;
            color: #A0AEC0;
            margin-top: 8px;
            text-align: center;
        }
    </style>
</head>
<body>

    <h1>WebView Native 接口测试</h1>

    <!-- 1. 状态栏测试卡片 -->
    <div class="card">
        <div class="card-title">系统状态栏与导航栏测试</div>
        
        <button class="btn-dark" onclick="changeThemeColor('#1A1A22')">变更为深灰底色（亮白图标）</button>
        <button class="btn-light" onclick="changeThemeColor('#FFFFFF')">变更为白色底色（暗黑图标）</button>
        <button class="btn-green" onclick="changeThemeColor('#2E7D32')">变更为深绿底色（亮白图标）</button>
        <button class="btn-blue" onclick="changeThemeColor('rgb(26, 35, 126)')">使用 RGB 变更为深蓝底色</button>
        
        <div class="status" id="color-status">当前 Meta Theme Color: #1A1A22</div>
    </div>

    <!-- 2. 系统功能卡片 -->
    <div class="card">
        <div class="card-title">系统原生动作交互</div>
        
        <button class="btn-action" onclick="openBrowser()">用系统浏览器打开谷歌</button>
        <button class="btn-exit" onclick="closeWebView()">关闭网页返回 VPN 首页</button>
    </div>

    <script>
        // 动态修改 Meta Theme Color 触发 App 变色
        function changeThemeColor(color) {
            // 方法 A：修改 HTML Meta 属性（推荐，App 自动观察并同步）
            var meta = document.querySelector('meta[name="theme-color"]');
            if (meta) {
                meta.setAttribute('content', color);
                document.getElementById('color-status').innerText = "当前 Meta Theme Color: " + color;
            }
            
            // 实时修改页面本身的背景，使之与系统栏保持一致
            document.body.style.backgroundColor = color;
            if (color.toLowerCase() === '#ffffff' || color.toLowerCase() === 'white') {
                document.body.style.color = '#1A1A22';
                document.querySelector('h1').style.color = '#1A1A22';
            } else {
                document.body.style.color = '#E2E8F0';
                document.querySelector('h1').style.color = '#FFFFFF';
            }

            // 方法 B：手动调用桥接 JS 接口（效果完全相同）
            /*
            if (window.Android && typeof window.Android.setStatusBarColor === 'function') {
                window.Android.setStatusBarColor(color);
            }
            */
        }

        // 调用系统浏览器
        function openBrowser() {
            if (window.Android && typeof window.Android.openSystemBrowser === 'function') {
                window.Android.openSystemBrowser("https://www.google.com");
            } else {
                alert("未检测到 Android 原生桥接接口，可能当前不在内置 WebView 容器中运行。");
            }
        }

        // 退出 WebView 容器
        function closeWebView() {
            if (window.Android && typeof window.Android.exitWebView === 'function') {
                window.Android.exitWebView();
            } else {
                alert("未检测到 Android 原生桥接接口，可能当前不在内置 WebView 容器中运行。");
            }
        }
    </script>
</body>
</html>
```
