@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "__MVNW_ARG0_NAME__=%~nx0")
@SET %%ENV_VAR%%=
@SETLOCAL

@SET MAVEN_PROJECTBASEDIR=%~dp0
@IF NOT "%MAVEN_BASEDIR%"=="" @SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@ECHO Checking for Maven wrapper jar...
@IF NOT EXIST %WRAPPER_JAR% (
  @ECHO Downloading maven wrapper from %WRAPPER_URL%
  powershell -Command "Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile %WRAPPER_JAR%"
)

@SET MAVEN_OPTS=-Xms256m -Xmx512m

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
  @IF "%%A"=="distributionUrl" @SET DISTRIBUTION_URL=%%B
)

@SET MVN_CMD=mvn
@IF NOT "%JAVA_HOME%"=="" @SET MVN_CMD="%JAVA_HOME%\bin\mvn"

"%JAVA_HOME%\bin\java" -jar %WRAPPER_JAR% %*
@IF ERRORLEVEL 1 GOTO error
@GOTO end

:error
@SET ERROR_CODE=%ERRORLEVEL%
:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%
@EXIT /B %ERROR_CODE%
