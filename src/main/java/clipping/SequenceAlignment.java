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

public abstract class SequenceAlignment {

	protected SequenceAlignmentResults alignmentResults;
	protected String querySequence;
	protected String targetSequence;
	
	public static final int FROM_UPPER = 1;
	public static final int FROM_LEFT = 2;
	public static final int FROM_UPPER_LEFT = 3;
	public static final int FROM_NOWHERE = 4;
	
	private double gapPenalty;
	private double matchPenalty;
	private double mismatchPenalty;
	private double neutralPenalty;
	
	protected double[][] scoreMatrix;
	protected int[][] originMatrix;
	protected char[][] matchMatrix;
	
	protected double[] queryBorder;
	protected double[] targetBorder;
	
	public SequenceAlignment() {
		gapPenalty = -5;
		matchPenalty = 1;
		mismatchPenalty = -1;
		neutralPenalty = 0.1;
	}
	
	public int matrixWidth() {
		if(scoreMatrix == null)
			return 0;
		return scoreMatrix.length;
	}
	
	public int matrixHeight() {
		if(scoreMatrix == null)
			return 0;
		return scoreMatrix[0].length;
	}
	
	public double gapPenalty() {
		return this.gapPenalty;
	}
	
	public double matchPenalty() {
		return this.matchPenalty;
	}
	
	public double mismatchPenalty() {
		return this.mismatchPenalty;
	}
	
	public double neutralPenalty() {
		return this.neutralPenalty;
	}
	
	public String querySequence() {
		return this.querySequence;
	}
	
	public String targetSequence() {
		return this.targetSequence;
	}
	
	public char queryNucleotide(int queryIndex) {
		return querySequence.charAt(queryIndex);
	}
	
	public char targetNucleotide(int targetIndex) {
		return targetSequence.charAt(targetIndex);
	}
	
	public SequenceAlignmentResults results() {
		return this.alignmentResults;
	}
	
	public char matchValue(char q, char t) {
		if(q=='N' || t=='N') {
			return 'N';
		}
		return q==t ? 'M' : 'x';
	}
	
	public char match(int queryIndex, int targetIndex) {
		return matchMatrix[queryIndex][targetIndex];
	}
	
	public int origin(int queryIndex, int targetIndex) {
		return originMatrix[queryIndex][targetIndex];
	}
	
	public double score(int queryIndex, int targetIndex) {
		return scoreMatrix[queryIndex][targetIndex];
	}
	
	public double safeScore(int queryIndex, int targetIndex) {
		if(queryIndex == -1 && targetIndex == -1) {
			return 0;
		}
		if(queryIndex == -1) {
			return targetBorder[targetIndex];
		}
		if(targetIndex == -1) {
			return queryBorder[queryIndex];
		}
		return scoreMatrix[queryIndex][targetIndex];
	}
	
	public double nucleotideMatchScore(int queryIndex, int targetIndex) {
		char q = queryNucleotide(queryIndex);
		char t = targetNucleotide(targetIndex);
		
		if(q=='N' && t=='N') {
			return 0.0;
		}
		
		if(q=='N' || t=='N') {
			return neutralPenalty();
		}
		
		return q==t ? matchPenalty() : mismatchPenalty();
	}
	
	abstract public void printMatrix();
	
	abstract public SequenceAlignmentResults align(String query, String target);
	
	abstract protected void resizeMatrix(int width, int height);
	
	abstract protected void populateMatchMatrix();
	
	abstract protected void resetAlignmentResults();
	
	abstract protected void setSequences(String query, String target);
	
	abstract protected void resetMatrix(int width, int height);
	
	abstract protected void populateMatrix();
	
	abstract protected void findOptimalAlignment();
	
	abstract protected void postProcess();
}
