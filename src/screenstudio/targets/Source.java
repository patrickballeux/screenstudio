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
        View vOriginal = Views.get(CurrentViewIndex);
        for (View v : Views) {
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
