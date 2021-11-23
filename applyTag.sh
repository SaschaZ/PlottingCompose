#!/bin/bash

echo "applying tag $1"

sed -E 's/projectVersion=[0-9]+\.[0-9]+\.[0-9]+/projectVersion='${1:1}'/g' gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties

sed -E 's/username = ""/username = "'$2'"/g' build.gradle.kts > build.gradle.kts2
mv build.gradle.kts2 build.gradle.kts

sed -E 's/password = ""/password = "'$3'"/g' build.gradle.kts > build.gradle.kts2
mv build.gradle.kts2 build.gradle.kts