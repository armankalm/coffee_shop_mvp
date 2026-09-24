#!/bin/sh
# Maven Wrapper - delegates to local Maven installation
MVN_PATH="/c/Users/user/.m2/wrapper/dists/apache-maven-3.8.3-bin/5a6n1u8or3307vo2u2jgmkhm0t/apache-maven-3.8.3/bin/mvn"
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.17.10-hotspot"

if [ -f "$MVN_PATH" ]; then
    exec "$MVN_PATH" "$@"
else
    exec mvn "$@"
fi
