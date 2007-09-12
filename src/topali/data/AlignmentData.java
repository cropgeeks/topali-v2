// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.*;
import java.io.*;
import java.util.LinkedList;

/* Represents an Alignment and the results/analyses run upon it. */
public class AlignmentData extends DataObject implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2631590265549224643L;

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

	//public AlignmentData(int id) {
	//	super(id);
	//}
	
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
	
	public boolean isPartitionCodons() {
		return (activeRegionE-activeRegionS+1)%3==0;
	}
	
	public LinkedList<int[]> containsPartitionStopCodons() {
		LinkedList<int[]> pos = new LinkedList<int[]>();
		
		int[] index = sequenceSet.getSelectedSequences();
		for(int i=0; i<index.length; i++) {
			Sequence seq = sequenceSet.getSequence(index[i]);
			String part = seq.getPartition(activeRegionS, activeRegionE);
			for(int j=0; j<part.length(); j+=3) {
				String tmp = part.substring(j, (j+3)).toLowerCase();
				if(tmp.equals("taa") || tmp.equals("tga") || tmp.equals("tag"))
					pos.add(new int[]{i+1, j});
			}
		}
		
		return pos;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((sequenceSet == null) ? 0 : sequenceSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AlignmentData other = (AlignmentData) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sequenceSet == null)
		{
			if (other.sequenceSet != null)
				return false;
		} else if (!sequenceSet.equals(other.sequenceSet))
			return false;
		return true;
	}
	
	public void merge(AlignmentData data) {
		
		//merge sequences
		for(Sequence seq : data.getSequenceSet().getSequences()) {
			if(sequenceSet.getSequences().contains(seq)) {
				Sequence match = null;
				for(Sequence tmp : sequenceSet.getSequences()) {
					if(tmp.equals(seq)) {
						match = tmp;
						break;
					}
				}
				match.setName(seq.getName());
				match.setSequence(seq.getSequence());
			}
			else
			{
				sequenceSet.addSequence(seq);
			}
		}
		
		//merge results
		for(AnalysisResult res : data.getResults()){
			if(results.contains(res)) {
				//TODO: match names
			}
			else {
				addResult(res);
			}
		}
		
		//TODO: merge annotations
	}
}