// 根据环境变量设置 API 基础 URL
// 在 Vite 项目中，使用 import.meta.env.MODE 或 import.meta.env.PROD 来判断环境
const isProduction = import.meta.env.PROD;

export const API_BASE_URL = isProduction
  ? '/api/ai' // 生产环境（微信云托管）使用相对路径，通过 Nginx 转发
  : 'http://localhost:8102/api/ai'; // 开发环境指向本地后端服务

export default {
  API_BASE_URL
};
