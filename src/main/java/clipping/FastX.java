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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class FastX {

	public boolean validateNucleotidesString(String seq) {
		boolean match = true;
		for(int i = 0; i < seq.length(); i++) {
			char c = seq.charAt(i);
			Boolean allowed = table.get(c);
			if(allowed == null)
				 match = false;
		}
		return match;
	}
	
	private boolean allowN;
	private boolean allowU;
	private boolean allowLowercase;
	
	private HashMap<Character, Boolean> table;
	
	public void createLookupTable() {
		table = new HashMap<Character, Boolean>();
		
		table.put('A', true);
		table.put('C', true);
		table.put('G', true);
		table.put('T', true);
		
		if(allowN()) {
			table.put('N', true);
		}
		if(allowU()) {
			table.put('U', true);
		}
		
		if(allowLowercase()) {
			table.put('a', true);
			table.put('c', true);
			table.put('g', true);
			table.put('t', true);
			
			if(allowN()) {
				table.put('n', true);
			}
			if(allowU()) {
				table.put('u', true);
			}
		}
	}
	
	public boolean allowN() {
		return this.allowN;
	}
	
	public boolean allowU() {
		return this.allowU;
	}
	
	public boolean allowLowercase() {
		return this.allowLowercase;
	}
	
	public void convertAsciiQualityScoreLine(String asciiQualityScores) throws Exception {
		
		if(asciiQualityScores.length() != this.nucleotides.length) {
			throw new Exception("Number of quality values doesn't match number of nucleotides!");
		}
		
		for(int i = 0; i < asciiQualityScores.length(); i++) {
			char quality = (char)asciiQualityScores.charAt(i);
			setQuality(i, quality);
		}
	}
	
	private char[] quality;
	private char[] nucleotides;
	
	public String nucleotides() {
		return new String(nucleotides);
	}
	
	public void setQuality(int index, char quality) {
		this.quality[index] = quality;
	}
	
	private BufferedReader br;
	
	public void initReader(InputStream input, boolean allowN, boolean allowU, boolean allowLowercase) throws FileNotFoundException {
		br = new BufferedReader(new InputStreamReader(input));
		setAllowLowercase(allowLowercase);
		setAllowN(allowN);
		setAllowU(allowU);
		
		createLookupTable();
	}
	
	public void setAllowLowercase(boolean allowLowercase) {
		this.allowLowercase = allowLowercase;
	}
	
	public void setAllowN(boolean allowN) {
		this.allowN = allowN;
	}
	
	public void setAllowU(boolean allowU) {
		this.allowU = allowU;
	}
	
	private String name;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String name() {
		return this.name;
	}
	
	public void setNucleotides(String nucleotides) {
		if(this.nucleotides == null || nucleotides.length() != this.nucleotides.length) {
			this.nucleotides = nucleotides.toCharArray();
		} else {
			for(int i = 0; i < this.nucleotides.length; i++) {
				this.nucleotides[i] = nucleotides.charAt(i);
			}
		}
	}
	
	public boolean readNextRecord() throws Exception {
		int count  = 0;
		String line = null;
		while((line = br.readLine()) != null) {
			count++;
			
			switch(count) {
			case 1:
				if(!line.startsWith("@")) {
					throw new Exception("Invalid input: expecting FASTQ prefix character '@'. Is this a valid FASTQ file?");
				}
				setName(line);
				break;
			case 2:
				if(!validateNucleotidesString(line)) {
					throw new Exception("Found invalid nucleotide sequence: " + line);
				}
				setNucleotides(line);
				break;
			case 4:
				//allow only ascii quality scores
				if(line.length() != this.nucleotides.length) {
					System.err.println("Error:");
					System.err.println("Nucleotides:\t" + new String(this.nucleotides));
					System.err.println("Quality:\t" + line);
					throw new Exception("Length of quality value line doesn't match length of nucleotides line!");
				}
				initQuality(line.length());
				convertAsciiQualityScoreLine(line);
				break;
			}
			
			if(count == 4)
				break;
		}
		
		if(count == 4) {
			setNumberInputReads(getReadsCount()+1);
			return true;
		}
			
		
		return false;
	}
	
	private int numInputReads = 0;
	private int numOutputReads = 0;
	
	public void setNumberInputReads(int count) {
		this.numInputReads = count;
	}
	
	public void setNumberOutputReads(int count) {
		this.numOutputReads = count;
	}
	
	public void initQuality(int length) {
		if(quality == null) {
			quality = new char[length];
		} else {
			if(this.quality.length != length) {
				this.quality = new char[length];
			}
		}
	}
	
	public String getAsciiQualString() {
		StringBuffer buf = new StringBuffer(this.quality.length);
		for(int i = 0; i < this.nucleotides.length; i++) {
			buf.append(this.quality[i]);
		}
		return buf.toString();
	}
	
	//we only deal with fastq files, so this is always 1
	public int getReadsCount() {
		return 1;
	}
	
	public int numInputReads() {
		return this.numInputReads;
	}
	
	public int numOutputReads() {
		return this.numOutputReads;
	}

	public void init(boolean allowN, boolean allowU, boolean allowLowercase) {
		setAllowLowercase(allowLowercase);
		setAllowN(allowN);
		setAllowU(allowU);
		
		createLookupTable();
	}

	public void readNextRecord(Read read) throws Exception {
		if(!read.name.startsWith("@")) {
			throw new Exception("Invalid input: expecting FASTQ prefix character '@'. Is this a valid FASTQ file?");
		}
		setName(read.name);

		if(!validateNucleotidesString(read.sequence)) {
			throw new Exception("Found invalid nucleotide sequence: " + read.sequence);
		}
		setNucleotides(read.sequence);

		//allow only ascii quality scores
		if(read.quality.length() != this.nucleotides.length) {
			System.err.println("Error:");
			System.err.println("Nucleotides:\t" + new String(this.nucleotides));
			System.err.println("Quality:\t" + read.quality);
			throw new Exception("Length of quality value line doesn't match length of nucleotides line!");
		}
		initQuality(read.quality.length());
		convertAsciiQualityScoreLine(read.quality);
	}

	public Read getClippedRead() {
		Read clippedRead = new Read(name(), nucleotides(), "+", getAsciiQualString());
		setNumberOutputReads(getReadsCount()+1);
		return clippedRead;
	}
}
