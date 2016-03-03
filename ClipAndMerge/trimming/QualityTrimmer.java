/*
 * Copyright (c) 2016. ClipAndMerge Günter Jäger
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package trimming;

import clipping.Read;
import setting.MergeSettings;

public class QualityTrimmer {

	private MergeSettings settings;
	
	public QualityTrimmer(MergeSettings settings) {
		this.settings = settings;
	}
	
	public void trim(Read r) {
		int minQual = settings.getMinBaseQuality();
		String sequence = r.sequence;
		String 	quality = r.quality;
		
		int lastIndex = sequence.length() - 1;
		
		while(lastIndex >= 0 && translateQuality(quality.charAt(lastIndex), settings.getQualityEncoding()) < minQual) {
			lastIndex--;
		}
		
		if(lastIndex < 0) {
			r.sequence = "";
			r.quality = "";
		} else {
			r.sequence = r.sequence.substring(0, lastIndex + 1);
			r.quality = r.quality.substring(0, lastIndex + 1);
		}
	}
	
	public static double getPercentageBadQuality(Read r, int minQual, int qualOffset) {
		String sequence = r.sequence;
		String quality = r.quality;
		
		int lastIndex = sequence.length() - 1;
		
		int badQualCount = 0;
		
		for(int i = 0; i <= lastIndex; i++) {
			if(translateQuality(quality.charAt(i), qualOffset) < minQual) {
				badQualCount++;
			}
		}
		
		return (double)badQualCount / (double) sequence.length();
	}
	
	public static int translateQuality(char c, int qualityOffset) {
		c = Character.toUpperCase(c);
		int num = (int)c; //transform to integer
		int r = num - qualityOffset;
		return r;
	}
}
