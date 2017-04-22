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
package screenstudio.sources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author patrick
 */
public class SlideShow {

    private final ArrayList<BufferedImage> mImages = new ArrayList<>();
    private String mID;

    public SlideShow(File[] images) throws IOException {
        mID = "";
        for (File f : images) {
            BufferedImage img = javax.imageio.ImageIO.read(f);
            mImages.add(img);
            mID += f.getAbsolutePath() + ";";
        }
        if (mID.length() > 0) {
            mID = mID.substring(0, mID.length() - 1);
        }
    }

    public SlideShow(String ids) throws IOException {
        mID = ids;
        String[] list = ids.split(";");
        for (String f : list) {
            BufferedImage img = javax.imageio.ImageIO.read(new File(f));
            mImages.add(img);
        }
    }

    public String getID() {
        return mID;
    }

    public ArrayList<BufferedImage> getImages() {
        return mImages;
    }

    public BufferedImage getImage(int index) {
        return mImages.get(index);
    }
    public String toString(){
        return "Slide Show (" + mImages.size() + " images)";
    }
}
