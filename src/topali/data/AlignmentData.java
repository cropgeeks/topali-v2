// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

/* Represents an Alignment and the results/analyses run upon it. */
public class AlignmentData
{
	// The alignment's name
	public String name;
			
	// And the data for it
	private SequenceSet sequenceSet;
		
	// All the AnalysisResults created and associated with this alignment
	private LinkedList<AnalysisResult> results = new LinkedList<AnalysisResult>();
	// And the tracker to mark how many of each has been run
	private ResultsTracker tracker = new ResultsTracker();
		
	// A set of alignment annotations (partitions, coding regions, etc)
	private TOPALiAnnotations topaliAnnotations = new TOPALiAnnotations(1);

	
	public AlignmentData()
	{
	}
	
	public AlignmentData(String name, SequenceSet sequenceSet)
	{
		this.name = name;
		this.sequenceSet = sequenceSet;
		
		topaliAnnotations = new TOPALiAnnotations(sequenceSet.getLength());
	}
	
	public SequenceSet getSequenceSet()
		{ return sequenceSet; }
	
	public void setSequenceSet(SequenceSet sequenceSet)
		{ this.sequenceSet = sequenceSet; }
	
	public void setTopaliAnnotations(TOPALiAnnotations topaliAnnotations)
		{ this.topaliAnnotations = topaliAnnotations; }
	
	public TOPALiAnnotations getTopaliAnnotations()
		{ return topaliAnnotations; }
	
	public LinkedList<AnalysisResult> getResults()
		{ return results; }
	
	public void setResults(LinkedList<AnalysisResult> results)
		{ this.results = results; }
	
	
	public void replaceResult(AnalysisResult oldR, AnalysisResult newR)
	{
		int index = results.indexOf(oldR);
		
		System.out.println("Index is " + index);
		
		results.remove(index);
		results.add(index, newR);
	}
	
	public void removeResult(AnalysisResult result)
		{ results.remove(result); }
	
	public ResultsTracker getTracker()
		{ return tracker; }
	public void setTracker(ResultsTracker tracker)
		{ this.tracker = tracker; }
}