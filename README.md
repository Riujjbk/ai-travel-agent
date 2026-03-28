# AI 旅游助手 (AI Travel Assistant)

本项目是一个基于 Spring AI 和 Vue 3 开发的综合性 AI 旅游服务系统。通过集成多种智能代理（Agents）和强大的工具链，为用户提供行程规划、旅游日记生成、资源搜索及下载等一站式旅游伴侣体验。

## 🌟 核心功能

### 1. 智能代理 (Agents)

- **旅游日记助手 (JournalAgent)**：将用户零散的文字、图片描述转化为具有故事感的温馨旅游日记。支持多轮引导式对话，帮助用户找回记忆细节。
- **全能助手 (UIManus)**：通用型 AI 智能体，具备任务拆解和多工具自动调度能力，能处理复杂的综合性请求。
- **旅游规划助手 (TravelApp)**：基于 RAG（检索增强生成）技术，结合本地知识库为用户提供精准的旅游攻略。

### 2. 强大工具链 (Tools)

- **联网搜索与抓取**：集成 WebmySearch 和 WebScraping，支持实时获取最新旅游资讯。
- **文件与 PDF 操作**：
  - **PDF 生成**：支持将旅游日记或攻略生成美观的 PDF 文件（含图片）。
  - **文件操作**：支持本地文件的读写与管理。
  - **资源下载**：支持从网络 URL 直接下载资源到服务器。
- **系统集成**：支持终端命令执行（TerminalOperationTool）和实时天气查询。

### 3. 安全文件下载系统

- 实现了完善的文件下载与预览控制器。
- **脱敏处理**：自动隐藏服务器内部绝对路径，前端仅展示 `/api/download` 安全链接。
- **文件名兼容**：支持中文文件名编码，解决跨浏览器下载乱码问题。

## 🛠 技术栈

### 后端 (Java)

- **框架**：Spring Boot 3.x, Spring AI
- **AI 模型**：阿里通义千问 (DashScope)
- **PDF 处理**：iText 7
- **工具库**：Hutool, Lombok
- **向量数据库**：PGVector (支持 RAG)

### 前端 (Vue 3)

- **框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **Markdown 渲染**：Markdown-it (支持链接自动识别)
- **样式**：原生 CSS 模块化设计

## 📁 项目结构

```text
ai-lvyou/
├── ai-agent-frontend/        # 前端 Vue 3 项目
├── ai-image-search-mcp/      # MCP 图像搜索服务
├── src/
│   └── main/
│       ├── java/com/ui/ailvyou/
│       │   ├── agent/        # 智能体逻辑 (JournalAgent, UIManus 等)
│       │   ├── controller/   # API 接口 (AI 对话, 文件下载)
│       │   ├── tools/        # AI 工具实现 (PDF, 搜索, 文件等)
│       │   ├── util/         # 通用工具类 (FileDownloadHelper)
│       │   └── rag/          # RAG 检索增强实现
│       └── resources/
│           └── application.yml # 配置文件 (API Key, 数据库配置)
└── tmp/                      # 服务器临时存储目录 (PDF, 下载的文件)
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- PostgreSQL (带 pgvector 插件，如需使用 RAG)

### 后端启动

1. 在 `src/main/resources/application.yml` 中配置您的 `dashscope` API Key。
2. 运行 `AiLvyouApplication.java`。

### 前端启动

1. 进入 `ai-agent-frontend` 目录。
2. 执行 `npm install` 安装依赖。
3. 执行 `npm run dev` 启动开发服务器。

##  最近更新

- 优化了 Agent 的提示词引导，现在 AI 会主动提供 Markdown 格式的下载链接。
- 修复了 `ERR_RESPONSE_HEADERS_MULTIPLE_CONTENT_DISPOSITION` 下载冲突问题。
- 实现了全局路径脱敏，增强了系统安全性。
