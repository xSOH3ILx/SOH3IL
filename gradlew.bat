@rem SOH3IL Gradle entry point for Windows.
@rem Uses the wrapper jar when committed; otherwise falls back to a system Gradle.
@echo off
setlocal
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

if exist "%WRAPPER_JAR%" (
    java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
    goto :eof
)

echo NOTE: gradle-wrapper.jar is not committed yet; falling back to system gradle. 1>&2
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
    gradle %*
    goto :eof
)

echo ERROR: no Gradle found. Install Gradle 9.5+ or run: gradle wrapper --gradle-version 9.5.0 1>&2
exit /b 1
