/*
 * Copyright (C) 2014 Patrick Balleux
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

/**
 *
 * @author patrick
 */
public enum SIZES {

    SOURCE,
    OUT_240P,
    OUT_360P,
    OUT_480P,
    OUT_720P,
    OUT_1080P;

    @Override
    public String toString() {
        String retValue = "ND";
        switch (this) {
            case OUT_1080P:
                retValue = "1080p";
                break;
            case OUT_240P:
                retValue = "240p";
                break;
            case OUT_360P:
                retValue = "360p";
                break;
            case OUT_480P:
                retValue = "480p";
                break;
            case OUT_720P:
                retValue = "720p";
                break;
            case SOURCE:
                retValue = "Display";
                break;
        }
        return retValue;
    }

}
