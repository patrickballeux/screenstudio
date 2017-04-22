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
 */
package screenstudio.sources;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class SourceImage extends Source {

    private final File mFile;
    private byte[] data = null;
    private BufferedImage[] images;
    private int currentIndex = 0;
    private long mNextPicture = 0;
    private long mTimeDelay = 1000;
    
    public SourceImage(List<screenstudio.targets.Source.View> views,  File image) {
        super(views, 0, image.getAbsolutePath(), BufferedImage.TYPE_4BYTE_ABGR);
        mFile = image;
        mType = Layout.SourceType.Image;
        mNextPicture = System.currentTimeMillis() + mTimeDelay;
    }
    public SourceImage(List<screenstudio.targets.Source.View> views,  SlideShow imgs) {
        super(views, 0, imgs.getID(), BufferedImage.TYPE_4BYTE_ABGR);
        mFile = null;
        images = new BufferedImage[imgs.getImages().size()];
        for (int i = 0; i < images.length; i++){
            images[i] = new BufferedImage(mBounds.width, mBounds.height, mImageType);
            images[i].createGraphics().drawImage(imgs.getImage(i).getScaledInstance(mBounds.width, mBounds.height, Image.SCALE_SMOOTH), 0, 0, null);
            data = ((DataBufferByte) images[i].getRaster().getDataBuffer()).getData();
        }
        mTimeDelay = 10000;
        mNextPicture = System.currentTimeMillis() + mTimeDelay;
        mType = Layout.SourceType.Image;
    }

    public SourceImage(List<screenstudio.targets.Source.View> views ,BufferedImage image, String id) {
        super(views, 0, id, image.getType());
        mFile = null;
        images = new BufferedImage[1];
        images[0] = image;
        mNextPicture = System.currentTimeMillis() + mTimeDelay;
        mType = Layout.SourceType.Frame;
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        if (mNextPicture <= System.currentTimeMillis()){
            currentIndex++;
            mNextPicture = System.currentTimeMillis() +mTimeDelay;
        }
        if (currentIndex >= images.length) {
            currentIndex = 0;
        }
        data = ((DataBufferByte) images[currentIndex].getRaster().getDataBuffer()).getData();
        System.arraycopy(data, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        if (mFile != null) {
            if (mFile.getName().toUpperCase().endsWith(".GIF")) {
                images = readGif(mFile);
            } else {
                BufferedImage buffer = javax.imageio.ImageIO.read(mFile);
                images = new BufferedImage[1];
                images[0] = new BufferedImage(mBounds.width, mBounds.height, mImageType);
                images[0].createGraphics().drawImage(buffer.getScaledInstance(mBounds.width, mBounds.height, Image.SCALE_SMOOTH), 0, 0, null);
                data = ((DataBufferByte) images[0].getRaster().getDataBuffer()).getData();
            }
        } else if (images.length == 1) {
            BufferedImage buffer = images[0];
            images = new BufferedImage[1];
            images[0] = new BufferedImage(mBounds.width, mBounds.height, mImageType);
            images[0].createGraphics().drawImage(buffer.getScaledInstance(mBounds.width, mBounds.height, Image.SCALE_SMOOTH), 0, 0, null);
            data = ((DataBufferByte) images[0].getRaster().getDataBuffer()).getData();
        } 
    }

    @Override
    protected void disposeStream() throws IOException {
        //nothing to do
    }

    private BufferedImage[] readGif(File input) {
        BufferedImage[] images = new BufferedImage[0];
        try {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            ImageInputStream stream = ImageIO.createImageInputStream(input);
            reader.setInput(stream);
            int count = reader.getNumImages(true);
            images = new BufferedImage[count];
            for (int index = 0; index < count; index++) {
                BufferedImage frame = reader.read(index);
                images[index] = new BufferedImage(mBounds.width, mBounds.height, mImageType);
                images[index].createGraphics().drawImage(frame.getScaledInstance(mBounds.width, mBounds.height, Image.SCALE_SMOOTH), 0, 0, null);
                data = ((DataBufferByte) images[index].getRaster().getDataBuffer()).getData();
            }
            mTimeDelay = 100;

        } catch (IOException ex) {

        }
        return images;
    }
}
