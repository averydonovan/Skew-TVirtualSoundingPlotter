#!/bin/bash

version=`mvn org.apache.maven.plugins:maven-help-plugin:3.1.1:evaluate -Dexpression=project.version -DforceStdout -q`
echo "Creating package for version $version"

if [ ! -f "./target/SkewTVSP-$version.jar" ]; then
	echo "Running maven..."
	mvn clean compile package
	echo "...finished"
fi

if [ -d "./SkewTVSP-$version" ]; then
	rm -rf ./SkewTVSP-$version
fi
jlink --output ./SkewTVSP-$version --no-header-files --no-man-pages --compress=2 --strip-debug --module-path "$PATH_TO_FX_MODS" --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.rmi,java.scripting,java.security.jgss,java.sql,java.xml,jdk.unsupported,jdk.unsupported.desktop
cp ./target/SkewTVSP-$version.jar ./SkewTVSP-$version/
echo "#!/bin/bash" > ./SkewTVSP-$version/SkewTVSP.sh
echo "\`dirname \"\$0\"\`/bin/java -jar \`dirname \"\$0\"\`/SkewTVSP-$version.jar" >> ./SkewTVSP-$version/SkewTVSP.sh
chmod +x ./SkewTVSP-$version/SkewTVSP.sh

if [ "$1" = 'archive' ]; then
	tar czf SkewTVSP-$version.tar.gz SkewTVSP-$version
	rm -rf ./SkewTVSP-$version
fi

