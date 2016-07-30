/*
 * Copyright (C) 2016 patrick
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
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
        Desktop, Webcam, Image, LabelFile, LabelText
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

    public void setOutputHeight(int value) {
        Node node = document.createAttribute("outputheight");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);
    }

    public void setOutputFramerate(int value) {
        Node node = document.createAttribute("outputframerate");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);

    }

    public void setOutputTarget(Targets.FORMATS value) {
        Node node = document.createAttribute("outputtarget");
        node.setNodeValue(value.name());
        output.getAttributes().setNamedItem(node);

    }

    public void setVideoBitrate(int value) {
        Node node = document.createAttribute("videobitrate");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);
    }

    public void setAudioBitrate(FFMpeg.AudioRate value) {
        Node node = document.createAttribute("audiobitrate");
        node.setNodeValue(value.name());
        output.getAttributes().setNamedItem(node);
    }

    public void setOutputPreset(FFMpeg.Presets value) {
        Node node = document.createAttribute("outputpreset");
        node.setNodeValue(value.name());
        output.getAttributes().setNamedItem(node);
    }

    public void setOutputRTMPServer(String value) {
        Node node = document.createAttribute("rtmpserver");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public void setOutputRTMPKey(String value) {
        Node node = document.createAttribute("rtmpkey");
        node.setNodeValue("" + value);
        output.getAttributes().setNamedItem(node);

    }

    public void setAudioMicrophone(String value) {
        Node node = document.createAttribute("microphone");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public void setAudioSystem(String value) {
        Node node = document.createAttribute("audiosystem");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public void setShortcutsCapture(String value) {
        Node node = document.createAttribute("shortcutcapture");
        node.setNodeValue(value);
        output.getAttributes().setNamedItem(node);

    }

    public void setOutputVideoFolder(File value) {
        Node node = document.createAttribute("outputvideofolder");
        node.setNodeValue(value.getAbsolutePath());
        output.getAttributes().setNamedItem(node);

    }

    public void reset() {
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            root.removeChild(nodes.item(i));
        }
    }

    public void addDesktop(SourceType typeValue, String idValue, int xValue, int yValue, int wValue, int hValue, float alphaValue) {
        String nodeName = "";
        switch (typeValue) {
            case Desktop:
                nodeName = "desktop";
                break;
            case Image:
                nodeName = "image";
                break;
            case LabelFile:
                nodeName = "label";
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
        Node w = document.createAttribute("w");
        Node h = document.createAttribute("h");
        Node id = document.createAttribute("id");
        Node type = document.createAttribute("type");
        Node alpha = document.createAttribute("alpha");
        x.setNodeValue("" + xValue);
        y.setNodeValue("" + yValue);
        w.setNodeValue("" + wValue);
        h.setNodeValue("" + hValue);
        id.setNodeValue("" + idValue);
        alpha.setNodeValue("" + alphaValue);
        switch (typeValue) {
            case LabelFile:
                type.setNodeValue("file");
                break;
            case LabelText:
                type.setNodeValue("text");
                id.setNodeValue("text");
                node.setNodeValue(idValue);
                break;
            default:
                type.setNodeValue("");
                break;
        }
        node.getAttributes().setNamedItem(x);
        node.getAttributes().setNamedItem(y);
        node.getAttributes().setNamedItem(w);
        node.getAttributes().setNamedItem(h);
        node.getAttributes().setNamedItem(id);
        node.getAttributes().setNamedItem(type);
        node.getAttributes().setNamedItem(alpha);
        root.appendChild(node);
    }

    public Source[] getDesktops() {
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
            sources[i] = s;
        }
        return sources;
    }

    public Source[] getWebcams() {
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
            sources[i] = s;
        }
        return sources;
    }

    public Source[] getImages() {
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
            sources[i] = s;
        }
        return sources;
    }

    public Source[] getLabels() {
        NodeList nodes = document.getElementsByTagName("label");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source();
            s.Type = SourceType.LabelFile;
            Node n = nodes.item(i);
            s.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
            s.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
            s.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
            s.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
            s.ID = n.getAttributes().getNamedItem("id").getNodeValue();
            if (n.getAttributes().getNamedItem("type").getNodeValue().equalsIgnoreCase("text")) {
                s.Type = SourceType.LabelText;
                s.ID = n.getTextContent();
            }
            s.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
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

    public void save(File file) throws FileNotFoundException, IOException {
        document.normalizeDocument();
        try (java.io.FileWriter out = new java.io.FileWriter(file)) {
            out.write(document.toString());
        }
    }
}
