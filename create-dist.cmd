@echo off

call mvn org.apache.maven.plugins:maven-help-plugin:3.1.1:evaluate -Dexpression=project.version -DforceStdout -q > version.tmp
set /P version= < version.tmp
echo Creating package for version %version%

if not exist "target\SkewTVSP-%version%.jar" (
echo Running maven...
call mvn package
echo ...finished
)

if exist "SkewTVSP-%version%" rmdir /s /q SkewTVSP-%version%
jlink --output SkewTVSP-%version% --no-header-files --no-man-pages --compress=2 --strip-debug --module-path "%PATH_TO_FX_MODS%" --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.rmi,java.scripting,java.security.jgss,java.sql,java.xml,jdk.unsupported,jdk.unsupported.desktop
copy target\SkewTVSP-%version%.jar SkewTVSP-%version%\
echo @start %%~dp0bin\javaw -jar %%~dp0SkewTVSP-%version%.jar > SkewTVSP-%version%\SkewTVSP.cmd

if "%1"=="archive" (
  7z a -sdel SkewTVSP-%version%.zip SkewTVSP-%version%
)

del version.tmp
set version=
set mvnversion=

