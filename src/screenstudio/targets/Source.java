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


public class Source {
    public Layout.SourceType Type;
    public String ID;
    public int X;
    public int Y;
    public int CaptureX;
    public int CaptureY;
    public int Width;
    public int Height;
    public float Alpha;
    public int Order;
    public int foregroundColor;
    public int backgroundColor;
    public String fontName;
    public long startTime;
    public long endTime;
}
