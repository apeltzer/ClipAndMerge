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

package clipping;

public class SequenceAlignmentResults {
	
	private int querySize;
	private int queryStart;
	private int queryEnd;
	
	private int targetSize;
	private int targetStart;
	private int targetEnd;
	
	private int gaps;
	private int neutralMatches;
	private int matches;
	private int mismatches;
	
	private double score;
	
	private StringBuffer queryAlignment;
	private StringBuffer targetAlignment;
	
	private String querySequence;
	private String targetSequence;
	
	public SequenceAlignmentResults() {
		this.querySize = 0;
		this.queryStart = 0;
		this.queryEnd = 0;
		
		this.targetSize = 0;
		this.targetStart = 0;
		this.targetEnd = 0;
		
		this.gaps = 0;
		this.neutralMatches = 0;
		this.matches = 0;
		this.mismatches = 0;
		
		this.score = 0;
		
		this.queryAlignment = new StringBuffer();
		this.targetAlignment = new StringBuffer();
	}
	
	public void print() {
		//TODO
	}
	
	public int querySize() {
		return this.querySize;
	}
	
	public int queryEnd() {
		return this.queryEnd;
	}
	
	public int queryStart() {
		return this.queryStart;
	}
	
	public int targetSize() {
		return this.targetSize;
	}
	
	public int targetStart() {
		return this.targetStart;
	}
	
	public int targetEnd() {
		return this.targetEnd;
	}
	
	public int gaps() {
		return this.gaps;
	}
	
	public int matches() {
		return this.matches;
	}
	
	public int mismatches() {
		return this.mismatches;
	}
	
	public int neutralMatches() {
		return this.neutralMatches;
	}

	public void setQuerySequence(String querySequence) {
		this.querySequence = querySequence;
	}

	public void setTargetSequence(String targetSequence) {
		this.targetSequence = targetSequence;
	}

	public void setQueryEnd(int queryIndex) {
		this.queryEnd = queryIndex;
	}

	public void setTargetEnd(int targetIndex) {
		this.targetEnd = targetIndex;
	}

	public void setQuerySize(int length) {
		this.querySize = length;
	}

	public void setTargetSize(int length) {
		this.targetSize = length;
	}

	public void setQueryStart(int queryIndex) {
		this.queryStart = queryIndex;
	}
	
	public void setTargetStart(int targetIndex) {
		this.targetStart = targetIndex;
	}

	public void appendTargetAlignment(char tNuc) {
		this.targetAlignment.append(tNuc);
	}

	public void appendQueryAlignment(char qNuc) {
		this.queryAlignment.append(qNuc);
	}

	public void setGaps(int gaps) {
		this.gaps = gaps;
	}

	public double score() {
		return this.score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public void setNeutralMatches(int neutralMatches) {
		this.neutralMatches = neutralMatches;
	}

	public void setMatches(int matches) {
		this.matches = matches;
	}

	public void setMismatches(int mismatches) {
		this.mismatches = mismatches;
	}

	public void reverseQueryAlignment() {
		reverse(queryAlignment);
	}
	
	private String reverse(StringBuffer sequence) {
		StringBuffer sb = new StringBuffer(sequence);
		sb.reverse();
		return sb.toString();
	}

	public void reverseTargetAlignment() {
		reverse(targetAlignment);
	}
	
	public String querySequence() {
		return this.querySequence;
	}
	
	public String targetSequence() {
		return this.targetSequence;
	}
}
