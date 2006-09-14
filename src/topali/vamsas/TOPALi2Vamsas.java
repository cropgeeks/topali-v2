package topali.vamsas;

import java.util.*;

import org.vamsas.objects.core.*;
import org.vamsas.objects.utils.*;

class TOPALi2Vamsas
{
	private VAMSAS vVAMSAS = null;
	private boolean isUpdating = false;
	
	// TODO: ASSUME for now that we're only ever dealing with a single dataset
	// and a single alignment (SequenceSet)
	private topali.data.AlignmentData tAlignmentData;
	private topali.data.SequenceSet tSequenceSet;
	
	// Constructor called when no VAMSAS object exists yet
	TOPALi2Vamsas()
	{
		vVAMSAS = new VAMSAS();
	}
	
	// Constructor called when an existing VAMSAS object is available
	TOPALi2Vamsas(VAMSAS vVAMSAS)
	{
		this.vVAMSAS = vVAMSAS;
		
		isUpdating = true;
	}
	
	VAMSAS createVAMSAS(topali.data.AlignmentData tAlignmentData)
	{
		this.tAlignmentData = tAlignmentData;
		this.tSequenceSet = tAlignmentData.getSequenceSet();
		
		vVAMSAS.addDataSet(createDataSet());
		
		return vVAMSAS;
	}
	
	DataSet createDataSet()
	{
		DataSet vDataSet = new DataSet();
		// Does this dataset already exist?
		
		// Sequences are only added when we add AlignmentSequences
		vDataSet.addAlignment(createAlignment(vDataSet));
		
		// TODO: id - optional - TOPALi ignores trees and annotations at the
		// dataset level as we're only interested in alignments.
		
		vDataSet.setProvenance(createProvenance());
		return vDataSet;
	}
	
	Alignment createAlignment(DataSet vDataSet)
	{
		Alignment vAlignment = new Alignment();
		
		vAlignment.setGapChar("-");
		vAlignment.setAligned(true);
		vAlignment.setModifiable(false);
		
		// TODO: id - optional
		
		topali.data.SequenceSet tSequenceSet = tAlignmentData.getSequenceSet();
		for (topali.data.Sequence tSequence: tSequenceSet.getSequences())
		{
			// Create (and add) the DataSetSequence first...
			Sequence vSequence = createSequence(tSequence);
			vDataSet.addSequence(vSequence);
			
			// So it can be used as a reference for the AlignmentSequence
			AlignmentSequence alignmentSequence = createAlignmentSequence(vSequence, tSequence);
			vAlignment.addAlignmentSequence(alignmentSequence);
		}
		
		// Results annotations
		for (topali.data.AnalysisResult tResult: tAlignmentData.getResults())
		{
//			if (tResult instanceof topali.data.HMMResult)
//				for (int graph = 1; graph <= 3; graph++)	
//					vAlignment.addAlignmentAnnotations(createHMMAnnotations((topali.data.HMMResult)tResult, graph));
					
//			if (tResult instanceof topali.data.DSSResult)
//			{
//				vAlignment.addAlignmentAnnotations(
//					createDSSAnnotations((topali.data.DSSResult)tResult));
//			}
			
			if (tResult instanceof topali.data.TreeResult)
				vAlignment.addTree(createTree((topali.data.TreeResult)tResult));
		}
		
		createAnnotations(vAlignment);
		
		vAlignment.setProvenance(createProvenance());
		return vAlignment;
	}
	
	// Returns a VAMSAS Sequence object
	Sequence createSequence(topali.data.Sequence tSequence)
	{
		Sequence vSequence = new Sequence();
		
		vSequence.setName(tSequence.name);
		vSequence.setSequence(tSequence.getSequence());
		vSequence.setStart(1);
		vSequence.setEnd(tSequence.getLength());
		
		if (tSequenceSet.isDNA())
			vSequence.setDictionary(SymbolDictionary.STANDARD_NA);
		else
			vSequence.setDictionary(SymbolDictionary.STANDARD_AA);
	
		// TODO: dbRef
		
		return vSequence;
	}
	
	// Returns an AlignmentSequence
	// TODO: This is almost the same as the above - can't they be merged in
	// some way?
	AlignmentSequence createAlignmentSequence(Sequence ref, topali.data.Sequence tSequence)
	{
		AlignmentSequence vAlignmentSequence = new AlignmentSequence();
		
		vAlignmentSequence.setName(tSequence.name);
		vAlignmentSequence.setSequence(tSequence.getSequence());
		vAlignmentSequence.setStart(1);
		vAlignmentSequence.setEnd(tSequence.getLength());
				
		vAlignmentSequence.setRefid(ref);
				
		return vAlignmentSequence;
	}
	
	void createAnnotations(Alignment vAlignment)
	{
		// For now, let's just take the current graph display (partition) annotations (F6)
		// and ignore each of the individual analysis runs' ones.
		topali.data.TOPALiAnnotations tAnnotations = tAlignmentData.getTopaliAnnotations();		
		topali.data.PartitionAnnotations pAnnotations = tAnnotations.getPartitionAnnotations();
		
		AlignmentAnnotation vAlignmentAnnotation = new AlignmentAnnotation();
		vAlignmentAnnotation.setType("topali:Current Partitions");
		vAlignmentAnnotation.setGraph(false);
		vAlignmentAnnotation.setSeg(new Seg[] { createAlignmentSegment()});
		vAlignmentAnnotation.setProvenance(createProvenance());
		
		Glyph vGlyph1 = new Glyph();
		vGlyph1.setContent("[");
		Glyph vGlyph2 = new Glyph();
		vGlyph2.setContent("]");
		
		for (topali.data.RegionAnnotations.Region region: pAnnotations)
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();
			vAnnotationElement.setPosition(region.getS());
			vAnnotationElement.setAfter(false);			
			vAnnotationElement.setDescription("topali:Partition Start");
			vAnnotationElement.setGlyph(new Glyph[] { vGlyph1 });
			vAlignmentAnnotation.addAnnotationElement(vAnnotationElement);
			
			vAnnotationElement = new AnnotationElement();
			vAnnotationElement.setPosition(region.getE());
			vAnnotationElement.setAfter(false);			
			vAnnotationElement.setDescription("topali:Partition End");
			vAnnotationElement.setGlyph(new Glyph[] { vGlyph2 });
			vAlignmentAnnotation.addAnnotationElement(vAnnotationElement);
		}
		
		vAlignment.addAlignmentAnnotation(vAlignmentAnnotation);
	}

	Seg createAlignmentSegment()
	{
		Seg vSeg = new Seg();
		vSeg.setStart(1);
		vSeg.setEnd(tSequenceSet.getLength());
		vSeg.setInclusive(true);
		
		return vSeg;
	}

	private Provenance createProvenance()
		{ return createProvenance("update"); }

	private Provenance createProvenance(String action)
	{
		Provenance vProvenance = new Provenance();
		
		vProvenance.addEntry(createProvenanceEntry(action));
		
		return vProvenance;
	}
	
	private Entry createProvenanceEntry(String action)
	{
		Entry vEntry = new Entry();
		
		vEntry.setUser(System.getProperty("user.name"));
		vEntry.setApp("TOPALi");
		vEntry.setAction(action);
		vEntry.setDate(new org.exolab.castor.types.Date(System.currentTimeMillis()));
		
		return vEntry;
	}
	
/*	AlignmentAnnotations createAlignmentAnnotations(topali.data.Annotations tAnnotations)
	{
		AlignmentAnnotations vAlignmentAnnotations = new AlignmentAnnotations();
		
		vAlignmentAnnotations.setLabel(tAnnotations.getLabel());
		vAlignmentAnnotations.setDescription(tAnnotations.getDescription());
		vAlignmentAnnotations.setProvenance(createProvenance());
		// TODO: graph?
		vAlignmentAnnotations.setGraph(false);
		
		for (topali.data.AnnotationElement tAnnotationElement: tAnnotations.getAnnotations())
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();
			vAnnotationElement.setPosition(tAnnotationElement.position);
			vAnnotationElement.setDisplayCharacter(tAnnotationElement.displayCharacter);
			vAnnotationElement.setDescription(tAnnotationElement.description);
			vAnnotationElement.setSecondaryStructure(tAnnotationElement.secondaryStructure);
			vAnnotationElement.setValue(tAnnotationElement.value);
			
			vAlignmentAnnotations.addAnnotationElement(vAnnotationElement);
		}
		
		return vAlignmentAnnotations;
	}
*/
	
	Tree createTree(topali.data.TreeResult tTree)
	{
		Tree vTree = new Tree();
		
		vTree.setTitle(tTree.getTitle());
		
		// Safe newick formatted tree
		Newick newick = new Newick();
		newick.setContent(tTree.getTreeStr());
		vTree.addNewick(newick);
		
		// Unsafe format (but with real sequence names)
		try
		{
			newick = new Newick();
			newick.setContent(tTree.getTreeStrActual(tSequenceSet));
			vTree.addNewick(newick);
		}
		catch (pal.tree.TreeParseException e) {}
		
		vTree.setProvenance(createProvenance());
		
		return vTree;
	}
	
	AlignmentAnnotations createDSSAnnotations(topali.data.DSSResult result)
	{
		AlignmentAnnotations vAlignmentAnnotations = new AlignmentAnnotations();
		
		vAlignmentAnnotations.setLabel(result.guiName);
		vAlignmentAnnotations.setDescription("DSS Results Data");
		vAlignmentAnnotations.setGraph(true);
		vAlignmentAnnotations.setProvenance(createProvenance());
		
		createAnnotationsForDataArray(vAlignmentAnnotations, result.data);
	
		return vAlignmentAnnotations;
	}
	
	AlignmentAnnotations createHMMAnnotations(topali.data.HMMResult result, int graph)
	{
		AlignmentAnnotations vAlignmentAnnotations = new AlignmentAnnotations();
		
		vAlignmentAnnotations.setLabel(result.guiName);
		vAlignmentAnnotations.setDescription("HMM Results Data " + graph);
		vAlignmentAnnotations.setGraph(true);
		vAlignmentAnnotations.setProvenance(createProvenance());
		
		float[][] data = null;
		if (graph == 1) data = result.data1;
		else if (graph == 2) data = result.data2;
		else if (graph == 3) data = result.data3;
		
		createAnnotationsForDataArray(vAlignmentAnnotations, data);
		
		return vAlignmentAnnotations;
	}
	
	private void createAnnotationsForDataArray(AlignmentAnnotations vAlignmentAnnotations, float[][] data)
	{
/*		for (int i = 0; i < data.length; i++)
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();
			
			vAnnotationElement.setPosition((int)data[i][0]);
			vAnnotationElement.setValue(data[i][1]);
			
			vAlignmentAnnotations.addAnnotationElement(vAnnotationElement);
		}
*/
	}
	
/*	// Equations for a straight line y = mx+c
	// getM()
	private float eqGetM(int x1, int y1, int x2, int y2)
	{
		return (y1-y2) - (x1-x2);
	}
	
	// getX()
	private float eqGetC(int x1, int y1, int x2, int y2)
	{
		return ((x1*y2) - (x2*y1)) / (x1-x2);
	}
*/
}