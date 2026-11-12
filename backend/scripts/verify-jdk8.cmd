@echo off
setlocal
rem Local JDK 8 capability probe for CMD. No Maven / Spring / network required.
cd /d "%~dp0.."
if not exist target mkdir target
set "SRC=target\jdk8-sources.txt"
set "OUT=target\jdk8-probe-classes"
if not exist "%OUT%" mkdir "%OUT%"

> "%SRC%" (
  for /r "freight-infra\src\main\java\com\lukachangtong\freight\infra\client" %%f in (*.java) do @echo "%%f"
)

javac -source 8 -target 8 -encoding UTF-8 -Xlint:-options -d "%OUT%" @"%SRC%"
if errorlevel 1 goto :fail
java -cp "%OUT%" com.lukachangtong.freight.infra.client.mock.support.Jdk8CapabilityProbe
if errorlevel 1 goto :fail
goto :eof

:fail
echo [verify] FAILED
exit /b 1
