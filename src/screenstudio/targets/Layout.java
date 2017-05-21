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
import screenstudio.sources.effects.Effect;
import screenstudio.sources.transitions.Transition;

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
        Desktop, Webcam, Image, LabelText, Video, Stream, Frame, Custom
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

    public void setBackgroundMusic(File bgMusic) {
        Node node = document.createAttribute("backgroundmusic");
        if (bgMusic == null) {
            node.setNodeValue("");
        } else {
            node.setNodeValue(bgMusic.getAbsolutePath());
        }
        settings.getAttributes().setNamedItem(node);
    }

    public File getBackgroundMusic() {
        File retValue = null;
        if (settings.getAttributes().getNamedItem("backgroundmusic") != null) {
            String f = settings.getAttributes().getNamedItem("backgroundmusic").getNodeValue();
            retValue = new File(f);
            if (f.length() == 0 || !retValue.exists()) {
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

    public void addSource(Source source) {
        String nodeName = "";
        switch (source.getType()) {
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
            case Frame:
                nodeName = "frame";
                break;
            case Custom:
                nodeName = "custom";
                break;
        }
        Node node = document.createElement(nodeName);
        Node capx = document.createAttribute("capturex");
        Node capy = document.createAttribute("capturey");
        
        //This is for the layouts
        
        for (Source.View v : source.getViews()) {
            Node nodeView = document.createElement("view");
            
            Node viewName = document.createAttribute("name");
            Node x = document.createAttribute("x");
            Node y = document.createAttribute("y");
            Node w = document.createAttribute("w");
            Node h = document.createAttribute("h");
            Node alpha = document.createAttribute("alpha");
            Node order = document.createAttribute("order");
            Node remoteDisplay = document.createAttribute("display");

            viewName.setNodeValue(v.ViewName);
            x.setNodeValue("" + v.X);
            y.setNodeValue("" + v.Y);
            w.setNodeValue("" + v.Width);
            h.setNodeValue("" + v.Height);
            alpha.setNodeValue("" + v.Alpha);
            order.setNodeValue("" + v.Order);
            remoteDisplay.setNodeValue("" + v.remoteDisplay);

            nodeView.getAttributes().setNamedItem(x);
            nodeView.getAttributes().setNamedItem(y);
            nodeView.getAttributes().setNamedItem(w);
            nodeView.getAttributes().setNamedItem(h);
            nodeView.getAttributes().setNamedItem(alpha);
            nodeView.getAttributes().setNamedItem(order);
            nodeView.getAttributes().setNamedItem(remoteDisplay);
            nodeView.getAttributes().setNamedItem(viewName);
            node.appendChild(nodeView);
        }

        Node id = document.createAttribute("id");
        Node type = document.createAttribute("type");
        Node foreg = document.createAttribute("fg");
        Node backg = document.createAttribute("bg");
        Node fontg = document.createAttribute("font");
        Node timeStart = document.createAttribute("start");
        Node timeEnd = document.createAttribute("end");
        Node transitionStart = document.createAttribute("transstart");
        Node transitionStop = document.createAttribute("transstop");
        Node effectFilter = document.createAttribute("effect");
        Node fontSize = document.createAttribute("fontsize");
        Node backgroundAreaColor = document.createAttribute("bgAreaColor");

        capx.setNodeValue("" + source.getCaptureX());
        capy.setNodeValue("" + source.getCaptureY());

        id.setNodeValue("" + source.getID());
        foreg.setNodeValue("" + source.getForegroundColor());
        backg.setNodeValue("" + source.getBackgroundColor());
        fontg.setNodeValue(source.getFontName());
        timeStart.setNodeValue(source.getStartTime() + "");
        timeEnd.setNodeValue(source.getEndTime() + "");
        transitionStart.setNodeValue(source.getTransitionStart().name());
        transitionStop.setNodeValue(source.getTransitionStop().name());
        effectFilter.setNodeValue(source.getEffect().name());
        switch (source.getType()) {
            case LabelText:
                type.setNodeValue("text");
                break;
            default:
                type.setNodeValue("");
                break;
        }
        fontSize.setNodeValue(""+source.getFontSize());
        backgroundAreaColor.setNodeValue(""+source.getBackgroundAreaColor());
        
        node.getAttributes().setNamedItem(capx);
        node.getAttributes().setNamedItem(capy);
        node.getAttributes().setNamedItem(id);
        node.getAttributes().setNamedItem(type);
        node.getAttributes().setNamedItem(foreg);
        node.getAttributes().setNamedItem(backg);
        node.getAttributes().setNamedItem(fontg);
        node.getAttributes().setNamedItem(timeStart);
        node.getAttributes().setNamedItem(timeEnd);
        node.getAttributes().setNamedItem(transitionStart);
        node.getAttributes().setNamedItem(transitionStop);
        node.getAttributes().setNamedItem(effectFilter);
        node.getAttributes().setNamedItem(fontSize);
        node.getAttributes().setNamedItem(backgroundAreaColor);
        root.appendChild(node);
    }

    private Source[] getDesktops() {
        NodeList nodes = document.getElementsByTagName("desktop");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            Node n = nodes.item(i);
            s.setType(SourceType.Desktop);

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());
            if (n.getAttributes().getNamedItem("capturex") != null) {
                s.setCaptureX((int) new Integer(n.getAttributes().getNamedItem("capturex").getNodeValue()));
                s.setCaptureY((int) new Integer(n.getAttributes().getNamedItem("capturey").getNodeValue()));
            } else {
                s.setCaptureX(0);
                s.setCaptureY(0);
            }
            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
            }
            
            sources[i] = s;
        }
        return sources;
    }
    private Source[] getCustoms() {
        NodeList nodes = document.getElementsByTagName("custom");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            Node n = nodes.item(i);
            s.setType(SourceType.Custom);

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());
            if (n.getAttributes().getNamedItem("capturex") != null) {
                s.setCaptureX((int) new Integer(n.getAttributes().getNamedItem("capturex").getNodeValue()));
                s.setCaptureY((int) new Integer(n.getAttributes().getNamedItem("capturey").getNodeValue()));
            } else {
                s.setCaptureX(0);
                s.setCaptureY(0);
            }
            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
            }
            sources[i] = s;
        }
        return sources;
    }

    private Source[] getWebcams() {
        NodeList nodes = document.getElementsByTagName("webcam");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            s.setType(SourceType.Webcam);
            Node n = nodes.item(i);
            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            sources[i] = s;
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
            }
        }
        return sources;
    }

    public ArrayList<Source> getSources() {
        ArrayList<Source> list = new ArrayList<>();
        list.addAll(Arrays.asList(getImages()));
        list.addAll(Arrays.asList(getWebcams()));
        list.addAll(Arrays.asList(getDesktops()));
        list.addAll(Arrays.asList(getLabels()));
        list.addAll(Arrays.asList(getFrames()));
        list.sort((Source o1, Source o2) -> o1.getViews().get(o1.getCurrentViewIndex()).Order - o2.getViews().get(o2.getCurrentViewIndex()).Order);
        return list;
    }

    private void loadView(Source s, Node n) {
        Source.View view = new Source.View();
        s.getViews().add(view);
        s.setCurrentViewIndex(0);
        view.X = new Integer(n.getAttributes().getNamedItem("x").getNodeValue());
        view.Y = new Integer(n.getAttributes().getNamedItem("y").getNodeValue());
        view.Width = new Integer(n.getAttributes().getNamedItem("w").getNodeValue());
        view.Height = new Integer(n.getAttributes().getNamedItem("h").getNodeValue());
        view.Alpha = new Float(n.getAttributes().getNamedItem("alpha").getNodeValue());
        view.Order = new Integer(n.getAttributes().getNamedItem("order").getNodeValue());
        if (n.getAttributes().getNamedItem("display") != null) {
            view.remoteDisplay = Boolean.parseBoolean(n.getAttributes().getNamedItem("display").getNodeValue());
        } else {
            view.remoteDisplay = true;
        }
    }

    private Source[] getImages() {
        NodeList nodes = document.getElementsByTagName("image");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            s.setType(SourceType.Image);
            Node n = nodes.item(i);
            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
            }
            sources[i] = s;
        }
        return sources;
    }

    private Source[] getFrames() {
        NodeList nodes = document.getElementsByTagName("frame");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            s.setType(SourceType.Frame);
            Node n = nodes.item(i);
            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
            }
            sources[i] = s;
        }
        return sources;
    }

    private Source[] getLabels() {
        NodeList nodes = document.getElementsByTagName("label");
        Source[] sources = new Source[nodes.getLength()];
        for (int i = 0; i < sources.length; i++) {
            Source s = new Source(0);
            s.setType(SourceType.LabelText);
            Node n = nodes.item(i);
            s.setID(n.getAttributes().getNamedItem("id").getNodeValue());

            if (n.getAttributes().getNamedItem("x") != null) {
                loadView(s, n);
            } else {
                // Load multiple views...
                for (int j = 0;j < n.getChildNodes().getLength();j++){
                    Node view = n.getChildNodes().item(j);
                    if (view.getNodeName().equals("view")){
                        loadView(s, view);
                    }
                }
            }

            // IF is required since not available in version 3.0.0
            if (n.getAttributes().getNamedItem("fg") != null) {
                s.setForegroundColor((int) new Integer(n.getAttributes().getNamedItem("fg").getNodeValue()));
                s.setBackgroundColor((int) new Integer(n.getAttributes().getNamedItem("bg").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("font") != null) {
                s.setFontName(n.getAttributes().getNamedItem("font").getNodeValue());
            }
            if (n.getAttributes().getNamedItem("start") != null) {
                s.setStartTime(Long.parseLong(n.getAttributes().getNamedItem("start").getNodeValue()));
                s.setEndTime(Long.parseLong(n.getAttributes().getNamedItem("end").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("transstart") != null) {
                s.setTransitionStart(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstart").getNodeValue()));
                s.setTransitionStop(Transition.NAMES.valueOf(n.getAttributes().getNamedItem("transstop").getNodeValue()));
            }
            if (n.getAttributes().getNamedItem("effect") != null) {
                s.setEffect(Effect.eEffects.valueOf(n.getAttributes().getNamedItem("effect").getNodeValue()));
            } else {
                s.setEffect(Effect.eEffects.None);
            }
            if (n.getAttributes().getNamedItem("fontsize") != null) {
                s.setFontSize(Integer.parseInt(n.getAttributes().getNamedItem("fontsize").getNodeValue()));
            } else {
                s.setFontSize(20);
            }
            if (n.getAttributes().getNamedItem("bgAreaColor") != null) {
                s.setBackgroundColor(Integer.parseInt(n.getAttributes().getNamedItem("bgAreaColor").getNodeValue()));
            } else {
                s.setBackgroundColor(0);
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
