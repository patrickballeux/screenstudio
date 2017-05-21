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
package screenstudio.gui;

/**
 *
 * @author patrick
 */
public class LabelText {
    private String mText = "";
    private int mforegroundColor = 0xFFFFFFFF;
    private int mbackgroundColor = 0x88FFFFFF;
    private int mbackgroundAreaColor = 0x00000000;
    private String mFontName = "Dialog";
    private int mFontSize = 20;
    public LabelText(String text){
        mText = text;
    }
    public void setText(String text){
        mText = text;
    }
    public void setForegroundColor(int value){
        mforegroundColor = value;
    }
    public void setBackgroundColor(int value){
        mbackgroundColor = value;
    }
    public int getForegroundColor(){
        return mforegroundColor;
    }
    public int getBackgroundColor(){
        return mbackgroundColor;
    }
    public String getText(){
        return mText;
    }
    public void setFontName(String value){
        mFontName = value;
    }
    public String getFontName() {
        return mFontName;
    }
    public int getBackgroundAreaColor(){
        return mbackgroundAreaColor;
    }
    public void setBackgroundAreaColor(int c){
        mbackgroundAreaColor = c;
    }
    public int getFontSize(){
        return mFontSize;
    }
    public void setFontSize(int s){
        mFontSize = s;
    }
    @Override
    public String toString(){
        return mText.replaceAll("\\<[^>]*>", "");
    }
}
