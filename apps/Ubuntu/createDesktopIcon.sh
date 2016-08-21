DIR=$(pwd)
echo "[Desktop Entry]">ScreenStudio.desktop
echo "Encoding=UTF-8">>ScreenStudio.desktop
echo "Name=ScreenStudio 3">>ScreenStudio.desktop
echo "Comment=Streaming, made easy!">>ScreenStudio.desktop
echo "Path=$DIR">>ScreenStudio.desktop
echo "Exec=$DIR/ScreenStudio.sh">>ScreenStudio.desktop
echo "Icon=$DIR/logo.png">>ScreenStudio.desktop
echo "Categories=Application;">>ScreenStudio.desktop
echo "Version=@VERSION">>ScreenStudio.desktop
echo "Type=Application">>ScreenStudio.desktop
echo "Terminal=0">>ScreenStudio.desktop
chmod +x ScreenStudio.desktop
mv ScreenStudio.desktop $(xdg-user-dir DESKTOP)/ScreenStudio.desktop


