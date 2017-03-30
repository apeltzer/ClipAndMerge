/*
 * Copyright (c) 2016. ClipAndMerge Guenter Jaeger
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

package threads;

import java.io.BufferedReader;
import java.io.IOException;

import setting.MergeSettings;
import clipping.Clipper;
import clipping.Read;

public class ReverseClipperThread extends ClipperThread {
	
	private BufferedReader[] br;
	private Clipper clipperR;
	private MergeThread merger;
	private MergeSettings settings;
	
	public ReverseClipperThread(MergeThread merger, MergeSettings settings) {
		this.br = settings.getReverseReadsReader();
		clipperR = new Clipper(settings);
		clipperR.setAdapter(settings.getReverseAdapter());
		clipperR.setAdapterType("reverse");
		clipperR.setMinLength(settings.getMinMergeOverlap()); //min merge overlap at this point to guarantee that merging would be possible afterwards
		clipperR.setMinimumAdapterLength(settings.getMinAdapterAlignmentLength());
		clipperR.setDiscardUnknownBases(!settings.keepSequencesWithN());
		clipperR.init();
		this.merger = merger;
		this.settings = settings;
	}
	
	public void run() {
		if(this.br != null) {
			for(int i = 0; i < this.br.length; i++) {
				try {
					if(br == null) {
						//no file here, nothing to do -> quit
						merger.shutdownReverse();
						return;
					}
					
					String[] readR = readFourLines(br[i]);
					while(readR != null) {
						Read reverseRead = new Read(readR[0], readR[1], readR[2], readR[3]);
						merger.updateReverseBytes(bytesProcessed(readR));
						
						if(settings.noClipping()) {
							merger.putReverseRead(reverseRead);
						} else {
							Read clippedR = clipperR.clip(reverseRead);
							merger.putReverseRead(clippedR);
						}
						
						readR = readFourLines(br[i]);
					}
					br[i].close();
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			if(settings.showClippingStats()) {
				try {
					clipperR.outputStats(settings.getLogWriter());
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			merger.shutdownReverse();
		}
	}
}
