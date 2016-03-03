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
import java.io.IOException;

public class Statistics {

	private static int numMergedReads = 0;
	private static int numFReads = 0;
	private static int numRReads = 0;
	
	private static int numNotMergedFReads = 0;
	private static int numNotMergedRReads = 0;
	
	private static int numReadPairsTooSmallOverlap = 0;
	
	private static int numReadsNotMergedTooShortF = 0;
	private static int numReadsNotMergedTooShortR = 0;
	
	private static int numReadsNoPartnerTooShortF = 0;
	private static int numReadsNoPartnerTooShortR = 0;
	
	private static int numReadsTooShortF = 0;
	private static int numReadsTooShortR = 0;
	private static int numReadsMergedTooShort = 0;
	
	private static int numDiscardedMergedReads = 0;
	
	private static int sumOverlaps = 0;
	
	public static void printStats(BufferedWriter logWriter) throws IOException {
		logWriter.write("[Merging]");
		logWriter.newLine();
		
		logWriter.write("- Number of usable reads in the output file(s): " + Integer.toString(getNumReads()));
		logWriter.newLine();
		logWriter.write("- Number of usable forward reads with no pairing reverse read: " + Integer.toString(numFReads));
		logWriter.newLine();
		logWriter.write("- Number of usable reverse reads with no pairing forward read: " + Integer.toString(numRReads));
		logWriter.newLine();
		logWriter.write("- Number of usable not merged forward reads: " + Integer.toString(numNotMergedFReads));
		logWriter.newLine();
		logWriter.write("- Number of usable not merged reverse reads: " + Integer.toString(numNotMergedRReads));
		logWriter.newLine();
		logWriter.write("- Number of merged reads discarded due to bad quality: " + Integer.toString(numDiscardedMergedReads));
		logWriter.newLine();
		logWriter.write("- Number of usable merged reads: " + Integer.toString(numMergedReads));
		logWriter.newLine();
		double percMerged = Math.round(((double)numMergedReads/getNumReads()) * 10000.) / 100.;
		logWriter.write("- Percentage of usable merged reads: " + Double.toString(percMerged) + " %");
		logWriter.newLine();
		logWriter.write("- Average overlap region size: " + Double.toString(getAverageOverlap()));
		logWriter.newLine();
		
		logWriter.newLine();
		
		logWriter.write("- Number of read pairs not merged (no overlap): " + Integer.toString(numReadPairsTooSmallOverlap));
		logWriter.newLine();
		
		logWriter.newLine();
		
		int removedSingle = numReadsNotMergedTooShortF + numReadsNotMergedTooShortR + numReadsNoPartnerTooShortF + numReadsNoPartnerTooShortR + numReadsMergedTooShort;
		logWriter.write("- Number of single reads removed: " + Integer.toString(removedSingle));
		logWriter.newLine();
		logWriter.write("     ... not merged and too short forward read: " + Integer.toString(numReadsNotMergedTooShortF));
		logWriter.newLine();
		logWriter.write("     ... not merged and too short reverse read: " + Integer.toString(numReadsNotMergedTooShortR));
		logWriter.newLine();
		logWriter.write("     ... too short forward read with no pairing reverse read: " + Integer.toString(numReadsNoPartnerTooShortF));
		logWriter.newLine();
		logWriter.write("     ... too short reverse read with no pairing forward read: " + Integer.toString(numReadsNoPartnerTooShortR));
		logWriter.newLine();
		logWriter.write("     ... too short merged read: " + Integer.toString(numReadsMergedTooShort));
		logWriter.newLine();
		
		logWriter.newLine();
	}
	
	public static void increaseTooSmallOverlap() {
		numReadPairsTooSmallOverlap++;
	}

	public static int numReadsTotal() {
		int numReadsTooShort = getNumReadsTooShort();
		int numReads = getNumReads();
		return numReads + numReadsTooShort;
	}

	public static void increaseForwardReads() {
		numFReads++;
	}

	public static void increaseReverseReads() {
		numRReads++;
	}

	public static void increaseMergedReads() {
		numMergedReads++;
	}

	public static void increaseForwardTooShort() {
		numReadsTooShortF++;
	}

	public static void increaseReverseTooShort() {
		numReadsTooShortR++;
	}

	public static void increaseMergedTooShort() {
		numReadsMergedTooShort++;
	}
	
	public static int getNumReads() {
		return numFReads + numRReads + numMergedReads + numNotMergedFReads + numNotMergedRReads - numDiscardedMergedReads;
	}
	
	public static int getNumReadsTooShort() {
		return numReadsTooShortF + numReadsTooShortR + numReadsMergedTooShort;
	}

	public static void increaseNumNotMergedForward() {
		numNotMergedFReads++;
	}

	public static void increaseNumNotMergedReverse() {
		numNotMergedRReads++;
	}

	public static void increaseNotMergedForwardTooShort() {
		numReadsNotMergedTooShortF++;
		numReadsTooShortF++;
	}

	public static void increaseNotMergedReverseTooShort() {
		numReadsNotMergedTooShortR++;
		numReadsTooShortR++;
	}

	public static void increaseNoPartnerTooShortF() {
		numReadsNoPartnerTooShortF++;
		numReadsTooShortF++;
	}

	public static void increaseNoPartnerTooShortR() {
		numReadsNoPartnerTooShortR++;
		numReadsTooShortR++;
	}

	public static void increaseReadPairsNotMerged() {
		numReadPairsTooSmallOverlap++;
	}

	public static void increaseDiscardedMergedReads() {
		numDiscardedMergedReads++;
	}
	
	public static void increaseMergingOverlap(int overlap) {
		sumOverlaps+=overlap;
	}
	
	public static double getAverageOverlap() {
		return Math.round((((double)sumOverlaps / (double)numMergedReads))*1000.)/1000.;
	}
}
