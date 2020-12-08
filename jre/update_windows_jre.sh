#!/bin/bash

cd "$(dirname "$0")"

download_url=$(curl -L 'https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jre' | jq -r '.binaries[0].binary_link')

curl -L $download_url > windows.zip

cd windows
rm -r ./*
unzip ../windows.zip
mv * jre
