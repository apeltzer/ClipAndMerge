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

import io.DataHandler;
import io.Statistics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import main.MergeScript;
import setting.MergeSettings;
import trimming.QualityTrimmer;
import clipping.Read;

public class MergeThread extends Thread {

	private BlockingQueue<Read> forwardReads;
	private BlockingQueue<Read> reverseReads;

	Read terminatingRead = new Read("Ende", "", "", "");

	private long bytesProcessedForward = 0;
	private long bytesProcessedReverse = 0;

	private MergeScript mergeScript;
	private MergeSettings settings;

	private DataHandler dh;

	public MergeThread(MergeScript mergeScript, MergeSettings settings) throws Exception {
		this.mergeScript = mergeScript;
		this.settings = settings;
		this.dh = new DataHandler(settings);

		forwardReads = new LinkedBlockingQueue<Read>(settings.maxReadsPerQueue());
		reverseReads = new LinkedBlockingQueue<Read>(settings.maxReadsPerQueue());
	}

	public synchronized void updateForwardBytes(long processedBytes) {
		this.bytesProcessedForward += processedBytes;
	}

	public synchronized void updateReverseBytes(long processedBytes) {
		this.bytesProcessedReverse += processedBytes;
	}

	public void putForwardRead(Read forwardRead) {
		try {
			forwardReads.put(forwardRead);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void putReverseRead(Read reverseRead) {
		try {
			reverseReads.put(reverseRead);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdownReverse() {
		try {
			reverseReads.put(terminatingRead);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdownForward() {
		try {
			forwardReads.put(terminatingRead);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			Read forwardRead;
			Read reverseRead;

			dh.setup();

      long numReadPairsRead = 0;

			if(settings.getReverseReadsReader() == null) { //only single end read file available
				while((forwardRead = forwardReads.take()) != terminatingRead) {
					dh.writeSingleEndRead(forwardRead, "F");
				}
			} else { //forward and reverse read files available
				while ((forwardRead = forwardReads.take()) != terminatingRead
						&& (reverseRead = reverseReads.take()) != terminatingRead) {

          numReadPairsRead++;

					if(forwardRead == ClipperThread.nullRead && reverseRead != ClipperThread.nullRead) {
						//forward read was an adapter only read and removed already
						//just output the reverse read if it is long enough
						dh.writeSingleEndRead(reverseRead, "R");
					} else if(reverseRead == ClipperThread.nullRead && forwardRead != ClipperThread.nullRead) {
						//reverse read was an adapter only read and removed already
						//just output the forward read if it is long enough
						dh.writeSingleEndRead(forwardRead, "F");
					} else if(forwardRead != ClipperThread.nullRead && reverseRead != ClipperThread.nullRead) {
						//we have clipped both reads, now we can try to merge them
						if(settings.noMerging()) {
							dh.writeMatePairReads(forwardRead, reverseRead);
						} else {
							merge(forwardRead, reverseRead, settings.getMinMergeOverlap(), settings.getErrorRateForMerging(), settings.getQualityEncoding(), settings.getQualityBasedMM());
						}
					} else {
						//reads have both been rejected after clipping
            Statistics.increaseNumReadsFailClipping();
					}

					synchronized(this) {
						if(numReadPairsRead % 100000 == 0) {
							long processed = Math.round(bytesProcessedForward + bytesProcessedReverse);
							mergeScript.updateProcessBytes(processed);
						}
					}
			    }
			}

			dh.shutdown();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void merge(Read readF, Read readR, int minOverlap, double errRate, int qualityOffset, boolean qualityBasedMM) throws Exception {
		//get sequences
		String seqF = readF.sequence;
		String seqRreverse = reverseComplement(readR.sequence);

		String qualF = readF.quality;
		StringBuffer reverseQual = new StringBuffer(readR.quality);
		reverseQual.reverse();
		String qualR = reverseQual.toString();

		int[] overlapIndex = findOverlap(seqF, seqRreverse, qualF, qualR, minOverlap, errRate, qualityOffset, qualityBasedMM);

		//there is an overlap, build the new sequence
		if (overlapIndex[0] >= 0 && overlapIndex[1] >= 0) {
			//define new seq and new qual strings
			StringBuffer newSeq = new StringBuffer();
			newSeq.append(readF.sequence.subSequence(0, overlapIndex[0]));
			StringBuffer newQual = new StringBuffer();
			newQual.append(readF.quality.substring(0, overlapIndex[0]));

			// calculate overlap
			int overlap = readF.sequence.length() - overlapIndex[0];
			dh.setCurrentOverlap(overlap);

			// the better quality decides which nucleotide to use in the new sequence
			for(int i = 0; i < overlap; i++) {
				if (qualF.charAt(i+overlapIndex[0]) >= qualR.charAt(i+overlapIndex[1])){
					newSeq.append(seqF.charAt(i+overlapIndex[0]));
					newQual.append(qualF.charAt(i+overlapIndex[0]));
				} else {
					newSeq.append(seqRreverse.charAt(i+overlapIndex[1]));
					newQual.append(qualR.charAt(i+overlapIndex[1]));
				}
			}

			if(seqRreverse.length() > overlapIndex[1] + overlap) {
				newSeq.append(seqRreverse.substring(overlapIndex[1] + overlap));
				newQual.append(qualR.substring(overlapIndex[1] + overlap));
			}

			//replace old read
			readF.sequence = newSeq.toString();
			readF.quality = newQual.toString();

			dh.writeSingleEndRead(readF, "M");
		} else { //try to flip strands and merge again -> this helps in some cases!
			int[] overlapIndexReverse = findOverlap(seqRreverse, seqF, qualR, qualF, minOverlap, errRate, qualityOffset, qualityBasedMM);

			if(overlapIndexReverse[0] >= 0 && overlapIndexReverse[1] >= 0) {
				//calculate overlap
				int overlap = seqRreverse.length() - overlapIndexReverse[0];
				dh.setCurrentOverlap(overlap);

				//define new seq and new qual strings
				StringBuffer newSeq = new StringBuffer();
				newSeq.append(seqF.substring(0,overlapIndexReverse[1]));
				StringBuffer newQual = new StringBuffer();
				newQual.append(qualF.substring(0,overlapIndexReverse[1]));

				// the better quality decides which nucleotide to use in the new sequence
				for(int i = 0; i < overlap; i++) {
					if (qualR.charAt(i+overlapIndexReverse[0]) >= qualF.charAt(i+overlapIndexReverse[1])){
						newSeq.append(seqRreverse.charAt(i+overlapIndexReverse[0]));
						newQual.append(qualR.charAt(i+overlapIndexReverse[0]));
					} else {
						newSeq.append(seqF.charAt(i+overlapIndexReverse[1]));
						newQual.append(qualF.charAt(i+overlapIndexReverse[1]));
					}
				}

				if(overlapIndexReverse[1] > 0 && seqRreverse.length() > overlap) {
					newSeq.append(seqRreverse.substring(overlap));
					newQual.append(qualF.substring(overlap));
				}

				//replace old read
				readF.sequence = newSeq.toString();
				readF.quality = newQual.toString();


//				System.out.println("--------");
//				for(int i = 0; i < overlapIndexReverse[0]; i++)
//					System.out.print(" ");
//				System.out.println(seqF);
//				for(int i = 0; i < overlapIndexReverse[0]; i++)
//					System.out.print(" ");
//				System.out.println(qualF);
//				System.out.println(seqRreverse);
//				System.out.println(qualR);
//				for(int i = 0; i < overlapIndexReverse[0]; i++)
//					System.out.print(" ");
//				System.out.println(readF.sequence);
//				for(int i = 0; i < overlapIndexReverse[0]; i++)
//					System.out.print(" ");
//				System.out.println(readF.quality);
//
//				System.out.println(Arrays.toString(overlapIndexReverse));
//
//				System.out.println("Overlap: " + overlap);

				if(!settings.discardBadQualityReads()) {
					dh.writeSingleEndRead(readF, "M");
				} else {
					if(!badQualityCheck(readF, settings.getMinGoodQualityBasePercentage(),
							settings.getDiscardBadQualityReadsScore())) {
						dh.writeSingleEndRead(readF, "M");
					} else {
						Statistics.increaseDiscardedMergedReads();
					}
				}

			} else { // no overlap found
				dh.writeMatePairReads(readF, readR);
			}
		}
	}

	public boolean badQualityCheck(Read r, double p, int s) {
		double rp = QualityTrimmer.getPercentageBadQuality(r, s, settings.getQualityEncoding());
		//p is the percentage of needed good quality bases
		//the percentage of allowed bad quality bases is equal to 1-p
		return rp > (1-p);
	}

	public static String reverseComplement(String string) {
		StringBuilder build = new StringBuilder(string.length());
		for (int i = string.length() -1 ; i>=0 ;i-- ){
			build.append(replace(string.charAt(i)));
		}
		return build.toString();
	}

	public static int[] findOverlap(String seq1, String seq2, String qual1, String qual2, int minOverlap, double errRate, int qualOffset, boolean qualityBasedMM){
		char[] s1 = seq1.toCharArray();
		char[] s2 = seq2.toCharArray();

		char[] q1 = qual1.toCharArray();
		char[] q2 = qual2.toCharArray();

		int n = s1.length;
		int m = s2.length;

		//check whether reverse read is longer than forward read
		int rOffset = Math.max(m-n, 0);

		int start = Math.max(0, n-m);
		int stop = n - minOverlap + 1;

		int currentBestFIndex = -1;
		int currentBestRIndex = -1;
		int numMatches = 0;
		int sum = 0;

		for(int i = start; i < stop; i++) {
			while(rOffset > 0) {
				sum = 0;
				for(int j = 0; j < Math.min(n - i, m); j++) {
					char one = s1[i+j];
					char two = s2[j+rOffset];
					char qualOne = q1[i+j];
					char qualTwo = q2[j+rOffset];
					if(qualityBasedMM) {
						sum += missMatches(one, two, qualOne, qualTwo, qualOffset);
					} else {
						sum += missMatches(one, two);
					}
				}

				int overlap = n - i;
				double threshold = overlap * errRate;

				if(sum <= threshold) {
					int currentMatches = overlap - sum;
					if(currentMatches > numMatches) {
						currentBestFIndex = i;
						currentBestRIndex = rOffset;
						numMatches = currentMatches;
					}
				}

				rOffset--;
			}

			sum = 0;
			for(int j = 0; j < Math.min(n - i, m); j++) {
				char one = s1[i+j];
				char two = s2[j];
				char qualOne = q1[i+j];
				char qualTwo = q2[j];
				if(qualityBasedMM) {
					sum += missMatches(one, two, qualOne, qualTwo, qualOffset);
				} else {
					sum += missMatches(one, two);
				}
			}

			int overlap = n - i;
			double threshold = overlap * errRate;

			if(sum <= threshold) {
				int currentMatches = overlap-sum;
				if(currentMatches > numMatches) {
					currentBestFIndex = i;
					currentBestRIndex = rOffset;
					numMatches = currentMatches;
				}
			}
		}

		return new int[]{currentBestFIndex, currentBestRIndex};
	}

	public static int missMatches(char s1, char s2, char q1, char q2, int qualityEncoding) {

		int q1i = QualityTrimmer.translateQuality(q1, qualityEncoding);
		int q2i = QualityTrimmer.translateQuality(q2, qualityEncoding);

		if(q1i < 10) {
			s1 = 'N';
		}

		if(q2i < 10) {
			s2 = 'N';
		}

		if (s1 == s2 || s1 == 'N' || s2 == 'N') {
			return 0;
		}

		return 1;
	}

	public static int missMatches(char s1, char s2) {
		if (s1 == s2 || s1 == 'N' || s2 == 'N') {
			return 0;
		}
		return 1;
	}

	public static char replace(char in) {
		switch(in) {
		case 'A' : return 'T';
		case 'T' : return 'A';
		case 'G' : return 'C';
		case 'C' : return 'G';
		default : return in;
		}
	}
}
