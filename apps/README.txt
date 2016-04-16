ScreenStudio 2
-----------------------------------------------------------------
INSTALLATION:

- Copy the content of the archive into any folder
- Execute the script "createDesktopIcon.sh" to create a shortcut on your desktop
- Make sure that Java JRE 8 is installed
- Make sure that "FFMPEG" is installed (ffmpeg version 2.7.6-0ubuntu0.15.10.1)
- Make sure that Pulseaudio is installed as it is required for audio mixing.

To install dependencies on Ubuntu:
sudo apt-get install ffmpeg openjdk-8-jre

NOTE:  ScreenStudio is relying heavily on FFMpeg provided with Ubuntu 16.04.  Any other distros or custom builds of FFMPEG
may not be supported.  If ScreenStudio does not work on your distro (or custom build), download the source code of 
ScreenStudio to adjust the proper command to use with FFMpeg.

NOTE2:  An OSX version will be coming soon for version 2.  Use version 1.5.x for Mac OSX...

Source code is available at: http://screenstudio.crombz.com

-----------------------------------------------------------------
USAGE:

In your installation folder, you can find two sub-folders
- Overlays: This will contains any content panel that you wish to display.
- Capture: This is where your local recording will be saved

- TARGETS
You need to select the output format by selecting a TARGET.  For local recording, select any file format.  For live streaming,
select any service like Twitch or Youtube.

For live streaming, you will have to select a server and enter your magic key in the opening dialog.

Select the desired profile.  By default, "DISPLAY" is selected, meaning that the output will match the display size of your computer.  Other supported formats are: 240p, 360p, 480p, 720p, 1080p.

ScreenStudio will calculate to output format size based on your selection so that the width will be adjusted to the selected
height.

Your current configuration is displayed in the "Configuration" display.

- SOURCES

The SOURCES will let you select the different sources to use for a local recording or a live streaming:
- Display: If you have more than 1 display, select the proper display to use
- Webcam: Select the proper webcam to use.  "None" will not use any webcam.
- Microphone:  Select the microphone (audio input) to use.  
- Internal:  Select the internal audio monitor to use.  This is the input that will be used to capture the audio from your computer
- Webcam Title: A text that will be displayed at the top of your webcam.
- Debug mode:  When activated, the console will show all the output from FFMpeg.

- PANEL

This is where you configure your side panel.
- Panel:  Select the panel overlay you wish to use.  This is a list of the files found in the "Overlays" sub-folder
- Panel Width:  Set the with of the panel overlay to use.  It should match the width of your webcam format for best results
- Duration:  Enter your expected streaming duration.  This is used with Overlays to display the remaining time.

- PANEL AND OVERLAYS

Supported file format are : 
- *.txt : Basic text file 
- *.html: Simple HTML file (HTML4) are supported.   
- *.url:  A URL to be used to display content instead of a local HTML file.

HTML file are the best of showing content in your recording/live streaming.  Use any text editor to create your own overlay
using basic HTML tags.  Javascript is not supported.  For more dynamic content, your can use a URL file to load content
from a web server.

The HTML rendering do support some basic styles CSS but do not expect a full HTML5 support.  Here's an example:

<html>
<body bgcolor=white color=black with=320 height=800>
<H1>ScreenStudio is amazing!</H1>
<font color=red>Download now!</font>
</body
</html>

In the "BODY" tag, set the background and foreground color.  To ensure that the overlay will use all the available space, 
set the width and height also in the body tag.

See http://www.w3schools.com/tags/ for a list of tags to use...

- PANEL CONTENT TAGS

In the text/html content, some tags are supported to udate the text content with values like the current date and time.

	@CURRENTDATE (Current date)
	@CURRENTTIME (Current time)
 	@RECORDINGTIME (Recording time in minutes)
	@STARTTIME (Time when the recording started)
	@REMAININGTIME (Time remaining in minutes)
	@TEXT (Custom text from the text entry in the Panel tab...)
        @COMMAND (Custom text from a command output...)


- AUDIO INPUT:
ScreenStudio is relying on Pulseaudio for audio recording.  When two audio input are selected (Mic+Internal), ScreenStudio will 
add a virtual audio input that will mix both Mic and Internal audio input.  This virtual audio input will only exists while
ScreenStudio is capturing and will be remove once the capture is completed.

To adjust the audio levels, use the default audio mixer or install "pavucontrol" for more options.

-----------------------------------------------------------------
CONTACT:

Main website: http://screenstudio.crombz.com
Twitter: http://twitter.com/patrickballeux
G+ Community: https://plus.google.com/communities/107164189448403990139

Keep in mind that ScreenStudio is free and that I work on this project in my spare time.

Have fun!

Patrick
