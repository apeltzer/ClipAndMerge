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

package threads;

import clipping.Clipper;
import clipping.Read;
import setting.MergeSettings;

import java.io.BufferedReader;
import java.io.IOException;

public class ForwardClipperThread extends ClipperThread {
	
	private BufferedReader[] br;
	private Clipper clipperF;
	private MergeThread merger;
	private MergeSettings settings;
	
	public ForwardClipperThread(MergeThread merger, MergeSettings settings) {
		this.br = settings.getForwardReadsReader();
		clipperF = new Clipper(settings);
		clipperF.setAdapter(settings.getForwardAdapter());
		clipperF.setAdapterType("forward");
		clipperF.setMinLength(settings.getMinMergeOverlap()); //min merge overlap at this point to guarantee that merging would be possible afterwards
		clipperF.setMinimumAdapterLength(settings.getMinAdapterAlignmentLength());
		clipperF.setDiscardUnknownBases(!settings.keepSequencesWithN());
		clipperF.init();
		this.merger = merger;
		this.settings = settings;
	}
	
	public void run() {
		for(int i = 0; i < this.br.length; i++) {
			try {
				if(br == null) {
					//no file here, nothing to do -> quit
					merger.shutdownForward();
					return;
				}
				
				String[] readF = readFourLines(br[i]);
				while(readF != null) {
					Read forwardRead = new Read(readF[0], readF[1], readF[2], readF[3]);
					merger.updateForwardBytes(bytesProcessed(readF));
					
					if(settings.noClipping()) {
						merger.putForwardRead(forwardRead);
					} else {
						Read clippedF = clipperF.clip(forwardRead);
						merger.putForwardRead(clippedF);
					}
					
					readF = readFourLines(br[i]);
				}
				
				br[i].close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if(settings.showClippingStats()) {
			try {
				clipperF.outputStats(settings.getLogWriter());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		merger.shutdownForward();
	}
}
