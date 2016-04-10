VERSION=$( grep "MAIN =" src/screenstudio/Version.java | cut -d= -f 2 | cut -d'"' -f 2 )
echo "ScreenStudio - Build a new version..."
read -e -p "Enter new version: " -i "$VERSION" VERSION
sed "s/MAIN = \".*\"/MAIN = \"$VERSION\"/g" src/screenstudio/Version.java>src/screenstudio/Version.java.temp
rm src/screenstudio/Version.java
mv src/screenstudio/Version.java.temp src/screenstudio/Version.java
ant -Dnb.internal.action.name=rebuild clean
rm Capture/*.*
tar -zcvf "../ScreenStudio-OSX-$VERSION-src.tar.gz" .
ant -Dnb.internal.action.name=jar
echo "Building OSX app"
echo "Removing previous build..."
echo "Creating new folder app..."
cp -r apps/OSX/ ./ScreenStudio.app
mkdir ScreenStudio.app/Contents/MacOS/RTMP
mkdir ScreenStudio.app/Contents/MacOS/FFMPEG
mkdir ScreenStudio.app/Contents/MacOS/lib
echo "Copying ScreenStudio archive..."
cp dist/ScreenStudio.jar ScreenStudio.app/Contents/MacOS/ScreenStudio.jar
echo "Copying  files..."
cp apps/README.txt ScreenStudio.app/Contents/MacOS/README.txt
cp RTMP/*.properties ScreenStudio.app/Contents/MacOS/RTMP
cp FFMPEG/osx.properties ScreenStudio.app/Contents/MacOS/FFMPEG
cp FFMPEG/ffmpeg-osx ScreenStudio.app/Contents/MacOS/FFMPEG
cp libs/* ScreenStudio.app/Contents/MacOS/lib
tar -zcvf "../ScreenStudio-OSX-$VERSION-bin.tar.gz" ScreenStudio.app
echo "$VERSION">../osx.last.version
rm -r ScreenStudio.app




