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

package setting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MergeSettings {
	
	BufferedReader[] forwardReadsReader = null;
	BufferedReader[] reverseReadsReader = null;
	
	File[] forwardReads = null;
	File[] reverseReads = null;
	
	//minimal overlap in order to merge forward and reverse read
	int p = 10;
	//discard sequences shorter than l
	int l = 30;
	//keep sequences with 'N' nucleotides if n == true
	boolean n = true;
	//minimum adapter alignment length
	int m = 1;
	//error rate for merging
	double e = 0.05;
	//output additional clipper information
	boolean clippingStats = true;
			
	boolean noClipping = false;
			
	File outputMatePairsForward = null;
	File outputMatePairsReverse = null;
	
	String forwardAdapter = "AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC";
	String reverseAdapter = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA";
	
	BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(System.err));
	BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
	
	boolean noMerging = false;
	
	boolean removeSingleReads = false;
	
	boolean qualityTrimming = false;
	int minBaseQual = 20;
	int qualityEncoding = 33;
	
	boolean noTimeEstimation = true;
	boolean verbose = false;
	
	boolean qualityBasedMM = true;
	
	int maxReadsPerQueue = 1000;
	
	int discardBadQualityReadsScore = 0;
	double minGoodQualityBasePercentage = 0.9;
	
	int trim5P = 0;
	int trim3P = 0;
	int lastBaseTrim = Integer.MAX_VALUE;
	
	public MergeSettings() {
		//nothing to do;
	}
	
	public void setTrim5P(int trim5P) {
		this.trim5P = trim5P;
	}
	
	public void setTrim3P(int trim3P) {
		this.trim3P = trim3P;
	}
	
	public void setLastBaseToKeep(int lastBase) {
		this.lastBaseTrim = lastBase;
	}
	
	public int getLastBaseToKeep() {
		return this.lastBaseTrim;
	}
	
	public int getTrim5P() {
		return this.trim5P;
	}
	
	public int getTrim3P() {
		return this.trim3P;
	}
	
	public void setMinGoodQualityBasePercentage(double minGQBP) {
		this.minGoodQualityBasePercentage = minGQBP;
	}
	
	public double getMinGoodQualityBasePercentage() {
		return this.minGoodQualityBasePercentage;
	}
	
	public boolean discardBadQualityReads() {
		return this.discardBadQualityReadsScore > 0;
	}
	
	public int getDiscardBadQualityReadsScore() {
		return this.discardBadQualityReadsScore;
	}
	
	public void setDiscardBadQualityReadsScore(int score) {
		this.discardBadQualityReadsScore = score;
	}
	
	public void setQualityBasedMM(boolean qbMM) {
		this.qualityBasedMM = qbMM;
	}
	
	public void setMaxReadsPerQueue(int maxReads) {
		this.maxReadsPerQueue = maxReads;
	}
	
	public void setOutputFile(String filePath) throws Exception {
		File f = new File(filePath);
			
		if(!f.exists()) {
			try {
				f.createNewFile();
			} catch(Exception e) {
				throw new RuntimeException("ERROR: Cannot write to file " + f.getAbsolutePath() + " ! Exiting ...");
			}
		}
		
		if(!f.canWrite()) {
			throw new RuntimeException("ERROR: Cannot write to file " + f.getAbsolutePath() + " ! Exiting ...");
		}
		
		if(filePath.endsWith(".gz")) {
			OutputStream gzStream = new GZIPOutputStream(new FileOutputStream(f));
			Writer decoder = new OutputStreamWriter(gzStream);
			this.outputWriter = new BufferedWriter(decoder);
		} else {
			this.outputWriter = new BufferedWriter(new FileWriter(f));
		}
	}
	
	public String getForwardAdapter() {
		return this.forwardAdapter;
	}
	
	public void setForwardAdapter(String fa) {
		this.forwardAdapter = fa;
	}
	
	public String getReverseAdapter() {
		return this.reverseAdapter;
	}
	
	public void setReverseAdapter(String ra) {
		this.reverseAdapter = ra;
	}
	
	public int getMinMergeOverlap() {
		return this.p;
	}
	
	public void setMinMergeOverlap(int p) {
		this.p = p;
	}
	
	public int getMinSequenceLength() {
		return this.l;
	}
	
	public void setMinSequenceLength(int l) {
		this.l = l;
	}
	
	public boolean keepSequencesWithN() {
		return this.n;
	}
	
	public void setKeepSequencesWithN(boolean n) {
		this.n = n;
	}
	
	public int getMinAdapterAlignmentLength() {
		return this.m;
	}
	
	public void setMinAdapterAlignmentLength(int m) {
		this.m = m;
	}
	
	public double getErrorRateForMerging() {
		return this.e;
	}
	
	public void setErrorRateForMerging(double e) {
		this.e = e;
	}
	
	public void setShowClippingStats(boolean verbose) {
		this.clippingStats = verbose;
	}
	
	public boolean noClipping() {
		return this.noClipping;
	}
	
	public void setClipping(boolean clipping) {
		this.noClipping = !clipping;
	}
	
	public boolean noMerging() {
		return this.noMerging;
	}
	
	public void setMerging(boolean merging) {
		this.noMerging = !merging;
	}
	
	public boolean handleMatePairsSeperatly() {
		return this.outputMatePairsForward != null 
				&& this.outputMatePairsReverse != null;
	}
	
	public File getMatePairFileForward() {
		return this.outputMatePairsForward;
	}
	
	public File getMatePairFileReverse() {
		return this.outputMatePairsReverse;
	}
	
	public void setMatePairFileForward(File matePairFile) {
		if(!matePairFile.exists()) {
			try {
				matePairFile.createNewFile();
			} catch(Exception ex) {
				throw new RuntimeException("ERROR: Cannot write to file " + matePairFile.getAbsolutePath() + " ! Exiting ...");
			}
		}
		
		if(matePairFile.canWrite())
			this.outputMatePairsForward = matePairFile;
		else {
			throw new RuntimeException("ERROR: Cannot write to file " + matePairFile.getAbsolutePath() + " ! Exiting ...");
		}
	}
	
	public void setMatePairFileReverse(File matePairFile) {
		if(!matePairFile.exists()) {
			try {
				matePairFile.createNewFile();
			} catch(Exception e) {
				throw new RuntimeException("ERROR: Cannot write to file " + matePairFile.getAbsolutePath() + " ! Exiting ...");
			}
		}
		
		if(matePairFile.canWrite())
			this.outputMatePairsReverse = matePairFile;
		else {
			throw new RuntimeException("ERROR: Cannot write to file " + matePairFile.getAbsolutePath() + " ! Exiting ...");
		}
	}
	
	public BufferedReader[] getForwardReadsReader() {
		return this.forwardReadsReader;
	}
	
	public void setForwardReadsReader(File[] forwardReads) throws Exception {
		
		this.forwardReadsReader = new BufferedReader[forwardReads.length];
		
		for(int i = 0; i < forwardReads.length; i++) {
			if(!forwardReads[i].canRead()) {
				throw new RuntimeException("ERROR: Cannot read from file " + forwardReads[i].getAbsolutePath() + " ! Exiting ...");
			}
			
			if(forwardReads[i].getName().endsWith(".gz")) {
				InputStream gzStream = new GZIPInputStream(new FileInputStream(forwardReads[i]));
				Reader decoder = new InputStreamReader(gzStream);
				this.forwardReadsReader[i] = new BufferedReader(decoder);
			} else {
				this.forwardReadsReader[i] = new BufferedReader(new FileReader(forwardReads[i]));
			}
		}
		this.forwardReads = forwardReads;
	}
	
	public BufferedReader[] getReverseReadsReader() {
		return this.reverseReadsReader;
	}
	
	public void setReverseReadsReader(File[] reverseReads) throws Exception {
		
		this.reverseReadsReader = new BufferedReader[reverseReads.length];
		
		for(int i = 0; i < reverseReads.length; i++) {
			if(!reverseReads[i].canRead()) {
				throw new RuntimeException("ERROR: Cannot read from file " + reverseReads[i].getAbsolutePath() + " ! Exiting ...");
			}
			
			if(reverseReads[i].getName().endsWith(".gz")) {
				InputStream gzStream = new GZIPInputStream(new FileInputStream(reverseReads[i]));
				Reader decoder = new InputStreamReader(gzStream);
				this.reverseReadsReader[i] = new BufferedReader(decoder);
			} else {
				this.reverseReadsReader[i] = new BufferedReader(new FileReader(reverseReads[i]));
			}
		}
		this.reverseReads = reverseReads;
	}
	
	public long getBytesToProcess() throws IOException {
		
		long fwdBytes = 0;
		long rvsBytes = 0;
		
		if(noTimeEstimation) {
			return 0;
		}
		
		synchronized (logWriter) {
			logWriter.write("# Estimating the input file sizes ...");
			logWriter.newLine();
			logWriter.write("# Note that for gzipped input files, this can take a long time!");
			logWriter.newLine();
			logWriter.newLine();
			logWriter.flush();
		}
		
		for(int i = 0; i < forwardReads.length; i++) {
			if(forwardReads[i].getName().endsWith(".gz")) {
				try {
					GZIPInputStream gzStream = new GZIPInputStream(new FileInputStream(forwardReads[i]));
					long skipped = 0;
					while(gzStream.available() == 1) {
						skipped += gzStream.skip(10000000);
					}
					fwdBytes = skipped;
					gzStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				fwdBytes = forwardReads[i].length();
			}
		}
		
		for(int i = 0; i < reverseReads.length; i++) {
			if(reverseReads[i].getName().endsWith(".gz")) {
				try {
					GZIPInputStream gzStream = new GZIPInputStream(new FileInputStream(reverseReads[i]));
					long skipped = 0;
					while(gzStream.available() == 1) {
						skipped += gzStream.skip(10000000);
					}
					rvsBytes = skipped;
					gzStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				rvsBytes = reverseReads[i].length();
			}
		}
		
		return fwdBytes + rvsBytes;
	}
	
	public BufferedWriter getOutputWriter() {
		return this.outputWriter;
	}
	
	public boolean removeSingleReads() {
		return this.removeSingleReads;
	}
	
	public void setRemoveSingleReads(boolean remove) {
		this.removeSingleReads = remove;
	}

	public boolean showClippingStats() {
		return this.clippingStats;
	}

	public boolean qualityTrimming() {
		return this.qualityTrimming;
	}
	
	public void setQualityTrimming(boolean qualityTrimming) {
		this.qualityTrimming = qualityTrimming;
	}
	
	public void setMinBaseQuality(int qual) {
		this.minBaseQual = qual;
	}
	
	public int getMinBaseQuality() {
		return this.minBaseQual;
	}

	public int getQualityEncoding() {
		return this.qualityEncoding ;
	}

	public void setQualityEncoding(int qualityEncoding) {
		this.qualityEncoding = qualityEncoding;
	}

	public void setNoTimeEstimation(boolean noTimeEstimation) {
		this.noTimeEstimation = noTimeEstimation;
	}

	public void setVerbose(boolean b) {
		this.verbose = b;
	}
	
	public boolean verbose() {
		return this.verbose;
	}

	public boolean timeEstimation() {
		return !noTimeEstimation;
	}

	public int maxReadsPerQueue() {
		return this.maxReadsPerQueue;
	}

	public boolean getQualityBasedMM() {
		return this.qualityBasedMM;
	}

	public void setLogFile(String logFile) throws Exception {
		File f = new File(logFile);
		
		if(!f.exists()) {
			try {
				f.createNewFile();
			} catch(Exception e) {
				throw new RuntimeException("ERROR: Cannot write to file " + f.getAbsolutePath() + " ! Exiting ...");
			}
		}
		
		if(!f.canWrite()) {
			throw new RuntimeException("ERROR: Cannot write to file " + f.getAbsolutePath() + " ! Exiting ...");
		}
		
		this.logWriter = new BufferedWriter(new FileWriter(f));
	}

	public BufferedWriter getLogWriter() {
		return this.logWriter;
	}
}
