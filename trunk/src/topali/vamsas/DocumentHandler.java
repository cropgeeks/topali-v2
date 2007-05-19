package topali.vamsas;

import java.util.*;

import topali.data.*;

import uk.ac.vamsas.client.*;
import uk.ac.vamsas.objects.core.*;

class DocumentHandler
{
	private ObjectMapper vMap;
	private IClientDocument cdoc;
	
	DocumentHandler(ObjectMapper vMap, IClientDocument cdoc)
	{
		this.vMap = vMap;
		this.cdoc = cdoc;
		
		vMap.registerClientDocument(cdoc);
	}
	
	void writeToDocument(LinkedList<AlignmentData> tDataSets)
		throws Exception
	{
		// For each dataset in the current TOPALi project...
		for (AlignmentData tAlignmentData: tDataSets)
			writeAlignmentData(tAlignmentData);
	}
	
	// Given an existing TOPALi dataset, attempts to either find it within the
	// vamsas document, or creates a new vamsas DataSet to link it with, and
	// adds that to the document instead.
	private void writeAlignmentData(AlignmentData tAlignmentData)
		throws Exception
	{
		VAMSAS root = null;
		
		// Do we have an existing mapping between T/V for this object?
		DataSet vDataSet = (DataSet) vMap.getVamsasObject(tAlignmentData);
	
		if (vDataSet == null)
		{
			System.out.println("Dataset is null - making a new one");
			
			root = cdoc.getVamsasRoots()[0];
			
			// Create a new vamsas data set
			vDataSet = new DataSet();
			root.addDataSet(vDataSet);
			
			// Link it with the TOPALi data set
			vMap.registerObjects(tAlignmentData, vDataSet);
			
			// TODO: speak to Jim about this?
			vDataSet.setProvenance(getDummyProvenance());
		}
		else
		{
			root = (VAMSAS) vDataSet.getV_parent();
			
			System.out.println("Found existing dataset in vamsas");
		}
		
		
//		SequenceSet tSequenceSet = tAlignmentData.getSequenceSet();
//		writeAlignmentSequences(tSequenceSet, vDataSet, tAlignmentData.name);
	}
	
	private void writeAlignmentSequences(SequenceSet tSequenceSet, DataSet vDataSet, String alignmentName)
	{
		// Do we have an existing mapping between T/V for this object?
		Alignment vAlignment = (Alignment) vMap.getVamsasObject(tSequenceSet);
		
		if (vAlignment == null)
		{
			// Create a new vamsas data set
			vAlignment = new Alignment();
			
			// Link it with the TOPALi data set
			vMap.registerObjects(tSequenceSet, vAlignment);
			
			vAlignment.setProvenance(getDummyProvenance("added"));
			vAlignment.setGapChar("-");
			
			Property title = new Property();
			title.setName("title");
			title.setType("string");
			title.setContent(alignmentName);
			vAlignment.addProperty(title);
			
			// TODO: Add sequences
			
			vDataSet.addAlignment(vAlignment);
		}
	}
	
	
	void readFromDocument()
	{
		// TODO!!!
	}
	
	
	
	Provenance getDummyProvenance()
	{
		return getDummyProvenance(null);
	}
	
	Provenance getDummyProvenance(String action)
	{
		Provenance p = new Provenance();
		p.addEntry(getDummyEntry(action));
		
		return p;
	}
	
	Entry getDummyEntry(String action)
	{
		Entry e = new Entry();
		
		e.setApp(VamsasManager.app.getClientUrn());
		e.setUser(VamsasManager.user.getFullName());
		e.setDate(new org.exolab.castor.types.Date(new Date()));
		
		if (action != null)
			e.setAction(action);
		else
			e.setAction("created.");
		
		return e;
	}
}