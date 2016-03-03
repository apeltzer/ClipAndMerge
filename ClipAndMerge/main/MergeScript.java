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

package main;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.kohsuke.args4j.spi.DoubleOptionHandler;
import org.kohsuke.args4j.spi.IntOptionHandler;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import setting.MergeSettings;
import threads.ForwardClipperThread;
import threads.MergeThread;
import threads.ReverseClipperThread;

/**
 * Clip & Merge script
 * @author guenter jaeger
 * @version 1.7.3
 * 
 * This script allows one to clip adapters from paired reads and 
 * to merge the clipped reads into a single read if they overlap
 * significantly, e.g. at least 10 nucleotides with at most 5% 
 * error rate
 * 
 * In addition quality trimming can be performed for all non-merged reads
 *
 */
public class MergeScript {
	
	public static final String VERSION = "1.7.3";
	public static final String TITLE = "ClipAndMerge (v. " + VERSION +")";
	
	public long startTime;
	public long endTime;
	
	public long bytesProcessed;
	public long bytesToProcess;
	
	private double x = 0;
	private MergeSettings settings;
	
	//define options
	
	@Option(name="-in1", handler=StringArrayOptionHandler.class, required=true, usage="Forward reads input file(s) in fastq(.gz) file format.")
	private List<String> inForward;
	
	@Option(name="-in2", handler=StringArrayOptionHandler.class, required=false, usage="Reverse reads input file(s) in fastq(.gz) file format.")
	private List<String> inReverse;
	
	@Option(name="-f", required=false, usage="Forward reads adapter sequence.", metaVar="FORWARD_ADAPTER_STRING")
	private String forwardAdapter = "AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC";
	
	@Option(name="-r", required=false, usage="Reverse reads adapter sequence.", metaVar="REVERSE_ADAPTER_STRING")
	private String reverseAdapter = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA";
	
	@Option(name="-h", required=false, usage="Display this help page and exit.", handler=BooleanOptionHandler.class)
	private boolean displayHelp = false;
	
	@Option(name="-o", required=false, metaVar="OUTPUT_FILE_STRING", usage="Output file. If no file is provided, output will be written to System.out. If file ends with \'.gz', output will be gzipped.")
	private String outputFile = null;
	
	@Option(name="-maxParallelReads", required=false, metaVar="NUM_READS_INTEGER", usage="Maximal number of reads, that can be processed in parallel. This number largely depends on the processing system settings! Only change it if you know what you are doing!")
	private int maxParallelReads = 1000;
	
	@Option(name="-timeEstimation", required=false, usage="Perform remaining time estimation. Note: this can take long for large gzipped input files.", handler=BooleanOptionHandler.class)
	private boolean timeEstimation = false;
	
	@Option(name="-verbose", required=false, usage="Print additional processing information", handler=BooleanOptionHandler.class)
	private boolean verbose = false;
	
	@Option(name="-l", metaVar="INTEGER", required=false, usage="Discard sequences shorter than this number of nucleotides after adapter clipping.", handler=IntOptionHandler.class)
	private int minReadLength = 25;
	
	@Option(name="-n", required=false, usage="Discard sequences with unknown (N) nucleotides. Default is to keep such sequences.", handler=BooleanOptionHandler.class)
	private boolean discardReadsWithNs = false;
	
	@Option(name="-m", metaVar="INTEGER", required=false, usage="Require a minimum adapter alignment length. If less nucleotides align with the adapter, the sequences are not clipped.", handler=IntOptionHandler.class)
	private int minAdapterAlignmentLength = 8;
	
	@Option(name="-no_clip_stats", required=false, usage="Disable the display of clipping statistics.", handler=BooleanOptionHandler.class)
	private boolean noClipStats = false;
	
	@Option(name="-p", metaVar="INTEGER", required=false, usage="Minimal number of nucleotides that have to overlap in order to merge the forward and reverse read.", handler=IntOptionHandler.class)
	private int minMergeOverlap = 10;
	
	@Option(name="-e", metaVar="DOUBLE", required=false, usage="Error rate for merging forward and reverse reads. A value of 0.05 means that 5% mismatches are allowed in the overlap region.", handler=DoubleOptionHandler.class)
	private double errorRateMerging = 0.05;
	
	@Option(name="-no_qbMM", required=false, usage="Do not perform quality based mismatch calculation for merging. Default is to take quality scores into account.", handler=BooleanOptionHandler.class)
	private boolean noQualityBasedMMCalc = false;
	
	@Option(name="-u", metaVar="FORWARD_FILE REVERSE_FILE", required=false, usage="Write unmerged forward and reverse reads to extra files. Unmerged forward reads are written to the file 'FORWARD_FILE'. Unmerged reverse reads are written to the file 'REVERSE_FILE', i.e. the regular output file then only contains merged reads!"
				+ "\nAttention: If the option '-rm_no_partner' is not selected the two given output files also contain forward/reverse reads with no pairing partner!"
				+ "\nIf filenames end with '.gz' gzipped output is produced!", handler=StringArrayOptionHandler.class)
	private List<String> unmergedOutputFiles = null;
	
	@Option(name="-no_clipping", required=false, usage="Skip adapter clipping. Only read merging is performed! "
			+ "(This is only recommended if every forward and reverse read has a corresponding partner in the other respective fastq-file! "
			+ "Otherwise merging can not be performed correctly.", handler=BooleanOptionHandler.class)
	private boolean noClipping = false;
	
	@Option(name="-no_merging", required=false, usage="Skip read merging for paired-end sequencing data! Only adapter clipping is performed. This parameter is not needed for single-end data.", handler=BooleanOptionHandler.class, depends={"-u"})
	private boolean noMerging = false;
	
	@Option(name="-rm_no_partner", required=false, usage="Remove reads with no pairing partner after adapter clipping.", handler=BooleanOptionHandler.class)
	private boolean rmNoPartner = false;
	
	@Option(name="-qt", required=false, usage="Enable quality trimming for non-merged reads.", handler=BooleanOptionHandler.class)
	private boolean qualityTrimmingNonMerged = true;
	
	@Option(name="-q", metaVar="INTEGER", required=false, usage="Minimum base quality for quality trimming.", handler=IntOptionHandler.class)
	private int minQualTrim = 20;
	
	@Option(name="-qo", metaVar="INTEGER", required=false, usage="Phred Score offset.", handler=IntOptionHandler.class)
	private int phredScoreOffset = 33;
	
	@Option(name="-discardBadReads", required=false, usage="Discard reads after merging that do not fulfill the quality criteria.", handler=BooleanOptionHandler.class)
	private boolean discardBadReads = false;
	
	@Option(name="-qualFreqBadReads", metaVar="DOUBLE", required=false, usage="Percentage of reads that have to fulfill minimal base quality criterion.", handler=DoubleOptionHandler.class)
	private double goodQualFreq = 0.9;
	
	@Option(name="-minQualBadReads", metaVar="INTEGER", required=false, usage="Minimal base quality for keeping bad reads. If 0 is specified, then all reads are kept.", handler=IntOptionHandler.class)
	private int minQualBadReads = 0;
	
	@Option(name="-log", metaVar="LOG_FILE_STRING", required=false, usage="Write log messages to a file instead of the standard error stream.")
	private String logFile = null;
	
	@Option(name="-trim3p", metaVar="INTEGER", required=false, usage="Trim N nucleotides from the 3' end of each read. This step is performed after adapter clipping. Reverse reads are not reverse trancriped before trimming.", handler=IntOptionHandler.class)
	private int trim3p = 0;
	
	@Option(name="-trim5p", metaVar="INTEGER", required=false, usage="Trim N nucleotides from the 5' end of each read. This step is performed after adapter clipping. Reverse reads are not reverse transcriped before trimming.", handler=IntOptionHandler.class)
	private int trim5p = 0;
	
	@Option(name="-lastBase", metaVar="INTEGER", required=false, usage="Reads are trimmed from the 3' end until given value is reached. Trimming is not performed if read is already <= given value. If this option is given the '-trim3p' option is disregarded! Given value sould be 1-based!", handler=IntOptionHandler.class)
	private int lastBase = Integer.MAX_VALUE;
	
	/**
	 * estimates the remaining time in minutes
	 * @throws IOException
	 */
	public void reportRemainingTime() throws IOException {
		long currentTime = System.currentTimeMillis();
		double timeElapsed = Math.round(((currentTime-startTime)/(1000.*60.))*10000.)/10000.;
		double fractionProcessed = (double)bytesProcessed / (double)bytesToProcess;
		double fractionToProcess = 1 - fractionProcessed;
		double factor = fractionToProcess / fractionProcessed;
		
		double percProcessed = Math.round(fractionProcessed*100);
		
		if(fractionProcessed > 0 && fractionProcessed <= 1) {
			double timeRemaining = Math.round((timeElapsed * factor)*100.)/100.;
			if(timeRemaining > 0) {
				BufferedWriter logWriter = settings.getLogWriter();
				synchronized(logWriter) {
					logWriter.write("# Percentage of the data processed: " + Double.toString(percProcessed) + "%");
					logWriter.newLine();
					
					logWriter.write("#\tEstimated time remaining: " + Double.toString(timeRemaining) + " minutes");
					logWriter.newLine();
					
					logWriter.flush();
				}
			}
		}
	}
	
	/**
	 * reports the total runtime of the script after finishing all tasks
	 * @throws IOException 
	 */
	public void reportRuntime() throws IOException {
		long endTime = System.currentTimeMillis();
		double timeElapsed = Math.round(((endTime-startTime)/(1000.*60.))*10000.)/10000.;
		BufferedWriter logWriter = settings.getLogWriter();
		synchronized (logWriter) {
			logWriter.write("Time elapsed: " + Double.toString(timeElapsed) + " minutes");
			logWriter.newLine();
		}
	}
	
	private long lastTime = -1;
	
	//this method is used by the merge-thread in order to report the current progress state
	public synchronized void updateProcessBytes(long processedSoFar) throws IOException {
		if(bytesProcessed != processedSoFar) {
			if(settings.verbose()) { //perform additional information about process speed
				if(lastTime == -1) {
					lastTime = System.currentTimeMillis();
				} else {
					long endTime = System.currentTimeMillis();
					double timeElapsed = Math.round(((endTime-lastTime)/(1000.*60.))*10000.)/10000.;
					lastTime = endTime;
					long difference = processedSoFar - bytesProcessed;
					double bytesFraction = Math.round((difference / timeElapsed) * 100.)/100.;
					
					BufferedWriter logWriter = settings.getLogWriter();
					synchronized (logWriter) {
						logWriter.write("# Processing speed: " + Double.toString(bytesFraction) + " bytes/minute");
						logWriter.newLine();
					}
				}
			}
			
			bytesProcessed = processedSoFar;
			double fractionProcessed = (double)bytesProcessed / (double)bytesToProcess;
			
			//at most 1 report every 10% of the data
			if(fractionProcessed >= x && x+0.1 <= 1.1) {
				reportRemainingTime();
				x += 0.1;
			}
		}
	}
	
	public void doMain(String[] args) throws Exception {
		settings = new MergeSettings();
		
		CmdLineParser parser = new CmdLineParser(this);
		parser.getProperties().withUsageWidth(120);
		
		try {
			parser.parseArgument(args);
		} catch (Exception ex) {
			displayHelp(System.err);
			System.err.println(ex.getMessage());
            System.err.println("java -jar ClipAndMerge.jar [options...]");
            System.err.println();
            System.err.println("  Example: java -jar ClipAndMerge.jar"+parser.printExample(OptionHandlerFilter.REQUIRED));
            System.err.println();
            parser.printUsage(System.err);

            return;
		}
		
		File[] forwardReads = new File[inForward.size()];
		for(int i = 0; i < inForward.size(); i++) {
			forwardReads[i] = new File(inForward.get(i));
		}
		settings.setForwardReadsReader(forwardReads);
		
		if(inReverse != null) {
			File[] reverseReads = new File[inReverse.size()];
			for(int i = 0; i < inReverse.size(); i++) {
				reverseReads[i] = new File(inReverse.get(i));
			}
			settings.setReverseReadsReader(reverseReads);
		}
		
		if(displayHelp) {
			displayHelp(System.out);
			System.out.println("  Example: java -jar ClipAndMerge.jar"+parser.printExample(OptionHandlerFilter.REQUIRED));
			System.out.println();
			parser.printUsage(System.out);
		}
		
		if(outputFile != null) {
			settings.setOutputFile(outputFile);
		}
		
		if(logFile != null) {
			settings.setLogFile(logFile);
		}
		
		settings.setForwardAdapter(forwardAdapter);
		settings.setReverseAdapter(reverseAdapter);
		settings.setMinMergeOverlap(minMergeOverlap);
		settings.setMinSequenceLength(minReadLength);
		settings.setKeepSequencesWithN(!discardReadsWithNs);
		settings.setMinAdapterAlignmentLength(minAdapterAlignmentLength);
		settings.setErrorRateForMerging(errorRateMerging);
		settings.setShowClippingStats(!noClipStats);
		settings.setClipping(!noClipping);
		
		if(unmergedOutputFiles != null) {
			String filename_forward = unmergedOutputFiles.get(0);
			String filename_reverse = unmergedOutputFiles.get(1);
			settings.setMatePairFileForward(new File(filename_forward));
			settings.setMatePairFileReverse(new File(filename_reverse));
		}
		
		settings.setMerging(!noMerging);
		settings.setRemoveSingleReads(rmNoPartner);
		settings.setQualityTrimming(qualityTrimmingNonMerged);
		settings.setMinBaseQuality(minQualTrim);
		settings.setQualityEncoding(phredScoreOffset);
		settings.setNoTimeEstimation(!timeEstimation);
		settings.setVerbose(verbose);
		settings.setMaxReadsPerQueue(maxParallelReads);
		settings.setQualityBasedMM(!noQualityBasedMMCalc);
		
		if(discardBadReads) {
			settings.setDiscardBadQualityReadsScore(minQualBadReads);
			settings.setMinGoodQualityBasePercentage(goodQualFreq);
		}
		
		settings.setTrim3P(trim3p);
		settings.setTrim5P(trim5p);
		settings.setLastBaseToKeep(lastBase);
		
		this.clipAndMergeThreaded(settings);
	}

	public static void main(String[] args) throws Exception {
		MergeScript mergeScript = new MergeScript();
		mergeScript.doMain(args);
	}
	
	/**
	 * Initializes the clipping threads as well as the merging thread
	 * Performs clipping and merging.
	 */
	private void clipAndMergeThreaded(MergeSettings settings) throws Exception {
		
		BufferedWriter logWriter = settings.getLogWriter();
		synchronized (logWriter) {
			logWriter.write(TITLE);
			logWriter.newLine();
			logWriter.write("by G\u00FCnter J\u00E4ger");
			logWriter.newLine();
			logWriter.newLine();
			
			logWriter.write("[Parameters]");
			logWriter.newLine();
			logWriter.write("- Skip adapter clipping: " + settings.noClipping());
			logWriter.newLine();
			logWriter.write("- Skip read merging: " + settings.noMerging());
			logWriter.newLine();
			logWriter.write("- Handle mate pairs seperately: " + settings.handleMatePairsSeperatly());
			logWriter.newLine();
			
			logWriter.write("- Keep sequences containing N: "+ settings.keepSequencesWithN());
			logWriter.newLine();
			logWriter.write("- Discard single reads: " + settings.removeSingleReads());
			logWriter.newLine();
			
			logWriter.write("- Minimum sequence length: " + settings.getMinSequenceLength());
			logWriter.newLine();
			logWriter.write("- Minimum merge overlap: " + settings.getMinMergeOverlap());
			logWriter.newLine();
			logWriter.write("- Error Rate for merging: " + settings.getErrorRateForMerging());
			logWriter.newLine();
			logWriter.write("- Perform quality based mismatch calculation for merging: " + settings.getQualityBasedMM());
			logWriter.newLine();
			
			logWriter.write("- Perform quality trimming: " + settings.qualityTrimming());
			logWriter.newLine();
			logWriter.write("- Minimum base quality for quality trimming: " + settings.getMinBaseQuality());
			logWriter.newLine();
			logWriter.write("- Phred Score Offset: " + settings.getQualityEncoding());
			logWriter.newLine();
			
			logWriter.write("- Discard bad quality reads: " + settings.discardBadQualityReads());
			logWriter.newLine();
			logWriter.write("- Minimal good quality percentage: " + settings.getMinGoodQualityBasePercentage());
			logWriter.newLine();
			
			logWriter.newLine();
			logWriter.write("- Minimal base quality for keeping reads: " + settings.getDiscardBadQualityReadsScore());
			logWriter.newLine();
			
			logWriter.newLine();
			logWriter.write("- Trim 5' bases: " + settings.getTrim5P());
			logWriter.newLine();
			logWriter.write("- Trim 3' bases: " + settings.getTrim3P());
			logWriter.newLine();
			logWriter.write("- Last base to keep when trimming is performed: " + settings.getLastBaseToKeep());
			logWriter.newLine();
			
			logWriter.newLine();
			logWriter.write("- Maximal number of reads processed in parallel: " + settings.maxReadsPerQueue());
			logWriter.newLine();
			logWriter.write("- Time Estimation: " + settings.timeEstimation());
			logWriter.newLine();
			logWriter.write("- Verbose: " + settings.verbose());
			logWriter.newLine();
			logWriter.newLine();
		}
		
		bytesToProcess = settings.getBytesToProcess();
		
		if(bytesToProcess > 0) {
			synchronized (logWriter) {
				logWriter.write("# Bytes to process: " + Long.toString(bytesToProcess));
				logWriter.newLine();
			}
		}
		
		startTime = System.currentTimeMillis();
		
		MergeThread merger = new MergeThread(this, settings);
	
		ForwardClipperThread fCThread = new ForwardClipperThread(merger, settings);
		ReverseClipperThread rCThread = new ReverseClipperThread(merger, settings);
		
		if(settings.noClipping()) {
			synchronized (logWriter) {
				logWriter.write("Skipping adapter clipping.");
				logWriter.newLine();
			}
		}
		
		if(settings.noMerging()) {
			synchronized (logWriter) {
				logWriter.write("Skipping read merging.");
				logWriter.newLine();
			}
		}
		
		//start threads
		merger.start();
		fCThread.start();
		rCThread.start();
		
		//wait for threads to finish
		merger.join();
		fCThread.join();
		rCThread.join();
		
		//clean up everything after the calculations are finished
		merger = null;
		fCThread = null;
		rCThread = null;
		
		reportRuntime();
		logWriter.close();
	}
	
	/**
	 * displays a  help menu describing all possible parameters
	 * @param stream 
	 */
	public static void displayHelp(PrintStream stream) {
		stream.println(TITLE);
		stream.println("Integrative Transcriptomics");
		stream.println("University of T\u00FCbingen");
		stream.println();
		stream.println("Author: G\u00FCnter J\u00E4ger");
		stream.println();
		stream.println("This tool clips adapters from fastq sequences and merges overlapping regions from forward and reverse reads.");
		stream.println("Input sequences are accepted in fastq, or in gzipped fastq format.");
		stream.println();
	}
}