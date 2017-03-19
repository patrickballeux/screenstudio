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
package screenstudio.sources.effects;

import com.jhlabs.image.*;
import java.awt.image.BufferedImage;

/**
 *
 * @author patrick
 */
public class Effect {

    BlockFilter blockFilter;
    BlurFilter blurFilter;
    BorderFilter borderFilter;
    EdgeFilter edgeFilter;
    GrayscaleFilter grayFilter;
    KaleidoscopeFilter kaleidoscopeFilter;
    SharpenFilter sharpenFilter;
    
    public enum eEffects {
        None,
        Block,
        Blur,
        Sharpen,
        Edge,
        Gray,
        Kaleidoscope,
    }

    public BufferedImage apply(eEffects effect, BufferedImage img) {
        BufferedImage retValue = img;
        switch (effect) {
            case None:
                retValue = img;
                break;
            case Block:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                blockFilter.filter(img, retValue);
                break;
            case Edge:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                edgeFilter.filter(img, retValue);
                break;
            case Blur:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                blurFilter.filter(img, retValue);
                break;
            case Gray:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                grayFilter.filter(img, retValue);
                break;
            case Kaleidoscope:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                kaleidoscopeFilter.filter(img, retValue);
                break;
            case Sharpen:
                retValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                sharpenFilter.filter(img, retValue);
                break;
        }
        return retValue;
    }

    public Effect() {
        blockFilter = new BlockFilter();
        blockFilter.setBlockSize(5);
        blurFilter = new BlurFilter();
        borderFilter = new BorderFilter();
        borderFilter.setBottomBorder(3);
        borderFilter.setLeftBorder(3);
        borderFilter.setRightBorder(3);
        borderFilter.setTopBorder(3);

        edgeFilter = new EdgeFilter();

        grayFilter = new GrayscaleFilter();

        kaleidoscopeFilter = new KaleidoscopeFilter();
        kaleidoscopeFilter.setSides(6);
        
        sharpenFilter = new SharpenFilter();
    }

}
