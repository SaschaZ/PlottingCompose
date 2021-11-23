#!/bin/bash

echo "applying tag $1"

sed -E 's/projectVersion=[0-9]+\.[0-9]+\.[0-9]+/projectVersion='${1:1}'/g' gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties

sed -E 's/ziegerDevReleasesUsername=/ziegerDevReleasesUsername='$2'/g' gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties

sed -E 's/ziegerDevReleasesPassword=/ziegerDevReleasesPassword='$3'/g' gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties