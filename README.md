# 🗺️ Campus Mood Map — 校园情绪地图

> **一款基于 Android 的交互式校园地图应用，融合地理位置服务、情绪打卡与路径导航功能。**

---

## 📱 项目简介

**Campus Mood Map** 是一个面向校园场景的 Android 移动应用，以 Google Maps 为基础，为用户提供校园建筑物的可视化地图展示、实时导航、情绪打卡、评论互动以及地理围栏自动解锁等丰富功能。

用户可以在校园地图上浏览各栋建筑物，查看当前建筑的"情绪色彩"（基于用户评论的 emoji 统计），发表带 emoji 的评论来表达对该地点的感受，并通过 Google Maps 导航或应用内步行路径规划轻松到达目的地。

---

## ✨ 主要功能

### 🏫 交互式校园地图
- 基于 **Google Maps SDK** 的校园地图展示
- 8 个校园建筑物标记点，每个标记以 emoji 图标呈现
- 建筑物覆盖圆形区域（Geofence 区域），颜色随用户情绪反馈动态变化

| 建筑物 | 代号 | 说明 |
|--------|------|------|
| U Garden | UG | 花园餐厅 🍕 |
| Communal Canteen | WK |  communal 食堂 🍞 |
| VA Canteen | VA | VA 餐厅 🍜 |
| Block Z | Z | Z 座教学楼 📚 |
| Lawn Restaurant | L | 草坪餐厅 🍝 |
| H Cafe | H | H 咖啡馆 ☕️ |
| Auditorium | JC | 赛马会 auditorium ⛪️ |
| Library | lib | 包玉刚图书馆 📚 |

### 📍 地理围栏（Geofence）
- 应用注册 8 个地理围栏区域，覆盖每个建筑物周边
- 当用户 **进入或停留在** 建筑物范围内时，自动"解锁"该建筑物的评论功能
- 使用 Android `GeofencingClient` API 实现，支持后台位置监听
- 解锁状态持久化保存，无需重复解锁

### 🧭 导航功能
点击建筑物信息窗口弹出三种操作：
- **🗺 应用内导航** — 调用 Google Directions API 获取步行路线，在地图上绘制路径折线
- **🌐 Google Maps 导航** — 跳转至 Google Maps 应用进行导航
- **🌆 街景模式** — 启动 Google Street View，查看建筑物实景

### 😊 情绪打卡与评论
- 为每栋建筑物发表带有 **emoji 情绪** 的评论
- 支持 4 种 emoji 选择：😊 😍 😡 😞
- 评论列表实时刷新，可删除评论
- 系统根据评论 emoji 统计计算 **主导情绪**：
  - 🟢 **绿色** — 积极情绪占优（😊 / 😍 / 👍）
  - 🔴 **红色** — 消极情绪占优（😡 / 😞 / 👎）
  - 🟡 **黄色** — 情绪中性
  - 情绪颜色会实时反映在地图的圆形覆盖区域上

### 🔒 解锁机制
- 建筑物评论功能默认为 **锁定状态**
- 用户必须实际到达该建筑物附近（触发 Geofence）方可解锁
- 解锁后该建筑物的评论面板可见，用户可发表/查看评论
- 也提供测试用"一键解锁"按钮便于演示

### 💾 本地数据持久化
- 使用 **SharedPreferences** 存储评论数据和建筑物解锁状态
- 数据以 JSON 格式序列化，应用启动时自动恢复
- 采用单例模式 `CommentStore` 统一管理

---

## 🏗️ 项目结构

```
Groupproject/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/groupproject/
│   │   │   │   ├── MainActivity.java              # 应用主入口
│   │   │   │   ├── model/
│   │   │   │   │   ├── Building.java               # 建筑物数据模型
│   │   │   │   │   ├── Comment.java                # 评论数据模型
│   │   │   │   │   ├── CommentStore.java           # 评论 & 解锁状态管理（单例）
│   │   │   │   │   └── Mood.java                   # 情绪页面 Activity
│   │   │   │   ├── ui/
│   │   │   │   │   ├── map/
│   │   │   │   │   │   └── CampusMapFragment.java  # 校园地图核心 Fragment
│   │   │   │   │   └── detail/
│   │   │   │   │       └── BuildingDetailFragment.java  # 建筑物详情 & 评论 Fragment
│   │   │   │   ├── receiver/
│   │   │   │   │   └── GeofenceBroadcastReceiver.java   # 地理围栏广播接收器
│   │   │   │   └── data/local/
│   │   │   │       └── AppDatabase.java            # 数据库 Activity（预留）
│   │   │   ├── res/
│   │   │   │   ├── layout/                         # UI 布局文件
│   │   │   │   ├── navigation/
│   │   │   │   │   └── nav_graph.xml               # 导航图配置
│   │   │   │   ├── values/                         # 颜色、字符串、主题
│   │   │   │   ├── drawable/                       # 图标资源
│   │   │   │   └── mipmap-*/                       # 应用图标
│   │   │   └── AndroidManifest.xml                 # 应用清单
│   │   ├── test/                                   # 单元测试
│   │   └── androidTest/                            # 仪器化测试
│   ├── build.gradle.kts                            # 模块构建配置
│   └── proguard-rules.pro
├── build.gradle.kts                                # 顶层构建配置
├── settings.gradle.kts                             # 项目设置
├── gradle.properties                               # Gradle 属性
├── gradlew / gradlew.bat                           # Gradle 包装器
└── .gitignore
```

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| **Java** | 主要开发语言 |
| **Android SDK** (minSdk 24, targetSdk 36) | Android 平台 |
| **Google Maps SDK** | 地图展示与交互 |
| **Google Location Services** | 位置获取 & Geofence |
| **Google Directions API** | 步行路径规划 |
| **Android Navigation Component** | Fragment 导航管理 |
| **SharedPreferences** | 本地数据持久化 |
| **AndroidX / Material Design** | UI 组件与主题 |
| **Gradle Kotlin DSL** | 构建系统 |

---

## 🚀 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更高版本
- **JDK** 17+
- **Google Maps API Key**（需启用 Maps SDK、Directions API、Geolocation API）

### 配置步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/campus-mood-map.git
   cd Groupproject
   ```

2. **获取 Google Maps API Key**
   - 前往 [Google Cloud Console](https://console.cloud.google.com/)
   - 创建或选择项目
   - 启用以下 API：
     - Maps SDK for Android
     - Directions API
   - 创建 API Key 并设置 Android 应用限制（SHA-1 指纹）

3. **配置 API Key**
   - 打开 `app/src/main/res/values/strings.xml`
   - 添加：
     ```xml
     <string name="google_maps_key">YOUR_API_KEY_HERE</string>
     ```

4. **构建运行**
   - 在 Android Studio 中打开项目
   - 同步 Gradle
   - 连接 Android 设备或启动模拟器（需支持 Google Play 服务）
   - 点击 Run

---

## 📖 使用说明

1. **启动应用** — 进入校园地图主界面
2. **授权位置权限** — 应用需要精确定位权限以显示您的位置和触发地理围栏
3. **浏览校园** — 地图上显示各建筑物 emoji 标记，点击标记查看信息
4. **选择操作** — 点击信息窗口可选择导航、查看评论或街景
5. **解锁评论** — 走到建筑物附近自动解锁，或使用测试按钮手动解锁
6. **发表情绪** — 选择 emoji 并输入评论内容，提交后该区域颜色会变化
7. **路线规划** — 选择"应用内导航"，地图上会绘制步行路径折线

---

## 📸 演示视频

演示视频.mp4


---

## 📸 截图

地图 map_view.png
评论 comment.png


---

## 🧩 核心架构

```
┌─────────────────────────────────────────────────┐
│                  MainActivity                    │
│          (NavHost / Navigation Component)        │
└────────────────────┬────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
┌─────────────────┐   ┌─────────────────────────┐
│ CampusMapFragment│   │ BuildingDetailFragment   │
│                 │   │                         │
│ • Google Maps   │──▶│ • 情绪打卡              │
│ • Geofence      │   │ • 评论列表              │
│ • 导航/街景     │   │ • 解锁控制              │
│ • 情绪色彩渲染  │   │ • 删除评论              │
└────────┬────────┘   └─────────────────────────┘
         │
         ├── GeofenceBroadcastReceiver ◄── GeofencingClient
         │
         └── CommentStore (单例) ◄── SharedPreferences
```

---

## 🤝 参与贡献

欢迎提交 Issue 或 Pull Request 来改进本项目！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

---

## 📄 开源协议

本项目基于 **MIT License** 开源。

---

## 👥 开发团队

- **LSGI541 Group Project Group2** — 课程小组项目作品

---

## 🙏 致谢

- [Google Maps Android SDK](https://developers.google.com/maps/documentation/android-sdk)
- [Android Geofencing API](https://developer.android.com/training/location/geofencing)
- [Android Navigation Component](https://developer.android.com/guide/navigation)
- [Material Design](https://material.io/design)
