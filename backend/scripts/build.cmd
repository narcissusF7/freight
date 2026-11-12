@echo off
setlocal
rem Build with the Maven bundled in IntelliJ IDEA (PATH has no mvn on this machine).
rem Override with IDEA_MAVEN_HOME if your IDEA is installed elsewhere.
set "IDEA_MAVEN=D:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3"
if defined IDEA_MAVEN_HOME set "IDEA_MAVEN=%IDEA_MAVEN_HOME%"
set "MVN=%IDEA_MAVEN%\bin\mvn.cmd"

if not exist "%MVN%" (
  echo [build] IDEA Maven not found: "%MVN%"
  echo [build] Set IDEA_MAVEN_HOME to your IDEA plugins\maven\lib\maven3 directory.
  exit /b 1
)

echo [build] using "%MVN%"
rem Example: build.cmd clean compile   /   build.cmd -DskipTests package
call "%MVN%" -f "%~dp0..\pom.xml" %*
