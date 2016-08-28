VERSION=$( grep "MAIN =" src/screenstudio/Version.java | cut -d= -f 2 | cut -d'"' -f 2 )
echo "ScreenStudio - Build a new version..."
#/Applications/NetBeans/NetBeans\ 8.1.app/Contents/Resources/NetBeans/java/ant -Dnb.internal.action.name=rebuild clean
#/Applications/NetBeans/NetBeans\ 8.1.app/Contents/Resources/NetBeans/java/ant -Dnb.internal.action.name=jar
echo "Building OSX app"
echo "Removing previous build..."
echo "Creating new folder app..."
cp -r apps/OSX/ ./ScreenStudio.app
mkdir ScreenStudio.app/Contents/MacOS/FFMPEG
mkdir ScreenStudio.app/Contents/MacOS/lib
echo "Copying ScreenStudio archive..."
cp dist/ScreenStudio.jar ScreenStudio.app/Contents/MacOS/ScreenStudio.jar
echo "Copying  files..."
cp apps/README.txt ScreenStudio.app/Contents/MacOS/README.txt
cp FFMPEG/ffmpeg-osx ScreenStudio.app/Contents/MacOS/FFMPEG
chmod +x ScreenStudio.app/Contents/MacOS/FFMPEG/ffmpeg-osx
chmod +x ScreenStudio.app/Contents/MacOS/launcher
cp libs/* ScreenStudio.app/Contents/MacOS/lib
tar -zcvf "../ScreenStudio-OSX-$VERSION-bin.tar.gz" ScreenStudio.app
echo "$VERSION">../osx.last.version
rm -r ScreenStudio.app




