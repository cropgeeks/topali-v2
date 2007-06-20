// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.*;
import java.io.File;
import java.util.LinkedList;

/* Represents an Alignment and the results/analyses run upon it. */
public class AlignmentData extends ViewableDataObject
{
	// The alignment's name
	public String name;

	// And the data for it
	private SequenceSet sequenceSet;

	// This object can also be used to reference a list of alignments
	// (not held in memory)
	private LinkedList<AlignmentFileStat> refs = new LinkedList<AlignmentFileStat>();

	private boolean isReferenceList = false;

	// All the AnalysisResults created and associated with this alignment
	private LinkedList<AnalysisResult> results = new LinkedList<AnalysisResult>();

	// And the tracker to mark how many of each has been run
	private ResultsTracker tracker = new ResultsTracker();

	// A set of alignment annotations (partitions, coding regions, etc)
	private TOPALiAnnotations topaliAnnotations = new TOPALiAnnotations();

	// For marking the current selected Region.
	private int activeRegionS, activeRegionE;

	public AlignmentData()
	{
	}

	public AlignmentData(String name, SequenceSet sequenceSet)
	{
		this.name = name;
		this.sequenceSet = sequenceSet;
		activeRegionS = 1;
		activeRegionE = sequenceSet.getLength();

		topaliAnnotations = new TOPALiAnnotations();
	}

	public SequenceSet getSequenceSet()
	{
		return sequenceSet;
	}

	public void setSequenceSet(SequenceSet sequenceSet)
	{
		this.sequenceSet = sequenceSet;
		this.activeRegionS = 1;
		this.activeRegionE = sequenceSet.getLength();
	}

	public LinkedList<AlignmentFileStat> getReferences()
	{
		return refs;
	}

	public void setReferences(LinkedList<AlignmentFileStat> refs)
	{
		this.refs = refs;
	}

	public boolean isReferenceList()
	{
		return isReferenceList;
	}

	public void setIsReferenceList(boolean isReferenceList)
	{
		this.isReferenceList = isReferenceList;
	}

	public void setTopaliAnnotations(TOPALiAnnotations topaliAnnotations)
	{
		this.topaliAnnotations = topaliAnnotations;
	}

	public TOPALiAnnotations getTopaliAnnotations()
	{
		return topaliAnnotations;
	}

	public LinkedList<AnalysisResult> getResults()
	{
		return results;
	}

	public void setResults(LinkedList<AnalysisResult> results)
	{
		this.results = results;
	}

	public void replaceResult(AnalysisResult oldR, AnalysisResult newR)
	{
		int index = results.indexOf(oldR);
		results.remove(index);
		results.add(index, newR);
		
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "result", oldR, newR));
	}

	public void removeResult(AnalysisResult result)
	{
		results.remove(result);
		
		//NavPanel.propertyChange() should be called:
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "result", result, null));
	}

	public void addResult(AnalysisResult result) {
		results.add(result);
		
		//NavPanel.propertyChange() should be called:
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "result", null, result));
	}
	
	public ResultsTracker getTracker()
	{
		return tracker;
	}

	public void setTracker(ResultsTracker tracker)
	{
		this.tracker = tracker;
	}

	public void addReference(String path, SequenceSet ss)
	{
		AlignmentFileStat stat = new AlignmentFileStat(path);

		stat.length = ss.getLength();
		stat.size = ss.getSize();
		stat.isDna = ss.isDNA();
		stat.fileSize = new File(path).length();

		refs.add(stat);
	}

	public void setActiveRegion(int start, int end)
	{
		if (start == -1 || end == -1)
		{
			setActiveRegionS(1);
			setActiveRegionE(sequenceSet.getLength());
		} else
		{
			setActiveRegionS(start);
			setActiveRegionE(end);
		}
	}

	// ------------------
	public int getActiveRegionS()
	{
		return activeRegionS;
	}
	
	public int getActiveRegionE()
	{
		return activeRegionE;
	}

	public void setActiveRegionE(int activeRegionE)
	{
		int oldValue = this.activeRegionE;
		this.activeRegionE = activeRegionE;
		
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "activeRegion", oldValue, activeRegionE));
		
	}

	public void setActiveRegionS(int activeRegionS)
	{
		int oldValue = this.activeRegionS;
		this.activeRegionS = activeRegionS;
		
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "activeRegion", oldValue, activeRegionS));
		
	}

}