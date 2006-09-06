package topali.vamsas;

import org.vamsas.objects.core.*;

class TOPALi2Vamsas
{
	private VAMSAS vVAMSAS = null;
	private boolean isUpdating = false;
	
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
		vVAMSAS.addDataSet(createDataSet(tAlignmentData));
		
		return vVAMSAS;
	}
	
	DataSet createDataSet(topali.data.AlignmentData tAlignmentData)
	{
		DataSet vDataSet = new DataSet();
		// Does this dataset already exist?
		
		topali.data.SequenceSet tSequenceSet = tAlignmentData.getSequenceSet();
		
		vDataSet.addAlignment(createAlignment(tAlignmentData));
		vDataSet.setProvenance(createProvenance());
		// TODO: id - optional
		
		for (topali.data.Sequence tSequence: tSequenceSet.getSequences())
		{
			Sequence vSequence = createSequence(tSequence);
			vDataSet.addSequence(vSequence);
		}
		
		return vDataSet;
	}
	
	// Returns a Sequence
	Sequence createSequence(topali.data.Sequence tSequence)
	{
		Sequence vSequence = new Sequence();
		
		vSequence.setName(tSequence.name);
		vSequence.setSequence(tSequence.getSequence());
		vSequence.setStart(1);
		vSequence.setEnd(tSequence.getLength());
		vSequence.setId(tSequence.safeName);
		// TODO: dictionary - ?
		// TODO: dbRef
		
		return vSequence;
	}
	
	Alignment createAlignment(topali.data.AlignmentData tAlignmentData)
	{
		Alignment vAlignment = new Alignment();
		
		vAlignment.setGapChar("-");
		vAlignment.setAligned(true);
		vAlignment.setProvenance(createProvenance());
//		if (tAlignmentData.getAnnotations().size() > 0)
//		{
//			AlignmentAnnotations a = createAlignmentAnnotations(tAlignmentData.getAnnotations());
//			vAlignment.addAlignmentAnnotations(a);
//		}
//		else
//			System.out.println("no annotations");
		// TODO: id - optional
		
		topali.data.SequenceSet tSequenceSet = tAlignmentData.getSequenceSet();
		for (topali.data.Sequence tSequence: tSequenceSet.getSequences())
		{
			AlignmentSequence alignmentSequence = createAlignmentSequence(tSequence);
			vAlignment.addAlignmentSequence(alignmentSequence);
		}
		
		// Results annotations
		for (topali.data.AnalysisResult tResult: tAlignmentData.getResults())
		{
//			if (tResult instanceof topali.data.HMMResult)
//				for (int graph = 1; graph <= 3; graph++)	
//					vAlignment.addAlignmentAnnotations(createHMMAnnotations((topali.data.HMMResult)tResult, graph));
					
			if (tResult instanceof topali.data.DSSResult)
			{
				vAlignment.addAlignmentAnnotations(
					createDSSAnnotations((topali.data.DSSResult)tResult));
			}
			
			if (tResult instanceof topali.data.TreeResult)
			{
				vAlignment.addTree(createTree((topali.data.TreeResult)tResult));
			}
		}
		
		return vAlignment;
	}
	
	// Returns an AlignmentSequence
	AlignmentSequence createAlignmentSequence(topali.data.Sequence tSequence)
	{
		AlignmentSequence vAlignmentSequence = new AlignmentSequence();
		
		vAlignmentSequence.setName(tSequence.name);
		vAlignmentSequence.setSequence(tSequence.getSequence());
		vAlignmentSequence.setStart(1);
		vAlignmentSequence.setEnd(tSequence.getLength());
		vAlignmentSequence.setId(tSequence.safeName);
		vAlignmentSequence.setRefid(createSequence(tSequence));
				
		return vAlignmentSequence;
	}

	Provenance createProvenance()
	{
		Provenance vProvenance = new Provenance();
		
		vProvenance.addEntry(createProvenanceEntry());
		
		return vProvenance;
	}
	
	Entry createProvenanceEntry()
	{
		Entry vEntry = new Entry();
		
		vEntry.setUser(System.getProperty("user.name"));
		vEntry.setDate(new org.exolab.castor.types.Date(System.currentTimeMillis()));
		vEntry.setAction("created");
		// TODO: id - optional
		
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
		
		Newick newick = new Newick();
		newick.setContent(tTree.getTreeStr());
		
		vTree.addNewick(newick);
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
		for (int i = 0; i < data.length; i++)
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();
			
			vAnnotationElement.setPosition((int)data[i][0]);
			vAnnotationElement.setValue(data[i][1]);
			
			vAlignmentAnnotations.addAnnotationElement(vAnnotationElement);
		}
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