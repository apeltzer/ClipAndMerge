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

package clipping;

import setting.MergeSettings;
import threads.ClipperThread;
import trimming.EndTrimmer;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 
 * @author jaeger
 *
 */
public class Clipper {

	private String adapter = "CCTTAAGG";
	private String adapterType = "forward";
	private int minLength = 5;
	private boolean discardUnknownBases = true;
	private int keepDelta = 0;
	private boolean discardNonClipped = false;
	private boolean discardClipped = false;
	private boolean showAdapterOnly = false;
	private boolean debug = false;
	private int minimumAdapterLength = 0;
	
	private long countInput = 0;
	private long countDiscardedTooShort = 0; // -l N option
	private long countDiscardedAdapterAtIndexZero = 0; //empty sequences
	private long countDiscardedNoAdapterFound = 0; // -c option
	private long countDiscardedAdapterFound = 0; // -C option
	private long countDiscardedN = 0; // -n option
	
	private EndTrimmer trimmer;
	private FastX fastx;
	
	public Clipper(MergeSettings settings) {
		trimmer = new EndTrimmer(settings);
	}
	
	public void setAdapter(String adapter) {
		this.adapter = adapter;
	}
	
	public void setAdapterType(String adapterType) {
		this.adapterType = adapterType;
	}
	
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
	
	public void setDiscardUnknownBases(boolean discardUnknownBases) {
		this.discardUnknownBases = discardUnknownBases;
	}
	
	public void setKeepDelta(int keepDelta) {
		this.keepDelta = keepDelta;
	}
	
	public void setDiscardNonClipped(boolean discardNonClipped) {
		this.discardNonClipped = discardNonClipped;
	}
	
	public void setDiscardClipped(boolean discardClipped) {
		this.discardClipped = discardClipped;
	}
	
	public void setShowAdapterOnly(boolean showAdapterOnly) {
		this.showAdapterOnly = showAdapterOnly;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public void setMinimumAdapterLength(int minimumAdapterLength) {
		this.minimumAdapterLength = minimumAdapterLength;
	}
	
	public Read clip(Read read) throws Exception {
		fastx.readNextRecord(read);
		readsCount = fastx.getReadsCount();
		
		String query = fastx.nucleotides();
		String target = adapter;
		
		HalfLocalSequenceAlignment align = new HalfLocalSequenceAlignment();
		align.align(query, target);
		
		if(debug) {
			align.printMatrix();
			align.results().print();
		}
		
		countInput += readsCount;
		
		i = adapterCutoffIndex(align.results());
		
		if(i != -1 && i > 0) {
			i += keepDelta;
			//trim the string after this position
			String nucleotides = fastx.nucleotides();
			fastx.setNucleotides(nucleotides.substring(0, i));
		}
		
		if(i == 0) {
			if(!showAdapterOnly) {
				countDiscardedAdapterAtIndexZero += readsCount;
				return ClipperThread.nullRead;
			}
		}
	
		if(fastx.nucleotides().length() < minLength) {
			countDiscardedTooShort += readsCount;
			return ClipperThread.nullRead;
		}
		
		if(i == -1 && discardNonClipped) {
			countDiscardedNoAdapterFound += readsCount;
			return ClipperThread.nullRead;
		}
		
		if(i > 0 && discardClipped) {
			countDiscardedAdapterFound += readsCount;
			return ClipperThread.nullRead;
		}
		
		if(discardUnknownBases && fastx.nucleotides().contains("N")) {
			countDiscardedN += readsCount;
			return ClipperThread.nullRead;
		}
		
		//perform read trimming if necessary
		Read r = fastx.getClippedRead();
		trimmer.trim(r);
		
		//check size again after trimming
		if(r.sequence.length() < minLength) {
			countDiscardedTooShort += readsCount;
			return ClipperThread.nullRead;
		}
		
		return r;
	}
	
	int i = 0;
	int readsCount = 0;
	
	public void init() {
		fastx = new FastX();
		fastx.init(!discardUnknownBases, true, true);
	}
	
	public int adapterCutoffIndex(SequenceAlignmentResults alignmentResults) {
		int alignmentSize = alignmentResults.neutralMatches() +
								alignmentResults.matches() +
								alignmentResults.mismatches() +
								alignmentResults.gaps();
		
		//no alignment at all
		if(alignmentSize == 0) {
			return -1;
		}
		
		if(minimumAdapterLength > 0 && alignmentSize < minimumAdapterLength) {
			return -1;
		}
		
		if(alignmentResults.queryEnd() == alignmentResults.querySize() - 1 && alignmentResults.mismatches() == 0) {
			return alignmentResults.queryStart();
		}
		
		if(alignmentSize > 5 && alignmentResults.targetStart() == 0 && (alignmentResults.matches() * 100 / alignmentSize) >= 75) {
			return alignmentResults.queryStart();
		}
		
		if(alignmentSize > 11 && (alignmentResults.matches() * 100 / alignmentSize) >= 80) {
			return alignmentResults.queryStart();
		}
		
		if(alignmentResults.queryEnd() >= alignmentResults.querySize() - 2 && alignmentSize <= 5 && alignmentResults.matches() >= 3) {
			return alignmentResults.queryStart();
		}
		
		return -1; 
	}

	public void outputStats(BufferedWriter logWriter) throws IOException {
		synchronized(logWriter) {
			
			logWriter.newLine();
			logWriter.write("[Clipping " + adapterType + "]");
			logWriter.newLine();
			logWriter.write("- Min. Length: " + Integer.toString(minLength));
			logWriter.newLine();
			
			if(discardClipped) {
				logWriter.write("- Clipped reads - discarded.");
				logWriter.newLine();
			}
			if(discardNonClipped) {
				logWriter.write("- Non-Clipped reads - discarded");
				logWriter.newLine();
			}
			
			logWriter.write("- Input reads: " + Long.toString(countInput));
			logWriter.newLine();
			
			logWriter.write("- Output reads: " + (countInput 
					- countDiscardedTooShort 
					- countDiscardedNoAdapterFound 
					- countDiscardedAdapterFound 
					- countDiscardedN 
					- countDiscardedAdapterAtIndexZero));
			logWriter.newLine();
			
			logWriter.write("- Discarded too short: " + Long.toString(countDiscardedTooShort));
			logWriter.newLine();
			logWriter.write("- Discarded adapter only: " + Long.toString(countDiscardedAdapterAtIndexZero));
			logWriter.newLine();
			
			if(discardNonClipped) {
				logWriter.write("- Discarded non-clipped: " + Long.toString(countDiscardedNoAdapterFound));
				logWriter.newLine();
			}
			if(discardClipped) {
				logWriter.write("- Discarded clipped: " + Long.toString(countDiscardedAdapterFound));
				logWriter.newLine();
			}
			if(discardUnknownBases) {
				logWriter.write("- Discarded containing N: " + Long.toString(countDiscardedN));
				logWriter.newLine();
			}
			
			logWriter.newLine();
		}
	}
}
