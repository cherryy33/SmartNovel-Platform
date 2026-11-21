@echo off
chcp 65001 >nul
setlocal
title 智阅小说 - 一键演示启动

REM ============================================================
REM  智阅小说平台 一键启动脚本
REM  双击运行：自动拉起后端(8888) + 前端(5173)，并打开浏览器
REM  前置条件：已安装 JDK 21、Node.js 18+、MySQL 8，并导入 database/novel_init.sql.gz
REM  可选：设置 AI_API_KEY 环境变量以启用在线 AI（不设置则使用本地兜底文本）
REM ============================================================

set "BACKEND_DIR=%~dp0backend"
set "FRONTEND_DIR=%~dp0frontend"

REM ---- JDK：优先使用 JAVA_HOME，其次 PATH 中的 java ----
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%in\java.exe" set "JAVA_CMD=%JAVA_HOME%in\java.exe"

REM ---- 数据库口令：未设置时提示输入（不写入任何文件） ----
if not defined MYSQL_PASSWORD (
    set /p MYSQL_PASSWORD=请输入本机 MySQL root 密码（无密码直接回车）: 
)
if not defined JWT_SECRET set "JWT_SECRET=please-change-this-jwt-secret-32bytes!"

echo ============================================
echo   智阅小说平台 - 演示环境启动
echo ============================================
echo.

REM ---- 1. 启动后端 ----
echo [1/3] 启动后端服务 (http://localhost:8888) ...
set "BACKEND_JAR="
if exist "%~dp0backend\target\smart-novel-backend-1.0.0.jar" (
    set "BACKEND_JAR=%~dp0backend\target\smart-novel-backend-1.0.0.jar"
    start "SmartNovel-Backend" cmd /k ""%JAVA_CMD%" -jar "%BACKEND_JAR%""
) else (
    echo     未找到已打包的 jar，使用 Maven 启动...
    start "SmartNovel-Backend" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run"
)

REM ---- 等待后端端口就绪（最多 120 秒） ----
echo     等待后端启动...
set /a WAIT=0
:wait_backend
timeout /t 3 /nobreak >nul
set /a WAIT+=3
powershell -NoProfile -Command "try{(New-Object Net.Sockets.TcpClient).Connect('127.0.0.1',8888);exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 goto backend_ready
if %WAIT% LSS 120 goto wait_backend
echo     [警告] 后端 120 秒内未就绪，请查看后端窗口日志。
goto start_frontend

:backend_ready
echo     后端已就绪 (端口 8888)。

:start_frontend
REM ---- 2. 启动前端 ----
echo [2/3] 启动前端服务 (http://localhost:5173) ...
if not exist "%~dp0frontend\node_modules" (
    echo     首次运行，安装前端依赖...
    pushd "%~dp0frontend"
    call npm install
    popd
)
start "SmartNovel-Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev -- --host"

REM ---- 等待前端端口就绪（最多 90 秒） ----
set /a WAIT=0
:wait_frontend
timeout /t 3 /nobreak >nul
set /a WAIT+=3
powershell -NoProfile -Command "try{(New-Object Net.Sockets.TcpClient).Connect('127.0.0.1',5173);exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 goto frontend_ready
if %WAIT% LSS 90 goto wait_frontend
echo     [警告] 前端 90 秒内未就绪，请查看前端窗口日志。
goto open_browser

:frontend_ready
echo     前端已就绪 (端口 5173)。

:open_browser
REM ---- 3. 打开浏览器 ----
echo [3/3] 打开浏览器...
start "" "http://localhost:5173"

echo.
echo ============================================
echo   启动完成！演示账号见 README.md
echo   关闭本窗口不会停止服务，请关闭对应
echo   SmartNovel-Backend / SmartNovel-Frontend
echo   命令行窗口来停止服务。
echo ============================================
echo.
pause
