/*
 * Copyright (C) 2017 patrick
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
 */
package screenstudio.gui.images.frames;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author patrick
 */
public class Frames {
    public static enum eList{
        Frame_001,
        Frame_002,
        Frame_003,
	Frame_004,
	Frame_005,
	Frame_006,
    }
    
    public static BufferedImage getImage(eList img) throws IOException{
        URL url = Frames.class.getResource(img.name() + ".png");
        BufferedImage bimg = javax.imageio.ImageIO.read(url);
        return bimg;
    }
    public static String getDescription(eList img){
        String d = "None";
        switch (img){
            case Frame_001:
                d = "Fancy Purple";
                break;
            case Frame_002:
                d = "Modern Red";
                break;
            case Frame_003:
                d = "Classic Wood";
		break;
	    case Frame_004:
		d = "Flare";
                break;
	    case Frame_005:
		d = "Dark Fade";
		break;
	    case Frame_006:
		d = "Bright Eliptical";
		break;
        }
        return d;
    }
}
