# 智能小说阅读与创作平台（Smart Novel Platform）

原创前后端分离全栈项目。已彻底清除原开源项目（`io.github.xxyopen` / `201206030/novel`）的包名与历史残留，作为独立仓库维护。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4.5 + Java 21 + MyBatis-Plus 3.5.6 + Spring Security + JWT + Redis + Caffeine + Spring AI 1.0.0（智谱 `glm-4-flash` / `embedding-3`） |
| 前端 | Vue 3（Composition API `<script setup>`）+ Vite 5 + Element Plus + Pinia + Vue Router 4 + ECharts 5 + Axios |
| 数据 | MySQL 8.0（独立库 `smart_novel_pro`）+ Redis 5.0 |

## 目录结构

```
SmartNovel-platform/
├── backend/          # Spring Boot 后端（com.myproject.novel，端口 8888）
│   ├── src/main/java/com/myproject/novel/
│   │   ├── common/        # 常量、异常、结果封装（RestResp、BusinessException）
│   │   ├── config/        # Security、CORS、AI、Redis 配置
│   │   ├── controller/
│   │   │   ├── admin/     # 管理员接口（11 个控制器，40 个端点）
│   │   │   ├── author/    # 作者接口
│   │   │   └── front/     # 前台接口（阅读、社区、用户、搜索、推荐）
│   │   ├── dao/           # entity + mapper（MyBatis-Plus）
│   │   ├── dto/           # req（请求）/ resp（响应）DTO
│   │   ├── manager/       # 业务编排（AI RAG 索引、推荐刷新）
│   │   ├── security/      # JWT 过滤器 + UserContext（三端权限隔离）
│   │   └── service/       # 业务服务层
│   └── src/main/resources/
│       ├── db/schema.sql  # 全量表结构（26 张表）+ 种子数据
│       └── application.yml
├── frontend/         # Vue 3 前端（Vite 开发服务器，端口 5173）
│   ├── src/
│   │   ├── components/    # 可复用组件（BookCover 拟物书封等）
│   │   ├── router/        # Vue Router 路由（含全局鉴权守卫）
│   │   ├── stores/        # Pinia 状态管理（auth）
│   │   ├── utils/         # Axios 封装（JWT 拦截器、RestResp 解包）
│   │   └── views/
│   │       ├── admin/     # 管理员端 11 个页面
│   │       ├── author/    # 作者端 5 个页面
│   │       └── *.vue      # 用户端 12 个页面
│   └── vite.config.js     # Vite 配置（含 /api 代理到 :8888）
├── SPEC.md           # 项目规格书
└── README.md
```

## 环境要求

- JDK 21（设置 `JAVA_HOME`，或确保 `java` 在 PATH 中）
- MySQL 8.0.45（端口 3306）
- Redis 5.0.14（端口 6379）
- Node.js 24
- 环境变量 `MYSQL_PASSWORD`（本机 MySQL root 密码，**必须设置**，否则后端无法连接数据库）
- 环境变量 `JWT_SECRET`（可选，未设置时使用源码内占位值，仅适合本地演示）
- 环境变量 `AI_API_KEY`（可选，智谱开放平台密钥；不配置时 AI 接口自动走本地兜底文本）

## 环境变量配置（Windows）

首次运行前，在同一个命令行窗口设置：

```bat
set MYSQL_PASSWORD=你的MySQL root密码
set JWT_SECRET=任意至少32位字符串
set AI_API_KEY=你的智谱APIKey
```

PowerShell：

```powershell
$env:MYSQL_PASSWORD="你的MySQL root密码"
$env:JWT_SECRET="任意至少32位字符串"
$env:AI_API_KEY="你的智谱APIKey"
```

`AI_API_KEY` 可以省略；`start-demo.bat` 检测不到 `MYSQL_PASSWORD` 时会提示输入，无需手动 set。

## 快速启动

### 方式一：一键演示脚本（推荐）

1. 先准备好数据库（首次运行执行一次）：

```bash
# 1) 创建库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS smart_novel_pro DEFAULT CHARSET utf8mb4;"

# 2) 解压仓库内的压缩备份（约 40 MB -> 108 MB）
#    Git Bash / Linux / macOS：
gzip -dc database/novel_init.sql.gz > database/novel_init.sql
#    Windows 无 gzip 时，用 PowerShell 解压：
#    powershell -Command "$i=[IO.File]::OpenRead('database
ovel_init.sql.gz');$o=[IO.File]::Create('database
ovel_init.sql');$g=New-Object IO.Compression.GzipStream($i,[IO.Compression.CompressionMode]::Decompress);$g.CopyTo($o);$g.Close();$o.Close();$i.Close()"

# 3) 导入
mysql -uroot -p --default-character-set=utf8mb4 smart_novel_pro < database/novel_init.sql
```

> `database/novel_init.sql` 是解压后的临时文件，已被 `.gitignore` 忽略，无需提交。

2. 双击项目根目录的 `start-demo.bat`：

- 自动启动后端 Spring Boot（`http://localhost:8888`）与前端 Vite（`http://localhost:5173`）
- 自动等待两个端口就绪并打开浏览器
- 前端缺少 `node_modules` 时会自动执行 `npm install`
- 关闭本窗口不会停止服务，需关闭弹出的 `SmartNovel-Backend` / `SmartNovel-Frontend` 命令行窗口

前置条件：JDK 21、Node.js 18+、MySQL 8 已安装，并已设置 `MYSQL_PASSWORD`（`start-demo.bat` 会提示输入）；可选配置 `AI_API_KEY`（不配置时 AI 接口自动走本地兜底文本）。

### 方式二：手动启动

```bash
# 1. 后端
cd backend
set JAVA_HOME=<你的 JDK 21 安装目录>    # Windows
./mvnw spring-boot:run                    # 或 ./mvnw -DskipTests package && java -jar target/*.jar
# 后端服务地址：http://localhost:8888
# 健康检查：http://localhost:8888/actuator/health

# 2. 前端
cd frontend
npm install
npm run dev
# 前端服务地址：http://localhost:5173
```

## 数据库

- 库名：`smart_novel_pro`（配置项见 `backend/src/main/resources/application.yml`；账号 `root`，密码通过 `MYSQL_PASSWORD` 环境变量提供）
- **最新全量备份**：
  - `database/novel_init.sql`：完整表结构 + 最新演示数据（26 张表，约 108 MB，未入库，见 `.gitignore`）
  - `database/novel_init.sql.gz`：同一份备份的压缩版（约 40 MB，随仓库提交）
- 旧版结构脚本：`backend/src/main/resources/db/schema.sql`（仅表结构/种子数据，不含演示账号）
- 26 张表：小说、章节、正文、用户、作者、评论、社区、书架、阅读记录、分类、标签、公告、横幅、AI 索引、AI 块等
- 备份数据规模：演示用户 2 个（`reader_demo` / `author_demo`）、管理员 1 个（`admin_demo`）、小说 109 本、章节 11,144 条、公告 1 条（封面 `pic_url` 置空，由前端 `<BookCover />` 组件动态渲染）
- 重新导出备份（结构 + 数据）：

```bash
mysqldump -uroot -p --default-character-set=utf8mb4 --single-transaction --routines --triggers smart_novel_pro > database/novel_init.sql
gzip -9 -c database/novel_init.sql > database/novel_init.sql.gz
```

## 演示账号

登录/注册页顶部可切换「读者 / 作家 / 管理者」身份。三个演示账号密码统一为 `Demo@123456`：

| 身份 | 用户名 | 密码 | 登录后跳转 | 说明 |
| --- | --- | --- | --- | --- |
| 读者 READER | `reader_demo` | `Demo@123456` | `/` 读者首页 | 可提交「申请成为作者」 |
| 作家 AUTHOR | `author_demo` | `Demo@123456` | `/author` 作家工作台 | 已生成审核通过的作者档案，可直接创作 |
| 管理者 ADMIN | `admin_demo` | `Demo@123456` | `/admin` 管理后台 | 存储于 `sys_user`，可审核作家入驻 |

补充说明：

- 选择「读者」入口登录时**不校验角色**：任何账号（读者/作家/管理员）密码正确都会进入读者首页。
- 选择「作家」或「管理者」入口时，账号角色不匹配会提示「该账号无对应身份权限」。
- 读者注册零门槛：默认 `role=READER`、状态正常，注册成功即自动登录并跳转首页。

## 核心功能

### 用户端（Front）

- 🏠 **首页**：热门推荐、新书榜、评分榜、收藏榜、个性化推荐（基于协同过滤）、猜你喜欢
- 📖 **小说详情**：基本信息、数据看板（字数/点击/评分/评论/收藏）、章节目录、评分弹窗
- 📚 **章节阅读**：沉浸式阅读器、章节目录下拉导航、阅读进度自动保存（5 秒节流）
- 🤖 **AI 阅读助手**：基于 RAG 的阅读问答（对小说内容进行语义检索和自然语言问答）
- 🔍 **搜索**：关键词搜索 + 分类 + 创作方向 + VIP 筛选
- 📂 **分类浏览**：全部分类 / 男频 / 女频 三栏切换
- 👤 **个人中心**：资料编辑、兴趣标签、密码修改、VIP 升级
- 📚 **书架**：收藏管理、最近阅读
- 💬 **社区**：发帖、回帖、删除（支持权限控制）
- 💬 **评论**：小说评分和评论、评论回复、删除
- ✉️ **私信**：联系人列表 + 在线对话
- 📢 **通知/横幅**：系统通知展示

### 作者端（Author）

- ✍️ **工作台**：作品统计（书籍数/总字数/总阅读量）、AI 写作问答、近期评论预览
- 📚 **作品管理**：创建/编辑/删除小说，分类和标签选择，AI 写作辅助
- 📝 **章节管理**：添加/编辑/删除章节、VIP 设置、编辑时正文完整回显、AI 内容扩写（大模型不可用时本地兜底）
- 💬 **评论管理**：按书籍筛选读者评论、作者回复
- ✉️ **私信**：与读者私信、回复

### 管理员端（Admin）

- 📊 **仪表盘大屏**：5 个核心指标卡片、分类分布饼状图、7 日阅读量柱状图、7 日新增用户柱状图、阅读量与新增用户对比趋势图、AI 数据分析问答助手
- 👥 **用户管理**：搜索、VIP 开关、状态管理（启用/禁用）
- ✍️ **作者审核**：审核通过/驳回作者申请
- 📚 **小说管理**：搜索、审核状态筛选、审核通过/驳回、删除
- 💬 **评论管理**：审核状态筛选、通过/驳回、删除
- 📋 **社区管理**：帖子列表、帖子回复查看、帖子删除、回复删除
- ✍️ **作家入驻审核**：读者申请列表（待审核优先）、通过后自动升级为作家、驳回可填写原因
- 📢 **公告管理**：新增/编辑/删除公告，标题、内容、启用状态
- 🏷️ **标签管理**：CRUD 完整操作
- 📂 **分类管理**：CRUD 完整操作
- 💬 **留言板管理**：留言列表、官方回复、删除

## API 层级架构

```
/api
├── /front/*           # 前台公开接口（GET 全部公开，POST/PUT/DELETE 需认证）
│   ├── /recommend/    # 推荐（hot、new、score、favorited、personal、guess）
│   ├── /novels/       # 小说详情、章节、目录
│   ├── /categories    # 分类
│   ├── /tags          # 标签
│   ├── /search        # 搜索
│   ├── /user/         # 用户资料、书架、评分、VIP、阅读历史
│   ├── /comments/     # 评论
│   ├── /community/    # 社区帖子
│   ├── /messages/     # 私信
│   ├── /message-board # 留言板
│   ├── /auth/         # 登录、注册、OAuth
│   ├── /notices       # 通知
│   ├── /banners       # 横幅
│   └── /ai/           # AI 阅读助手（RAG）
├── /author/*          # 作者接口（需 AUTHOR 或 ADMIN 角色）
│   ├── /books/        # 作品 CRUD
│   ├── /comments/     # 评论管理
│   ├── /messages/     # 私信管理
│   ├── /info          # 作者资料
│   └── /ai/           # AI 写作助手
└── /admin/*           # 管理员接口（需 ADMIN 角色）
    ├── /dashboard     # 仪表盘数据 + AI 分析
    ├── /users/        # 用户管理
    ├── /authors/      # 作者审核
    ├── /books/        # 小说管理
    ├── /comments/     # 评论管理
    ├── /community/    # 社区管理
    ├── /author/       # 作家入驻申请列表与审核
    ├── /notices/      # 公告管理
    ├── /tags/         # 标签管理
    ├── /categories/   # 分类管理
    └── /messages/     # 留言板管理
```

## 安全架构

- **认证**：JWT 无状态令牌（24 小时有效期），登录/注册获取
- **权限模型**：三端隔离（USER / AUTHOR / ADMIN）
- **公开接口**：`GET /api/front/**` 全部放行，其余前端接口需登录
- **作者接口**：`@PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")`
- **管理接口**：`@PreAuthorize("hasRole('ADMIN')")`
- **前端路由守卫**：`meta.requiresAuth` + `meta.role` 拦截未授权页面访问

## AI 能力

| 场景 | 端点 | 说明 |
| --- | --- | --- |
| 读者阅读助手 | `POST /api/front/ai/chat/{bookId}` | RAG 语义检索 + 大模型问答 |
| 作者问答助手 | `POST /api/author/ai/ask` | 提供写作建议和创作灵感 |
| 作者写作助手 | `POST /api/author/ai/write/{bookId}` | 扩写、缩写、续写、润色（`actionType` 缺省为 `expand`，模型不可用时本地兜底） |
| 管理员数据分析 | `POST /api/admin/dashboard/ai-ask` | 平台数据分析问答（Spring AI Tool Calling） |

## 前端特色

- **拟物书封组件（BookCover）**：16 种分类配色、CSS 3D 效果、书脊阴影、纸纹理叠加、金边装饰角、衬线字体、动态标题缩放
- **ECharts 可视化**：饼状图、柱状图、趋势线图、渐变填充
- **AI 聊天 UI**：对话气泡、欢迎引导、快捷提问
- **响应式布局**：管理员仪表盘图表自适应

## 构建与部署

```bash
# 后端编译
cd backend
./mvnw compile -DskipTests

# 前端构建
cd frontend
npm run build
# 产出在 frontend/dist/
```

Vite 开发模式下前端通过 Vite proxy 将 `/api` 请求代理到 `localhost:8888`；生产部署时可将 `dist/` 作为静态资源由 Spring Boot 或 Nginx 提供。
