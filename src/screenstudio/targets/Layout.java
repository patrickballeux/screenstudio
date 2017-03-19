/*
 * Copyright (C) 2016 Patrick Balleux (Twitter: @patrickballeux)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This is an XML FORMAT to keep the settings for ScreenStudio
 * <screenstudio>
 * <webcam/>
 * <image/>
 * <label/>
 * <audio/>
 * <setting/>
 * <output/>
 * </screenstudio>
 */
package screenstudio.targets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import screenstudio.encoder.FFMpeg;

/**
 * @author patrick
 */
public class Layout {

    private Document document = null;
    private Node root = null;
    private Node output = null;
    private Node audios = null;
    private Node settings = null;

    public enum SourceType {
        Desktop, Webcam, Image, LabelText, Video, Stream
    }

    public Layout() {
        try {
            this.document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            root = this.document.createElement("screenstudio");
            document.appendChild(root);
            audios = document.createElement("audios");
            root.appendChild(audios);
            output = document.createElement("output");
            root.appendChild(output);
            settings = document.createElement("settings");
            root.appendChild(settings);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Layout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setOutputWith(int value) {
        Node node = document.createAttribute("outputwidth");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);
    }

    public int getOutputWidth() {
        return new Integer(output.getAttributes().getNamedItem("outputwidth").getNodeValue());
    }

    public void setOutputHeight(int value) {
        Node node = document.createAttribute("outputheight");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);
    }

    public int getOutputHeight() {
        return new Integer(output.getAttributes().getNamedItem("outputheight").getNodeValue());
    }

    public void setOutputFramerate(int value) {
        Node node = document.createAttribute("outputframerate");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);

    }

    public int getOutputFramerate() {
        return new Integer(output.getAttributes().getNamedItem("outputframerate").getNodeValue());
    }

    public void setOutputTarget(FFMpeg.FORMATS value) {
        Node node = document.createAttribute("outputtarget");
        node.setNodeValue(value.name());
        output.getAttributes().setNamedItem(node);

    }

    public FFMpeg.FORMATS getOutputTarget() {
        return FFMpeg.FORMATS.valueOf(output.getAttributes().getNamedItem("outputtarget").getNodeValue());
    }

    public void setVideoBitrate(int value) {
        Node node = document.createAttribute("videobitrate");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);
    }
    
    public int getVideoBitrate() {
        return new Integer(output.getAttributes().getNamedItem("videobitrate").getNodeValue());
    }

    public void setAudioBitrate(FFMpeg.AudioRate value) {
        Node node = document.createAttribute("audiobitrate");
        node.setNodeValue(value.name());
        audios.getAttributes().setNamedItem(node);
    }

    public FFMpeg.AudioRate getAudioBitrate() {
        return FFMpeg.AudioRate.valueOf(audios.getAttributes().getNamedItem("audiobitrate").getNodeValue());
    }

    public void setOutputPreset(FFMpeg.Presets value) {
        Node node = document.createAttribute("outputpreset");
        node.setNodeValue(value.name());
        output.getAttributes().setNamedItem(node);
    }

    public FFMpeg.Presets getOutputPreset() {
        return FFMpeg.Presets.valueOf(output.getAttributes().getNamedItem("outputpreset").getNodeValue());
    }

    public void setOutputRTMPServer(String value) {
        Node node = document.createAttribute("rtmpserver");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public String getOutputRTMPServer() {
        return output.getAttributes().getNamedItem("rtmpserver").getNodeValue();
    }

    public void setOutputRTMPKey(String value) {
        Node node = document.createAttribute("rtmpkey");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);

    }

    public String getOutputRTMPKey() {
        return output.getAttributes().getNamedItem("rtmpkey").getNodeValue();
    }

    public void setAudioMicrophone(String value) {
        Node node = document.createAttribute("microphone");
        node.setNodeValue(value);
        audios.getAttributes().setNamedItem(node);

    }

    public String getAudioMicrophone() {
        return audios.getAttributes().getNamedItem("microphone").getNodeValue();
    }

    public void setAudioSystem(String value) {
        Node node = document.createAttribute("audiosystem");
        node.setNodeValue(value);
        audios.getAttributes().setNamedItem(node);
    }

    public String getAudioSystem() {
        return audios.getAttributes().getNamedItem("audiosystem").getNodeValue();
    }

    public void setShortcutsCapture(String value) {
        Node node = document.createAttribute("shortcutcapture");
        node.setNodeValue(value);
        settings.getAttributes().setNamedItem(node);
    }

    public String getShortcutCapture() {
        return settings.getAttributes().getNamedItem("shortcutcapture").getNodeValue();
    }

    public void setOutputVideoFolder(String value) {
        Node node = document.createAttribute("outputvideofolder");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public String getOutputVideoFolder() {
        return output.getAttributes().getNamedItem("outputvideofolder").getNodeValue();
    }

    public void setBackgroundMusic(File bgMusic){
        Node node = document.createAttribute("backgroundmusic");
        if (bgMusic == null){
            node.setNodeValue("");
        } else {
            node.setNodeValue(bgMusic.getAbsolutePath());
        }
        settings.getAttributes().setNamedItem(node);
    }
    public File getBackgroundMusic(){
        File retValue = null;
        if (settings.getAttributes().getNamedItem("backgroundmusic") != null){
            String f = settings.getAttributes().getNamedItem("backgroundmusic").getNodeValue();
            retValue = new File(f);
            if (f.length() == 0 || !retValue.exists() ){
                retValue = null;
            }
        }
        return retValue;
    }
    public void reset() {
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            root.removeChild(nodes.item(i));
        }
    }

    public void addSource(SourceType typeValue, String idValue, int xValue, int yValue, int wValue, int hValue, float alphaValue, int orderValue, int fg, int bg, String font, int captureX, int captureY, long start, long end, String transStart, String transStop,boolean display, String effect) {
        String nodeName = "";
        switch (typeValue) {
            case Desktop:
                nodeName = "desktop";
                break;
            case Image:
                nodeName = "image";
                break;
            case LabelText:
                nodeName = "label";
                break;
            case Webcam:
                nodeName = "webcam";
                break;
        }
        Node node = document.createElement(nodeName);
        Node x = document.createAttribute("x");
        Node y = document.createAttribute("y");
        Node capx = document.createAttribute("capturex");
        Node capy = document.createAttribute("capturey");
        Node w = document.createAttribute("w");
        Node h = document.createAttribute("h");
        Node id = document.createAttribute("id");
        Node type = document.createAttribute("type");
        Node alpha = document.createAttribute("alpha");
        Node order = document.createAttribute("order");
        Node foreg = document.createAttribute("fg");
        Node backg = document.createAttribute("bg");
        Node fontg = document.createAttribute("font");
        Node timeStart = document.createAttribute("start");
        Node timeEnd = document.createAttribute("end");
        Node transitionStart = document.createAttribute("transstart");
        Node transitionStop = document.createAttribute("transstop");
        Node remoteDisplay = document.createAttribute("display");
        Node effectFilter = document.createAttribute("effect");
        x.setNodeValue("" + xValue);
        y.setNodeValue("" + yValue);
        capx.setNodeValue("" + captureX);
        capy.setNodeValue("" + captureY);
        w.setNodeValue("" + wValue);
        h.setNodeValue("" + hValue);
        id.setNodeValue("" + idValue);
        alpha.setNodeValue("" + alphaValue);
        order.setNodeValue("" + orderValue);
        foreg.setNodeValue("" + fg);
        backg.setNodeValue("" + bg);
        fontg.setNodeValue(font);
        timeStart.setNodeValue(start+"");
        timeEnd.setNodeValue(end+"");
        transitionStart.setNodeValue(transStart);
        transitionStop.setNodeValue(transStop);
        remoteDisplay.setNodeValue("" + display);
        effectFilter.setNodeValue(effect);
        switch (typeValue) {
            case LabelText:
                type.setNodeValue("text");
                break;
            default:
                type.setNodeValue("");
                break;
        }
        node.getAttributes().setNamedItem(x);
        node.getAttributes().setNamedItem(y);
        node.getAttributes().setNamedItem(capx);
        node.getAttributes().setNamedItem(capy);
        node.getAttributes().setNamedItem(w);
        node.getAttributes().setNamedItem(h);
        node.getAttributes().setNamedItem(id);
        node.getAttributes().setNamedItem(type);
        node.getAttributes().setNamedItem(alpha);
        node.getAttributes().setNamedItem(order);
        node.getAttributes().setNamedItem(foreg);
        node.getAttributes().setNamedItem(backg);
        node.getAttributes().setNamedItem(fontg);
        node.getAttributes().setNamedItem(timeStart);
        node.getAttributes().setNamedItem(timeEnd);
        node.getAttributes().setNamedItem(transitionStart);
        node.getAttributes().setNamedItem(transitionStop);
        node.getAttributes().setNamedItem(remoteDisplay);
        node.getAttributes().setNamedItem(effectFilter);
        root.appendChild(node);
    }

    private Source[] getDesktops() {
        NodeList nodes = document.getElementsByTagName("desktop");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source();
            Node n = nodes.item(i);
            s.Type = SourceType.Desktop;
            s.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
            s.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
            s.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
            s.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
            s.ID = n.getAttributes().getNamedItem("id").getNodeValue();
            s.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
            s.Order = new Integer(n.getAttributes().getNamedItem("order").getNodeValue());
            if (n.getAttributes().getNamedItem("capturex") != null) {
                s.CaptureX = new Integer(n.getAttributes().getNamedItem("capturex").getNodeValue());
                s.CaptureY = new Integer(n.getAttributes().getNamedItem("capturey").getNodeValue());
            } else {
                s.CaptureX = 0;
                s.CaptureY = 0;
            }
            if (n.getAttributes().getNamedItem("start") != null){
                s.startTime = Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue());
                s.endTime = Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("transstart") != null){
                s.transitionStart = n.getAttributes().getNamedItem("transstart").getNodeValue();
                s.transitionStop = n.getAttributes().getNamedItem("transstop").getNodeValue();
            }
            if (n.getAttributes().getNamedItem("display") != null){
                s.remoteDisplay = Boolean.parseBoolean(n.getAttributes().getNamedItem("display").getNodeValue());
            } else {
                s.remoteDisplay= true;
            }
            if (n.getAttributes().getNamedItem("effect") != null){
                s.effect = n.getAttributes().getNamedItem("effect").getNodeValue();
            } else {
                s.effect= "None";
            }
            sources[i] = s;
        }
        return sources;
    }

    private Source[] getWebcams() {
        NodeList nodes = document.getElementsByTagName("webcam");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source();
            s.Type = SourceType.Webcam;
            Node n = nodes.item(i);
            s.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
            s.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
            s.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
            s.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
            s.ID = n.getAttributes().getNamedItem("id").getNodeValue();
            s.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
            s.Order = new Integer(n.getAttributes().getNamedItem("order").getNodeValue());
            if (n.getAttributes().getNamedItem("start") != null){
                s.startTime = Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue());
                s.endTime = Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("transstart") != null){
                s.transitionStart = n.getAttributes().getNamedItem("transstart").getNodeValue();
                s.transitionStop = n.getAttributes().getNamedItem("transstop").getNodeValue();
            }
            if (n.getAttributes().getNamedItem("display") != null){
                s.remoteDisplay = Boolean.parseBoolean(n.getAttributes().getNamedItem("display").getNodeValue());
            } else {
                s.remoteDisplay= true;
            }
            if (n.getAttributes().getNamedItem("effect") != null){
                s.effect = n.getAttributes().getNamedItem("effect").getNodeValue();
            } else {
                s.effect= "None";
            }
            sources[i] = s;
        }
        return sources;
    }

    public ArrayList<Source> getSources() {
        ArrayList<Source> list = new ArrayList<>();
        list.addAll(Arrays.asList(getImages()));
        list.addAll(Arrays.asList(getWebcams()));
        list.addAll(Arrays.asList(getDesktops()));
        list.addAll(Arrays.asList(getLabels()));
        list.sort((Source o1, Source o2) -> o1.Order - o2.Order);
        return list;
    }

    private Source[] getImages() {
        NodeList nodes = document.getElementsByTagName("image");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source();
            s.Type = SourceType.Image;
            Node n = nodes.item(i);
            s.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
            s.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
            s.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
            s.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
            s.ID = n.getAttributes().getNamedItem("id").getNodeValue();
            s.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
            s.Order = new Integer(n.getAttributes().getNamedItem("order").getNodeValue());
            if (n.getAttributes().getNamedItem("start") != null){
                s.startTime = Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue());
                s.endTime = Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("transstart") != null){
                s.transitionStart = n.getAttributes().getNamedItem("transstart").getNodeValue();
                s.transitionStop = n.getAttributes().getNamedItem("transstop").getNodeValue();
            }
            if (n.getAttributes().getNamedItem("display") != null){
                s.remoteDisplay = Boolean.parseBoolean(n.getAttributes().getNamedItem("display").getNodeValue());
            } else {
                s.remoteDisplay= true;
            }
            if (n.getAttributes().getNamedItem("effect") != null){
                s.effect = n.getAttributes().getNamedItem("effect").getNodeValue();
            } else {
                s.effect= "None";
            }
            sources[i] = s;
        }
        return sources;
    }

    private Source[] getLabels() {
        NodeList nodes = document.getElementsByTagName("label");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source();
            s.Type = SourceType.LabelText;
            Node n = nodes.item(i);
            s.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
            s.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
            s.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
            s.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
            s.ID = n.getAttributes().getNamedItem("id").getNodeValue();
            s.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
            s.Order = new Integer(n.getAttributes().getNamedItem("order").getNodeValue());
            // IF is required since not available in version 3.0.0
            if (n.getAttributes().getNamedItem("fg") != null) {
                s.foregroundColor = new Integer(n.getAttributes().getNamedItem("fg").getNodeValue());
                s.backgroundColor = new Integer(n.getAttributes().getNamedItem("bg").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("font") != null) {
                s.fontName = n.getAttributes().getNamedItem("font").getNodeValue();
            }
            if (n.getAttributes().getNamedItem("start") != null){
                s.startTime = Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue());
                s.endTime = Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("transstart") != null){
                s.transitionStart = n.getAttributes().getNamedItem("transstart").getNodeValue();
                s.transitionStop = n.getAttributes().getNamedItem("transstop").getNodeValue();
            }
            if (n.getAttributes().getNamedItem("display") != null){
                s.remoteDisplay = Boolean.parseBoolean(n.getAttributes().getNamedItem("display").getNodeValue());
            } else {
                s.remoteDisplay= true;
            }
            if (n.getAttributes().getNamedItem("effect") != null){
                s.effect = n.getAttributes().getNamedItem("effect").getNodeValue();
            } else {
                s.effect= "None";
            }
            sources[i] = s;
        }
        return sources;
    }

    public void load(File file) throws IOException, ParserConfigurationException, SAXException {
        document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        root = document.getElementsByTagName("screenstudio").item(0);
        audios = document.getElementsByTagName("audios").item(0);
        output = document.getElementsByTagName("output").item(0);
        settings = document.getElementsByTagName("settings").item(0);
    }

    public void save(File file) throws TransformerConfigurationException, TransformerException {
        document.normalizeDocument();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result fileOutput = new StreamResult(file);
        transformer.transform(new DOMSource(document), fileOutput);
    }
}
