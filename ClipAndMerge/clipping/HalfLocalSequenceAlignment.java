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

public class HalfLocalSequenceAlignment extends SequenceAlignment {

	private int highestScoredQueryIndex;
	private int highestScoredTargetIndex;
	
	public boolean startingPointCloseToEndOfSequences(int queryIndex, int targetIndex) {
		if(queryIndex >= querySequence().length() - 2 || targetIndex >= targetSequence().length() - 2) {
			return true;
		} else {
			return false;
		}
	}
	
	public void findAlignmentStartingPoint(int newQueryIndex, int newTargetIndex) {
		double maxScore = score(matrixWidth()-1, matrixHeight()-1);
		
		for(int qIndex = 0; qIndex < matrixWidth(); qIndex++) {
			for(int tIndex = matrixHeight()-2; tIndex < matrixHeight(); tIndex++) {
				if(origin(qIndex, tIndex) > 0 && safeScore(qIndex, tIndex) > maxScore) {
					maxScore = safeScore(qIndex, tIndex);
					newTargetIndex = tIndex;
					newQueryIndex = qIndex;
				}
			}
		}
		
		for(int qIndex = matrixWidth()-2; qIndex < matrixWidth(); qIndex++) {
			for(int tIndex = 0; tIndex < matrixHeight(); tIndex++) {
				if(origin(qIndex, tIndex) > 0 && safeScore(qIndex, tIndex) > maxScore) {
					maxScore = safeScore(qIndex, tIndex);
					newTargetIndex = tIndex;
					newQueryIndex = qIndex;
				}
			}
		}
	}
	
	public SequenceAlignmentResults findOptimalAlignmentFromPoint(int queryStart, int targetStart) {
		SequenceAlignmentResults results = new SequenceAlignmentResults();
		
		results.setQuerySequence(querySequence());
		results.setTargetSequence(targetSequence());
		
		int queryIndex = queryStart;
		int targetIndex = targetStart;
		
		results.setQueryEnd(queryIndex);
		results.setTargetEnd(targetIndex);
		
		while(queryIndex >= 0 && targetIndex >= 0) {
			char qNuc = queryNucleotide(queryIndex);
			char tNuc = targetNucleotide(targetIndex);
			
			int currentOrigin = origin(queryIndex, targetIndex);
			char currentMatch = match(queryIndex, targetIndex);
			
			results.setQueryStart(queryIndex);
			results.setTargetStart(targetIndex);
			
			switch(currentOrigin) {
			case FROM_LEFT:
				results.appendTargetAlignment('-');
				results.appendQueryAlignment(qNuc);
				results.setGaps(results.gaps()+1);
				results.setScore(results.score() + gapPenalty());
				queryIndex--;
				break;
			case FROM_UPPER_LEFT:
				results.appendTargetAlignment(tNuc);
				results.appendQueryAlignment(qNuc);
				switch(currentMatch) {
				case 'N':
					results.setNeutralMatches(results.neutralMatches()+1);
					results.setScore(results.score()+neutralPenalty());
					break;
				case 'M':
					results.setMatches(results.matches()+1);
					results.setScore(results.score() + matchPenalty());
					break;
				case 'x':
					results.setMismatches(results.mismatches()+1);
					results.setScore(results.score()+mismatchPenalty());
					break;
				}
				queryIndex--;
				targetIndex--;
				break;
			case FROM_UPPER:
				results.appendTargetAlignment(tNuc);
				results.appendQueryAlignment('-');
				results.setGaps(results.gaps()+1);
				results.setScore(results.score()+gapPenalty());
				targetIndex--;
				break;
			case FROM_NOWHERE:
				break;
			}
		}
		
		results.setQuerySize(querySequence().length());
		results.setTargetSize(targetSequence().length());
		
		results.reverseTargetAlignment();
		results.reverseQueryAlignment();
		
		return results;
	}
	
	protected void postProcess() {}
	
	protected void findOptimalAlignment() {
		SequenceAlignmentResults results = findOptimalAlignmentFromPoint(highestScoredQueryIndex, highestScoredTargetIndex);
		
		if(results.matches() >= 7 && results.mismatches() == 0 && results.gaps() == 0) {
			this.alignmentResults = results;
			return;
		}
		
		if(startingPointCloseToEndOfSequences(highestScoredQueryIndex, highestScoredTargetIndex)) {
			this.alignmentResults = results;
			return;
		}
		
		int queryIndex = highestScoredQueryIndex;
		int targetIndex = highestScoredTargetIndex;
		
		findAlignmentStartingPoint(queryIndex, targetIndex);
		
		this.alignmentResults = results;
	}
	
	protected void populateMatrix() {
		int origin = FROM_LEFT;
		
		double highestScore = -1000000;
		highestScoredQueryIndex = 0;
		highestScoredTargetIndex = 0;
		
		for(int queryIndex = 0; queryIndex < matrixWidth(); queryIndex++) {
			for(int targetIndex = 0; targetIndex < matrixHeight(); targetIndex++) {
				double upScore = safeScore(queryIndex, targetIndex-1) + gapPenalty();
				double leftScore = safeScore(queryIndex-1, targetIndex) + gapPenalty();
				double upleftScore = safeScore(queryIndex-1, targetIndex-1) + nucleotideMatchScore(queryIndex, targetIndex);
				
				if(targetIndex > 3 && targetIndex-3 > queryIndex) {
					leftScore = -100000;
				}
				
				double score = -100000000;
				
				if(upleftScore > score) {
					score = upleftScore;
					origin = FROM_UPPER_LEFT;
				}
				if(upScore > score) {
					score = upScore;
					origin = FROM_UPPER;
				}
				if(leftScore > score) {
					score = leftScore;
					origin = FROM_LEFT;
				}
				
				scoreMatrix[queryIndex][targetIndex] = score;
				originMatrix[queryIndex][targetIndex] = origin;
				
				if(score > highestScore) {
					highestScoredQueryIndex = queryIndex;
					highestScoredTargetIndex = targetIndex;
					highestScore = score;
				}
			}
		}
	}
	
	protected void resetMatrix(int width, int height) {
		highestScoredQueryIndex = 0;
		highestScoredTargetIndex = 0;
		
		for(int x = 0; x < width; x++) {
			queryBorder[x] = 0;
		}
		
		for(int y = 0; y < height; y++) {
			targetBorder[y] = (y <= 3) ? 0 : (gapPenalty() * (y-3));
		}
	}
	
	protected void setSequences(String query, String target) {
		querySequence = query;
		targetSequence = target;
	}
	
	public void printMatrix() {
		//TODO
	}
	
	protected void populateMatchMatrix() {
		for(int x = 0; x < matrixWidth(); x++) {
			for(int y = 0; y < matrixHeight(); y++) {
				matchMatrix[x][y] = matchValue(queryNucleotide(x), targetNucleotide(y));
			}
		}
	}
	
	protected void resizeMatrix(int width, int height) {
		if(matrixWidth() == width && matrixHeight() == height) {
			return;
		}
		queryBorder = new double[width];
		targetBorder = new double[height];
		scoreMatrix = new double[width][height];
		originMatrix = new int[width][height];
		matchMatrix = new char[width][height];
	}
	
	public SequenceAlignmentResults align(String query, String target) {
		setSequences(query, target);
		resetAlignmentResults();
		resizeMatrix(querySequence().length(), targetSequence().length());
		populateMatchMatrix();
		resetMatrix(matrixWidth(), matrixHeight());
		populateMatrix();
		findOptimalAlignment();
		postProcess();
		return alignmentResults;
	}
	
	protected void resetAlignmentResults() {
		alignmentResults = new SequenceAlignmentResults();
		alignmentResults.setQuerySequence(querySequence());
		alignmentResults.setTargetSequence(targetSequence());
	}
}
