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

package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import setting.MergeSettings;
import trimming.QualityTrimmer;
import clipping.Read;

public class DataHandler {
	
	private MergeSettings settings;
	
	private BufferedWriter bw = null;
	private BufferedWriter mpwf = null;
	private BufferedWriter mpwr = null;
	
	private QualityTrimmer qt;
	
	private int currentOverlap = 0;
	
	public DataHandler(MergeSettings settings) {
		this.settings = settings;
		this.qt = new QualityTrimmer(settings);
	}
	
	public void setup() throws IOException {
		this.bw = settings.getOutputWriter();
		
		if(settings.handleMatePairsSeperatly()) {
			File frf = settings.getMatePairFileForward();
			File rrf = settings.getMatePairFileReverse();
			
			if(frf.getName().endsWith(".gz")) {
				mpwf = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(frf))));
			} else {
				mpwf = new BufferedWriter(new FileWriter(settings.getMatePairFileForward()));
			}
			
			if(rrf.getName().endsWith(".gz")) {
				mpwr = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(rrf))));
			} else {
				mpwr = new BufferedWriter(new FileWriter(settings.getMatePairFileReverse()));
			}
		} else {
			mpwf = bw;
			mpwr = bw;
		}
	}

	public void writeMatePairReads(Read readF, Read readR) throws IOException {
		boolean fOK = false;
		boolean rOK = false;
		int minLength = settings.getMinSequenceLength();
		
		if(settings.qualityTrimming()) {
			qt.trim(readF);
			qt.trim(readR);
		}
		
		if(readF.sequence.length() >= minLength) {
			fOK = true;
		} else {
			Statistics.increaseNotMergedForwardTooShort();
		}
		
		if(readR.sequence.length() >= minLength) {
			rOK = true;
		} else {
			Statistics.increaseNotMergedReverseTooShort();
		}
		
		if(fOK && rOK) {
			if(settings.handleMatePairsSeperatly()) {
				writeRead(readF, "", mpwf);
				writeRead(readR, "", mpwr);
			} else {
				writeRead(readF, "F", mpwf);
				writeRead(readR, "R", mpwr);
			}
			Statistics.increaseNumNotMergedForward();
			Statistics.increaseNumNotMergedReverse();
			Statistics.increaseReadPairsNotMerged();
		} else if(fOK && !settings.removeSingleReads()) {
			writeRead(readF, "F", bw);
			Statistics.increaseNumNotMergedForward();
		} else if(rOK && !settings.removeSingleReads()) {
			writeRead(readR, "R", bw);
			Statistics.increaseNumNotMergedReverse();
		}
	}
	
	public void writeSingleEndRead(Read read, String prefix) throws IOException {
		int minLength = settings.getMinSequenceLength();
		//merged reads do not need to be quality trimmed
		if(settings.qualityTrimming() && !prefix.equals("M")) {
			qt.trim(read);
		}
		
		if(read.sequence.length() < minLength) {
			if(prefix.equals("M")) {
				Statistics.increaseMergedTooShort();
			} else if(prefix.equals("F")) {
				Statistics.increaseNoPartnerTooShortF();
			} else if(prefix.equals("R")) {
				Statistics.increaseNoPartnerTooShortR();
			}
		} else {
			if(prefix.equals("F")) {
				Statistics.increaseForwardReads();
			} else if(prefix.equals("R")) {
				Statistics.increaseReverseReads();
			} else if(prefix.equals("M")) {
				Statistics.increaseMergedReads();
				Statistics.increaseMergingOverlap(getCurrentOverlap());
			}
			
			if(!settings.removeSingleReads())
				this.writeRead(read, prefix, bw);
		}
	}
	
	public int getCurrentOverlap() {
		return this.currentOverlap;
	}
	
	public void setCurrentOverlap(int overlap) {
		this.currentOverlap = overlap;
	}

	private void writeRead(Read read, String prefix, BufferedWriter bw) throws IOException {
		StringBuffer result = new StringBuffer();
		result.append(read.name.replaceFirst("@", "@"+prefix+"_"));
		result.append("\n");
		result.append(read.sequence);
		result.append("\n");
		result.append(read.empty);
		result.append("\n");
		result.append(read.quality);
		result.append("\n");

		bw.append(result);
		bw.flush();
	}
	
	public synchronized void shutdown() throws IOException {
		if(this.bw != null) {
			this.bw.flush();
			this.bw.close();
		}
		
		if(settings.handleMatePairsSeperatly()) {
			this.mpwf.flush();
			this.mpwf.close();
			this.mpwr.flush();
			this.mpwr.close();
		}
		
		synchronized(settings.getLogWriter()) {
			Statistics.printStats(settings.getLogWriter());
			settings.getLogWriter().flush();
		}
	}
}
