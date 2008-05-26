// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.*;
import java.io.File;
import java.util.LinkedList;
import topali.data.annotations.AnnotationList;

/* Represents an Alignment and the results/analyses run upon it. */
public class AlignmentData extends DataObject
{
	// The alignment's name
	private String name;

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
	private AnnotationList annotations = new AnnotationList();

	// For marking the current selected Region.
	
	private int activeRegionS, activeRegionE;

	public AlignmentData()
	{
		super();
	}

	public AlignmentData(int id) {
		super(id);
	}
	
	public AlignmentData(String name, SequenceSet sequenceSet)
	{
		this();
		this.name = name;
		this.sequenceSet = sequenceSet;
		activeRegionS = 1;
		activeRegionE = sequenceSet.getLength();

		//topaliAnnotations = new TOPALiAnnotations();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "name", oldName, name));
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

	public AnnotationList getAnnotations() {
		return annotations;
	}

	public void setAnnotations(AnnotationList annotations) {
		this.annotations = annotations;
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
		stat.isDna = ss.getProps().isNucleotides();
		stat.fileSize = new File(path).length();

		refs.add(stat);
	}

	public void setActiveRegion(int start, int end)
	{
		int oldS = this.activeRegionS;
		int oldE = this.activeRegionE;
		
		if (start == -1 || end == -1)
		{
			start = 1;
			end = sequenceSet.getLength();
		}
		
		this.activeRegionS = start;
		this.activeRegionE = end;
		
		for(PropertyChangeListener l : changeListeners) 
			l.propertyChange(new PropertyChangeEvent(this, "activeRegion", oldS+","+oldE, start+""+end));
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
	
	public void setActiveRegionS(int i) {
		this.activeRegionS = i;
	}
	
	public void setActiveRegionE(int i) {
		this.activeRegionE = i;
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
	
	
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((sequenceSet == null) ? 0 : sequenceSet.hashCode());
		return result;
	}

	
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
	
	public void merge(AlignmentData data ) {
		this.name = data.name;
		
		sequenceSet.merge(data.getSequenceSet());
		
		for(AnalysisResult res : data.getResults()) {
			boolean found = false;
			for(AnalysisResult thisRes : results) {
				if(thisRes.getID()==res.getID()) {
					thisRes.guiName = res.guiName;
					found = true;
				}
			}
			if(!found)
				addResult(res);
		}
	}
}