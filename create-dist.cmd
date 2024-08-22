@echo off

call mvn org.apache.maven.plugins:maven-help-plugin:3.1.1:evaluate -Dexpression=project.version -DforceStdout -q > version.tmp
set /P version= < version.tmp
echo Creating package for version %version%

echo Running maven...
call mvn clean compile package
echo ...finished
)

if not exist "dist" mkdir dist
cd dist

if exist "SkewTVSP-%version%" rmdir /s /q SkewTVSP-%version%
if exist "temp-dist" rmdir /s /q temp-dist
mkdir temp-dist
copy ..\target\SkewTVSP-%version%-jar-with-dependencies.jar temp-dist\

echo Running jpackage...

if "%1"=="installer" (
    jpackage --type exe @..\.jpackage-options\general @..\.jpackage-options\windows-exe --main-jar SkewTVSP-%version%-jar-with-dependencies.jar -i temp-dist
    echo ...finished
) else (
    jpackage --type app-image @..\.jpackage-options\general --main-jar SkewTVSP-%version%-jar-with-dependencies.jar -i temp-dist
    echo ...finished

    copy ..\license.txt SkewTVSP\

    rename SkewTVSP SkewTVSP-%version%

    if "%1"=="archive" (
        7z a -sdel SkewTVSP-%version%.zip SkewTVSP-%version%
    )
)

rem Cleanup

rmdir /s /q temp-dist
cd ..

del version.tmp
set version=
set mvnversion=

