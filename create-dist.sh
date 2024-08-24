#!/bin/bash

version=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
echo "Creating package for version $version"

base_package_name="SkewTVSP-$version"

echo Running maven...
mvn clean compile package
echo ...finished

if [ ! -d "./dist" ]; then
    mkdir ./dist
fi

cd ./dist

#if exist "SkewTVSP-%version%" rmdir /s /q SkewTVSP-%version%
if [ -d "./$base_package_name" ]; then
    rm -rf "./$base_package_name"
fi
#if exist "temp-dist" rmdir /s /q temp-dist
if [ -d "./temp-dist" ]; then
    rm -r ./temp-dist
fi
mkdir temp-dist
cp ../target/${base_package_name}-jar-with-dependencies.jar ./temp-dist/
cp ../license.txt ./temp-dist/

echo "Running jpackage..."

if [[ $1 == "deb" ]]; then
    jpackage --type deb @../.jpackage-options/general @../.jpackage-options/linux-deb --main-jar ${base_package_name}-jar-with-dependencies.jar -i ./temp-dist
    echo "...finished"
elif [[ $1 == "deb-wsl" ]]; then
    jpackage --type deb @../.jpackage-options/general @../.jpackage-options/linux-deb-wsl --main-jar ${base_package_name}-jar-with-dependencies.jar -i ./temp-dist
    echo "...finished"
else
    jpackage --type app-image @../.jpackage-options/general --main-jar ${base_package_name}-jar-with-dependencies.jar -i ./temp-dist
    echo "...finished"

    cp ../license.txt ./SkewTVSP/

    mv ./SkewTVSP ./$base_package_name

    if [[ $1 == "archive" ]]; then
        echo "Creating archive..."
        tar -czf ${base_package_name}.tar.gz ./${base_package_name}
        rm -rf ./${base_package_name}
        echo "...finished"
    fi
fi

rm -r ./temp-dist
cd ..
