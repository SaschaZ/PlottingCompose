#!/bin/bash

echo "applying tag $1"

sed -E 's/projectVersion=[0-9]+\.[0-9]+\.[0-9]+/projectVersion='${1:1}'/g' gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties