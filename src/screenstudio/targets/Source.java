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
package screenstudio.targets;

import java.util.ArrayList;

public class Source {

    public Layout.SourceType Type;
    public Object SourceObject;
    public String ID;
    public int CaptureX;
    public int CaptureY;
    public int foregroundColor;
    public int backgroundColor;
    public String fontName;
    public long startTime;
    public long endTime;
    public String transitionStart = "None";
    public String transitionStop = "None";
    public String effect = "None";
    public ArrayList<View> Views = new ArrayList<>();
    public int CurrentViewIndex = -1;

    public Source(int initialViewCount) {
        for (int i = 0; i < initialViewCount; i++) {
            Views.add(new View());
        }
    }

    public void initOtherViews() {
        View vOriginal = getViews().get(getCurrentViewIndex());
        for (View v : getViews()) {
            if (v != vOriginal) {
                v.X = vOriginal.X;
                v.Y = vOriginal.Y;
                v.Width = vOriginal.Width;
                v.Height = vOriginal.Height;
                v.Order = vOriginal.Order;
                v.Alpha = vOriginal.Alpha;
                v.remoteDisplay = vOriginal.remoteDisplay;
            }
        }
    }

    /**
     * @return the Type
     */
    public Layout.SourceType getType() {
        return Type;
    }

    /**
     * @param Type the Type to set
     */
    public void setType(Layout.SourceType Type) {
        this.Type = Type;
    }

    /**
     * @return the SourceObject
     */
    public Object getSourceObject() {
        return SourceObject;
    }

    /**
     * @param SourceObject the SourceObject to set
     */
    public void setSourceObject(Object SourceObject) {
        this.SourceObject = SourceObject;
    }

    /**
     * @return the ID
     */
    public String getID() {
        return ID;
    }

    /**
     * @param ID the ID to set
     */
    public void setID(String ID) {
        this.ID = ID;
    }

    /**
     * @return the CaptureX
     */
    public int getCaptureX() {
        return CaptureX;
    }

    /**
     * @param CaptureX the CaptureX to set
     */
    public void setCaptureX(int CaptureX) {
        this.CaptureX = CaptureX;
    }

    /**
     * @return the CaptureY
     */
    public int getCaptureY() {
        return CaptureY;
    }

    /**
     * @param CaptureY the CaptureY to set
     */
    public void setCaptureY(int CaptureY) {
        this.CaptureY = CaptureY;
    }

    /**
     * @return the foregroundColor
     */
    public int getForegroundColor() {
        return foregroundColor;
    }

    /**
     * @param foregroundColor the foregroundColor to set
     */
    public void setForegroundColor(int foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    /**
     * @return the backgroundColor
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * @return the fontName
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * @param fontName the fontName to set
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the transitionStart
     */
    public String getTransitionStart() {
        return transitionStart;
    }

    /**
     * @param transitionStart the transitionStart to set
     */
    public void setTransitionStart(String transitionStart) {
        this.transitionStart = transitionStart;
    }

    /**
     * @return the transitionStop
     */
    public String getTransitionStop() {
        return transitionStop;
    }

    /**
     * @param transitionStop the transitionStop to set
     */
    public void setTransitionStop(String transitionStop) {
        this.transitionStop = transitionStop;
    }

    /**
     * @return the effect
     */
    public String getEffect() {
        return effect;
    }

    /**
     * @param effect the effect to set
     */
    public void setEffect(String effect) {
        this.effect = effect;
    }

    /**
     * @return the Views
     */
    public ArrayList<View> getViews() {
        return Views;
    }

    /**
     * @param Views the Views to set
     */
    public void setViews(ArrayList<View> Views) {
        this.Views = Views;
    }

    /**
     * @return the CurrentViewIndex
     */
    public int getCurrentViewIndex() {
        return CurrentViewIndex;
    }

    /**
     * @param CurrentViewIndex the CurrentViewIndex to set
     */
    public void setCurrentViewIndex(int CurrentViewIndex) {
        this.CurrentViewIndex = CurrentViewIndex;
    }

    public int getX(){
        return Views.get(CurrentViewIndex).X;
    }
    public void setX(int x){
        Views.get(CurrentViewIndex).X = x;
    }
    public int getY(){
        return Views.get(CurrentViewIndex).Y;
    }
    public void setY(int y){
        Views.get(CurrentViewIndex).X = y;
    }
        public int getWidth(){
        return Views.get(CurrentViewIndex).Width;
    }
    public void setWidth(int w){
        Views.get(CurrentViewIndex).Width = w;
    }
    public int getHeight(){
        return Views.get(CurrentViewIndex).Height;
    }
    public void setHeight(int h){
        Views.get(CurrentViewIndex).Height = h;
    }
    public float getAlpha(){
        return Views.get(CurrentViewIndex).Alpha;
    }
    public void setAlpha(float a){
        Views.get(CurrentViewIndex).Alpha = a;
    }
    public boolean getDisplay(){
        return Views.get(CurrentViewIndex).remoteDisplay;
    }
    public void setDisplay(boolean b){
        Views.get(CurrentViewIndex).remoteDisplay = b;
    }
    public int getOrder(){
        return Views.get(CurrentViewIndex).Order;
    }
    public void setOrder(int o){
        Views.get(CurrentViewIndex).Order = o;
    }

    public static class View {

        public String ViewName = "View";
        public int X = 0;
        public int Y = 0;
        public int Width = 320;
        public int Height = 240;
        public float Alpha = 1f;
        public int Order = 0;
        public boolean remoteDisplay = true;
    }
}
