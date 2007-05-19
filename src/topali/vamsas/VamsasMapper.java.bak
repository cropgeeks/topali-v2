// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.util.LinkedList;
import java.util.List;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.fileio.AlignmentLoadException;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.core.Sequence;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

public class VamsasMapper
{

	private static AlignmentData aData;

	public static DataSet createVamsasDataSet(AlignmentData... aData)
	{
		DataSet vDataSet = new DataSet();

		for(AlignmentData data : aData) {
			VamsasMapper.aData = data;
			vDataSet.addAlignment(createAlignment(vDataSet));
		}

		vDataSet.setProvenance(createProvenance(""));

		return vDataSet;
	}

	public static AlignmentData[] createTopaliDataSet(DataSet vData)
	{
		AlignmentData[] aData = new topali.data.AlignmentData[vData
				.getAlignmentCount()];

		for (int i = 0; i < aData.length; i++)
		{
			Alignment algn = vData.getAlignment(i);
			AlignmentSequence[] vSeqs = algn.getAlignmentSequence();
			
			try
			{
				aData[i] = new AlignmentData();
				aData[i].setSequenceSet(createSequenceSet(vSeqs));
			} catch (AlignmentLoadException e)
			{
				e.printStackTrace();
				continue;
			}
			
			aData[i].setResults(createAlignmentResults(algn.getAlignmentAnnotation()));
		}

		return aData;
	}

	// Vamsas2Topali methods
	private static SequenceSet createSequenceSet(AlignmentSequence[] vSeqs) throws AlignmentLoadException
	{
		SequenceSet tSeqSet = new topali.data.SequenceSet();

		for (int i = 0; i < vSeqs.length; i++) {
			topali.data.Sequence tSeq = new topali.data.Sequence();
			tSeq.name = vSeqs[i].getName();
			tSeq.setSequence(vSeqs[i].getSequence());
			tSeqSet.addSequence(tSeq);
		}

		tSeqSet.checkValidity();
		return tSeqSet;
	}

	private static LinkedList<AnalysisResult> createAlignmentResults(AlignmentAnnotation[] annos) {
		LinkedList<AnalysisResult> res = new LinkedList<AnalysisResult>();
		for(AlignmentAnnotation anno : annos) {
			
			AnalysisResult result = null;
			
			for(AnalysisResult r : res) {
				if(r.guiName!=null && r.guiName.equals(anno.getDescription())) {
					result = r;
					break;
				}
			}
			
			boolean newRes = false;
			
			if(anno.getType().startsWith("PDMResult")) {
				if(result==null) {
					result = new PDMResult();
					result.guiName = anno.getDescription();
					newRes = true;
				}
				if(anno.getType().endsWith("Global")) {
					((PDMResult)result).glbData = createDataArray(anno.getAnnotationElement());
				}
				else if(anno.getType().endsWith("Local")) {
					((PDMResult)result).locData = createDataArray(anno.getAnnotationElement());
				}
				if(newRes) {
					result.status = JobStatus.COMPLETED;
					res.add(result);
				}
			}
			else if(anno.getType().startsWith("HMMResult")) {
				if(result==null) {
					result = new HMMResult();
					result.guiName = anno.getDescription();
					newRes = true;
				}
				
				if(anno.getType().endsWith("Graph1")) {
					((HMMResult)result).data1 = createDataArray(anno.getAnnotationElement());
				}
				else if(anno.getType().endsWith("Graph2")) {
					((HMMResult)result).data2 = createDataArray(anno.getAnnotationElement());
				}
				else if(anno.getType().endsWith("Graph3")) {
					((HMMResult)result).data3 = createDataArray(anno.getAnnotationElement());
				}
				
				if(newRes) {
					result.status = JobStatus.COMPLETED;
					res.add(result);
				}
			}
			else if(anno.getType().startsWith("DSSResult")) {
				result = new DSSResult();
				result.guiName = anno.getDescription();
				((DSSResult)result).data = createDataArray(anno.getAnnotationElement());
				result.status = JobStatus.COMPLETED;
				res.add(result);
			}
			else if(anno.getType().startsWith("LRTResult")) {
				result = new LRTResult();
				result.guiName = anno.getDescription();
				((LRTResult)result).data = createDataArray(anno.getAnnotationElement());
				result.status = JobStatus.COMPLETED;
				res.add(result);
			}
			else if(anno.getType().startsWith("CodeML")) {
				if(result==null) {
					result = new CodeMLResult();
					result.guiName = anno.getDescription();
					newRes = true;
				}
				
				((CodeMLResult)result).models.add(createCodeMLModel(anno));
				
				if(newRes) {
					result.status = JobStatus.COMPLETED;
					res.add(result);
				}
			}
		}
		return res;
	}
	
	private static float[][] createDataArray(AnnotationElement[] els) {
		float[][] arr = new float[els.length][2];
		for(int i=0; i<els.length; i++) {
			arr[i][0] = els[i].getPosition();
			arr[i][1] = els[i].getValue()[0];
		}
		return arr;
	}
	
	private static CMLModel createCodeMLModel(AlignmentAnnotation anno) {
		CMLModel mod = null;
		String[] tmp  = anno.getType().split("\\|");
		String model = tmp[1];
		if(model.equals("M2a")) 
			mod = new CMLModel(CMLModel.MODEL_M2a);
		else if(model.equals("M8"))
			mod = new CMLModel(CMLModel.MODEL_M8);
		
		mod.pssList = new LinkedList<PSSite>();
		
		AnnotationElement[] els = anno.getAnnotationElement();
		for(AnnotationElement el : els) {
			int pos = ((int)el.getPosition() + 1)/3;
			double value = el.getValue(0);
			mod.pssList.add(new PSSite(pos, '-', value));
		}
		
		return mod;
	}
	
	// Topali2Vamsas methods
	private static Alignment createAlignment(DataSet vDataSet)
	{
		Alignment vAlignment = new Alignment();

		vAlignment.setGapChar("-");
		vAlignment.setAligned(true);
		// vAlignment.setModifiable(false);

		// TODO: id - optional

		topali.data.SequenceSet tSequenceSet = aData.getSequenceSet();
		for (topali.data.Sequence tSequence : tSequenceSet.getSequences())
		{
			// Create (and add) the DataSetSequence first...
			Sequence vSequence = createSequence(tSequence);
			vDataSet.addSequence(vSequence);

			// So it can be used as a reference for the AlignmentSequence
			AlignmentSequence alignmentSequence = createAlignmentSequence(
					vSequence, tSequence);
			vAlignment.addAlignmentSequence(alignmentSequence);
		}

		// Partition annotations
		createPartitionAnnotations(vAlignment);

		// Results annotations
		for (topali.data.AnalysisResult tResult : aData.getResults())
		{
			if (tResult instanceof topali.data.PDMResult)
			{
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"PDMResult|Global", tResult.guiName,
								((topali.data.PDMResult) tResult).glbData));
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"PDMResult|Local", tResult.guiName,
								((topali.data.PDMResult) tResult).locData));
			}

			if (tResult instanceof topali.data.HMMResult)
			{
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"HMMResult|Graph1", tResult.guiName,
								((topali.data.HMMResult) tResult).data1));
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"HMMResult|Graph2", tResult.guiName,
								((topali.data.HMMResult) tResult).data3));
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"HMMResult|Graph3", tResult.guiName,
								((topali.data.HMMResult) tResult).data2));
			}

			if (tResult instanceof topali.data.DSSResult)
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"DSSResult", tResult.guiName,
								((topali.data.DSSResult) tResult).data));

			if (tResult instanceof topali.data.LRTResult)
				vAlignment
						.addAlignmentAnnotation(createAnnotationsForDataArray(
								"LRTResult", tResult.guiName,
								((topali.data.LRTResult) tResult).data));
			if (tResult instanceof topali.data.CodeMLResult)
			{
				List<AlignmentAnnotation> annos = createCodeMLAnnotations((CodeMLResult) tResult);
				for (AlignmentAnnotation anno : annos)
					vAlignment.addAlignmentAnnotation(anno);
			}

			if (tResult instanceof topali.data.TreeResult)
				vAlignment
						.addTree(createTree((topali.data.TreeResult) tResult));
		}

		vAlignment.setProvenance(createProvenance(""));
		return vAlignment;
	}

	private static Provenance createProvenance(String action)
	{
		Provenance vProvenance = new Provenance();

		Entry vEntry = new Entry();

		vEntry.setUser(System.getProperty("user.name"));
		vEntry.setApp("TOPALi");
		vEntry.setAction(action);
		vEntry.setDate(new org.exolab.castor.types.Date(System
				.currentTimeMillis()));

		vProvenance.addEntry(vEntry);

		return vProvenance;
	}

	private static Seg createAlignmentSegment()
	{
		Seg vSeg = new Seg();
		vSeg.setStart(1);
		vSeg.setEnd(aData.getSequenceSet().getLength());
		vSeg.setInclusive(true);

		return vSeg;
	}

	private static AlignmentAnnotation createAnnotationsForDataArray(
			String type, String name, float[][] data)
	{
		AlignmentAnnotation vAlignmentAnnotation = new AlignmentAnnotation();
		vAlignmentAnnotation.setType(type);
		vAlignmentAnnotation.setDescription(name);
		vAlignmentAnnotation.setGraph(true);
		vAlignmentAnnotation.setSeg(new Seg[]
		{ createAlignmentSegment() });
		vAlignmentAnnotation.setProvenance(createProvenance(""));

		for (int i = 0; i < data.length; i++)
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();

			vAnnotationElement.setPosition((int) data[i][0]);
			// TODO: At some point vamsas will take multiple Y's per X
			vAnnotationElement.setValue(new float[]
			{ data[i][1] });
			vAlignmentAnnotation.addAnnotationElement(vAnnotationElement);
		}

		return vAlignmentAnnotation;
	}

	private static List<AlignmentAnnotation> createCodeMLAnnotations(
			CodeMLResult res)
	{
		List<AlignmentAnnotation> annos = new LinkedList<AlignmentAnnotation>();
		for (CMLModel model : res.models)
		{
			if (model.supportsPSS)
			{
				AlignmentAnnotation anno = new AlignmentAnnotation();
				anno.setSeg(new Seg[]
				{ createAlignmentSegment() });
				anno.setProvenance(createProvenance(""));
				anno.setType("CodeML|" + model.abbr);
				anno.setDescription(res.guiName);
				anno.setGraph(true);
				List<PSSite> sites = model.getPSS(0);
				for (PSSite ps : sites)
				{
					AnnotationElement el = new AnnotationElement();
					el.setPosition(ps.getPos() * 3 - 1);
					el.setValue(new float[]
					{ (float) ps.getP() });
					anno.addAnnotationElement(el);
				}
				annos.add(anno);
			}
		}
		return annos;
	}

	private static Sequence createSequence(topali.data.Sequence tSequence)
	{
		Sequence vSequence = new Sequence();

		vSequence.setName(tSequence.name);
		vSequence.setSequence(tSequence.getSequence());
		vSequence.setStart(1);
		vSequence.setEnd(tSequence.getLength());

		if (aData.getSequenceSet().isDNA())
			vSequence.setDictionary(SymbolDictionary.STANDARD_NA);
		else
			vSequence.setDictionary(SymbolDictionary.STANDARD_AA);

		return vSequence;
	}

	private static AlignmentSequence createAlignmentSequence(Sequence ref,
			topali.data.Sequence tSequence)
	{
		AlignmentSequence vAlignmentSequence = new AlignmentSequence();

		vAlignmentSequence.setName(tSequence.name);
		vAlignmentSequence.setSequence(tSequence.getSequence());
		vAlignmentSequence.setStart(1);
		vAlignmentSequence.setEnd(tSequence.getLength());

		vAlignmentSequence.setRefid(ref);

		return vAlignmentSequence;
	}

	private static void createPartitionAnnotations(Alignment vAlignment)
	{
		// For now, let's just take the current graph display (partition)
		// annotations (F6)
		// and ignore each of the individual analysis runs' ones.
		topali.data.TOPALiAnnotations tAnnotations = aData
				.getTopaliAnnotations();
		topali.data.PartitionAnnotations pAnnotations = (PartitionAnnotations) tAnnotations
				.getAnnotations(PartitionAnnotations.class);

		AlignmentAnnotation vAlignmentAnnotation = new AlignmentAnnotation();
		vAlignmentAnnotation.setType("topali:Current Partitions");
		vAlignmentAnnotation.setGraph(false);
		vAlignmentAnnotation.setSeg(new Seg[]
		{ createAlignmentSegment() });
		vAlignmentAnnotation.setProvenance(createProvenance(""));

		Glyph vGlyph1 = new Glyph();
		vGlyph1.setContent("[");
		Glyph vGlyph2 = new Glyph();
		vGlyph2.setContent("]");

		for (topali.data.RegionAnnotations.Region region : pAnnotations)
		{
			AnnotationElement vAnnotationElement = new AnnotationElement();
			vAnnotationElement.setPosition(region.getS());
			vAnnotationElement.setAfter(false);
			vAnnotationElement.setDescription("topali:Partition Start");
			vAnnotationElement.setGlyph(new Glyph[]
			{ vGlyph1 });
			vAlignmentAnnotation.addAnnotationElement(vAnnotationElement);

			vAnnotationElement = new AnnotationElement();
			vAnnotationElement.setPosition(region.getE());
			vAnnotationElement.setAfter(false);
			vAnnotationElement.setDescription("topali:Partition End");
			vAnnotationElement.setGlyph(new Glyph[]
			{ vGlyph2 });
			vAlignmentAnnotation.addAnnotationElement(vAnnotationElement);
		}

		vAlignment.addAlignmentAnnotation(vAlignmentAnnotation);
	}

	private static Tree createTree(topali.data.TreeResult tTree)
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
			newick.setContent(tTree.getTreeStrActual(aData.getSequenceSet()));
			vTree.addNewick(newick);
		} catch (pal.tree.TreeParseException e)
		{
		}

		vTree.setProvenance(createProvenance(""));

		return vTree;
	}

}
