@echo off
title OCSMS Launcher
color 0A

echo.
echo  ===================================================
echo    OCSMS - Online College Society Management System
echo    FAST-NUCES Peshawar  ^|  SDA Task 1 ^& 2
echo  ===================================================
echo.

:: ── Set Java 17 ──────────────────────────────────────────────────────────
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

:: ── Set local Maven ───────────────────────────────────────────────────────
set MVN_HOME=C:\Users\M Bilal\Desktop\SDA CODE\apache-maven\apache-maven-3.9.6
set PATH=%MVN_HOME%\bin;%PATH%

echo  Java Home : %JAVA_HOME%
echo  Maven Home: %MVN_HOME%
echo.

:: ── Verify Java ───────────────────────────────────────────────────────────
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo  [ERROR] Java not found at %JAVA_HOME%
    echo  Please ensure Java 17 JDK is installed at that path.
    pause
    exit /b 1
)

:: ── Run the App ───────────────────────────────────────────────────────────
echo  Launching OCSMS...
echo.
mvn javafx:run

if %ERRORLEVEL% neq 0 (
    echo.
    echo  [ERROR] Launch failed. Check the output above for details.
    pause
)
