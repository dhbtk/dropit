$downloads = Invoke-WebRequest 'https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jre' | ConvertFrom-Json | Select binaries

$downloadLink = $downloads.binaries[0].binary_link

Remove-Item windows.zip -ErrorAction Ignore
Invoke-WebRequest -Uri $downloadLink -OutFile windows.zip
Remove-Item windows -Recurse -ErrorAction Ignore
mkdir windows
Expand-Archive windows.zip -DestinationPath windows
cd windows
mv * jre
# https://api.adoptopenjdk.net/v3/binary/latest/15/ga/windows/x64/jre/openj9/normal/adoptopenjdk?project=jdk
