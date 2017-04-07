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
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author patrick
 */
public class Frames {
    public static enum eList{
        Frame_001("Fancy Purple"),
        Frame_002("Modern Red"),
        Frame_003("Classic Wood"),
	Frame_004("Flare"),
	Frame_005("Dark Fade"),
	Frame_006("Bright Eliptical");

        String name;
        eList(String name){
            this.name = name;
        }
        @Override
        public String toString(){
            return this.name;
        }
    }
    
    public static BufferedImage getImage(eList img) throws IOException{
        URL url = Frames.class.getResource(img.name() + ".png");
        BufferedImage bimg = javax.imageio.ImageIO.read(url);
        return bimg;
    }
}
