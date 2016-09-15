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

public class EndTrimmer {

	private MergeSettings settings;
	
	public EndTrimmer(MergeSettings settings) {
		this.settings = settings;
	}
	
	public void trim(Read r) {
		int trim5P = settings.getTrim5P(); 
		int trim3P = settings.getTrim3P();
		int lastBase = settings.getLastBaseToKeep();
		
		String sequence = r.sequence;
		
		int firstIndex = Math.max(trim5P, 0); //trim5p equals the first index to keep
		int lastIndex = Math.max(sequence.length() - trim3P, 0);
		
		if(lastBase < Integer.MAX_VALUE) { //disregard trim3p if lastBase is set by the user
			lastIndex = Math.max(lastBase, 0);
			lastIndex = Math.min(lastIndex, sequence.length());
		}
		
		if(lastIndex <= firstIndex) {
			//nothing to do
			return;
		}
		
		//sequence should be trimmed!
		r.sequence = r.sequence.substring(firstIndex, lastIndex);
		r.quality = r.quality.substring(firstIndex, lastIndex);
	}
}
