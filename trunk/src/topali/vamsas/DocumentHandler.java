package topali.vamsas;

import java.util.*;

import pal.tree.TreeParseException;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.data.Sequence;
import topali.data.RegionAnnotations.Region;
import topali.gui.Project;
import topali.gui.dialog.CreateTreeDialog;
import uk.ac.vamsas.client.IClientDocument;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

class DocumentHandler
{
	//TODO: 
	// * Implement CodeMLResult (works to great extend)
	// * MGResult (Done)
	// * CodonWResult
	// * Annotations
	
	private Project project;

	private ObjectMapper mapper;

	private DataSet dataset;

	// TODO: General problem: Mapping between one topali object and several
	// vamsas objects
	// e.g. tSequence -> vSequence and vDataSetSequence
	// tAnalysisResult -> several vAnnotations

	public DocumentHandler(Project proj, ObjectMapper mapper,
			IClientDocument doc)
	{
		this.project = proj;
		this.mapper = mapper;

		// We will just deal with the first DataSet
		// TODO: Jalview doesn't!
		if (doc.getVamsasRoots()[0].getDataSetCount() < 1)
		{
			this.dataset = new DataSet();
			this.dataset.setProvenance(getDummyProvenance());
			doc.getVamsasRoots()[0].addDataSet(this.dataset);
		} else
			this.dataset = doc.getVamsasRoots()[0].getDataSet(0);

		mapper.registerClientDocument(doc);

	}

	void writeToDocument() throws Exception
	{
		LinkedList<AlignmentData> tDatasets = project.getDatasets();

		for (AlignmentData tAlign : tDatasets)
		{
			writeAlignment(tAlign);
		}

	}

	void readFromDocument() throws Exception
	{
		Alignment[] tDatasets = dataset.getAlignment();
		for (Alignment vAlign : tDatasets)
		{
			AlignmentData tAlign = (AlignmentData) mapper
					.getTopaliObject(vAlign);
			if (tAlign == null)
			{
				tAlign = new AlignmentData();
				readAlignment(vAlign, tAlign);
				tAlign.setActiveRegion(1, tAlign.getSequenceSet().getLength());
				mapper.registerObjects(tAlign, vAlign);
				project.addDataSet(tAlign);
			} else
			{
				readAlignment(vAlign, tAlign);
			}
		}
	}

	// ----------
	// Vamsas -> TOPALi methods:

	private void readAlignment(Alignment vAlign, AlignmentData tAlign)
			throws Exception
	{
		tAlign.name = getAlignmentName(vAlign);
		SequenceSet ss = tAlign.getSequenceSet();
		if (ss == null)
		{
			ss = new SequenceSet();
			ss.setOverview("");
			tAlign.setSequenceSet(ss);
		}
		readSequences(ss, vAlign);
		ss.checkValidity();

		readAnalysisResults(tAlign, vAlign);
		
		readAnnotations(tAlign, vAlign);
	}

	private void readSequences(SequenceSet ss, Alignment vAlign)
	{
		for (AlignmentSequence vSeq : vAlign.getAlignmentSequence())
		{
			Sequence tSeq = (Sequence) mapper.getTopaliObject(vSeq);

			if (tSeq == null)
			{
				tSeq = new Sequence();
				// actual sequence must be set before adding to sequenceset
				tSeq.setSequence(vSeq.getSequence());
				tSeq.name = vSeq.getName();
				mapper.registerObjects(tSeq, vSeq);
				ss.addSequence(tSeq);
			} else
			{
				tSeq.setSequence(vSeq.getSequence());
				tSeq.name = vSeq.getName();
			}
		}
	}

	private void readAnalysisResults(AlignmentData tAlign, Alignment vAlign)
	{
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			AnalysisResult tResult = (AnalysisResult) mapper
					.getTopaliObject(vAnno);

			if (tResult == null)
			{
				if (vAnno.getType().equals("PDMResult"))
				{
					PDMResult res = new PDMResult();
					res.guiName = vAnno.getDescription();
					float[][] glbData = getGraph(vAnno, "glbData");
					float[][] lclData = getGraph(vAnno, "locData");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.glbData = glbData;
					res.locData = lclData;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("HMMResult"))
				{
					HMMResult res = new HMMResult();
					res.guiName = vAnno.getDescription();
					float[][] data1 = getGraph(vAnno, "data1");
					float[][] data2 = getGraph(vAnno, "data2");
					float[][] data3 = getGraph(vAnno, "data3");
					float threshold = getData(vAnno, "threshold")[0];
					res.data1 = data1;
					res.data2 = data2;
					res.data3 = data3;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				} 
				
				else if (vAnno.getType().equals("LRTResult"))
				{
					LRTResult res = new LRTResult();
					res.guiName = vAnno.getDescription();
					float[][] data = getGraph(vAnno, "data");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.data = data;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				}
				
				else if (vAnno.getType().equals("DSSResult"))
				{
					DSSResult res = new DSSResult();
					res.guiName = vAnno.getDescription();
					float[][] data = getGraph(vAnno, "data");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.data = data;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				} 
				
				else if (vAnno.getType().equals("CodeMLResult"))
				{
					CodeMLResult res = new CodeMLResult();
					res.guiName = vAnno.getDescription();
					
					float[][] graph = getGraph(vAnno, CMLModel.MODEL_M2a);
					if(graph!=null) {
						CMLModel model = new CMLModel(CMLModel.MODEL_M2a);
						model.setGraph(graph);
						res.models.add(model);
					}
					
					graph = getGraph(vAnno, CMLModel.MODEL_M2);
					if(graph!=null) {
						CMLModel model = new CMLModel(CMLModel.MODEL_M2);
						model.setGraph(graph);
						res.models.add(model);
					}
					
					graph = getGraph(vAnno, CMLModel.MODEL_M3);
					if(graph!=null) {
						CMLModel model = new CMLModel(CMLModel.MODEL_M3);
						model.setGraph(graph);
						res.models.add(model);
					}
					
					graph = getGraph(vAnno, CMLModel.MODEL_M8);
					if(graph!=null) {
						CMLModel model = new CMLModel(CMLModel.MODEL_M8);
						model.setGraph(graph);
						res.models.add(model);
					}
					
					res.status = JobStatus.COMPLETED;
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				} 
				
				else if(vAnno.getType().equals("MGResult")) {
					MGResult res = new MGResult();
					res.guiName = vAnno.getDescription();
					res.status = JobStatus.COMPLETED;
					for(AnnotationElement el : vAnno.getAnnotationElement()) {
						SubstitutionModel mod = new SubstitutionModel();
						float[] data = el.getValue();
						mod.setName(el.getDescription());
						mod.setLnl(data[0]);
						mod.setAic1(data[1]);
						mod.setAic2(data[2]);
						mod.setBic(data[3]);
						res.models.add(mod);
					}
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				}
				
				else if(vAnno.getType().equals("CodonWResult")) {
					CodonWResult res = new CodonWResult();
					res.guiName = vAnno.getDescription();
					res.status = JobStatus.COMPLETED;
					AnnotationElement el = vAnno.getAnnotationElement(0);
					res.result = el.getDescription();
					tAlign.getResults().add(res);
					mapper.registerObjects(res, vAnno);
				}
			}
			
			Tree[] vTrees = vAlign.getTree();
			for(Tree vTree : vTrees) {
				TreeResult tTree = (TreeResult)mapper.getTopaliObject(vTree);
				if(tTree==null) {
					tTree = new TreeResult();
					tTree.setTreeStr(vTree.getNewick(0).getContent());
					tTree.guiName = vTree.getTitle();
					tTree.status = JobStatus.COMPLETED;
					
					for(Property prop : vTree.getProperty()) {
						if(prop.getName().equals("start"))
							tTree.setPartitionStart(Integer.parseInt(prop.getContent()));
						else if(prop.getName().equals("end"))
							tTree.setPartitionEnd(Integer.parseInt(prop.getContent()));
					}
					tAlign.getResults().add(tTree);
					mapper.registerObjects(tTree, vTree);
				}
			}
		}
	}

	private void readAnnotations(AlignmentData tAlign, Alignment vAlign) {
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			if(vAnno.getType().equals("Partition")) {
				Region region = (Region)mapper.getTopaliObject(vAnno);
				if(region==null) {
					PartitionAnnotations partAnnos = (PartitionAnnotations)tAlign.getTopaliAnnotations().getAnnotations(PartitionAnnotations.class);
					Seg seg = vAnno.getSeg(0);
					Region reg = new Region(seg.getStart(), seg.getEnd());
					mapper.registerObjects(reg, vAnno);
					partAnnos.addRegion(reg);
				}
			}
			
			else if(vAnno.getType().equals("Coding Region")) {
				Region region = (Region)mapper.getTopaliObject(vAnno);
				if(region==null) {
					CDSAnnotations cdsAnnos = (CDSAnnotations)tAlign.getTopaliAnnotations().getAnnotations(CDSAnnotations.class);
					Seg seg = vAnno.getSeg(0);
					Region reg = new Region(seg.getStart(), seg.getEnd());
					mapper.registerObjects(reg, vAnno);
					cdsAnnos.addRegion(reg);
				}
			}
		}
	}
	
	private float[][] getGraph(AlignmentAnnotation vAnno, String desc)
	{

		LinkedList<Float> positions = new LinkedList<Float>();
		LinkedList<Float> values = new LinkedList<Float>();

		AnnotationElement[] els = vAnno.getAnnotationElement();

		for (int i = 0; i < els.length; i++)
		{
			int pos = (int) els[i].getPosition();
			float value = els[i].getValue(0);
			String description = els[i].getDescription();
			if (description.equals(desc))
			{
				positions.add((float) pos);
				values.add(value);
			}
		}

		float[][] result = new float[positions.size()][2];
		for (int j = 0; j < positions.size(); j++)
		{
			result[j][0] = positions.get(j);
			result[j][1] = values.get(j);
		}

		return result;
	}
	
	private float[] getData(AlignmentAnnotation vAnno, String desc)
	{
		AnnotationElement[] els = vAnno.getAnnotationElement();
		for (int i = 0; i < els.length; i++)
		{
			if (els[i].getDescription().equals(desc))
				return els[i].getValue();
		}
		return null;
	}

	private String getAlignmentName(Alignment vAlign)
	{
		Property[] props = vAlign.getProperty();
		for (Property prop : props)
		{
			if (prop.getName().equals("title"))
				return prop.getContent();
		}
		return "Vamsas";
	}

	// -----------
	// TOPALi -> Vamsas methods:

	// Given an existing TOPALi dataset, attempts to either find it within the
	// vamsas document, or creates a new vamsas Alignment to link it with, and
	// adds that to the document instead.
	private void writeAlignment(AlignmentData tAlign)
	{

		// Do we have an existing mapping between T/V for this object?
		Alignment vAlign = (Alignment) mapper.getVamsasObject(tAlign);

		if (vAlign == null)
		{
			System.out.println("Alignment is null - making a new one");

			// Create a new vamsas alignment
			vAlign = new Alignment();
			vAlign.setProvenance(getDummyProvenance("added"));
			vAlign.setGapChar("-");
			Property title = new Property();
			title.setName("title");
			title.setType("string");
			title.setContent(tAlign.name);
			vAlign.addProperty(title);

			dataset.addAlignment(vAlign);

			// Link it with the TOPALi data set
			mapper.registerObjects(tAlign, vAlign);
		}

		SequenceSet tSequenceSet = tAlign.getSequenceSet();
		writeAlignmentSequences(tSequenceSet, vAlign);

		writeAnalysisResults(tAlign, vAlign);
		
		writeAnnotations(tAlign, vAlign);
	}

	private void writeAlignmentSequences(SequenceSet tSequenceSet,
			Alignment vAlign)
	{
		for (Sequence tSeq : tSequenceSet.getSequences())
		{
			AlignmentSequence vSeq = (AlignmentSequence) mapper
					.getVamsasObject(tSeq);

			uk.ac.vamsas.objects.core.Sequence vDSSequence = null;

			if (vSeq == null)
			{
				vSeq = new AlignmentSequence();
				mapper.registerObjects(tSeq, vSeq);
				vAlign.addAlignmentSequence(vSeq);

				// Add sequence to vamsas dataset:
				vDSSequence = new uk.ac.vamsas.objects.core.Sequence();
				vDSSequence.setSequence(tSeq.getSequence());
				vDSSequence.setName(tSeq.name);
				vDSSequence.setStart(0);
				vDSSequence.setEnd(tSeq.getLength() - 1);
				if (tSequenceSet.getParams().isDNA())
					vDSSequence.setDictionary(SymbolDictionary.STANDARD_NA);
				else
					vDSSequence.setDictionary(SymbolDictionary.STANDARD_AA);
				dataset.addSequence(vDSSequence);
				// TODO: very bad, but have to do this, to register vDSSequence
				// with the clientdocument
				mapper.registerObjects(tSeq, vDSSequence);

				// now register tSeq with the vamsas sequence, which really
				// matters:
				mapper.registerObjects(tSeq, vSeq);
			}

			vSeq.setSequence(tSeq.getSequence());
			vSeq.setName(tSeq.name);
			vSeq.setStart(0);
			vSeq.setEnd(tSeq.getLength() - 1);
			// new sequences have to be linked to the corresponding dataset
			// sequences
			if (vDSSequence != null)
				vSeq.setRefid(vDSSequence);
		}
	}

	private void writeAnalysisResults(AlignmentData tAlign, Alignment vAlign)
	{
		for (AnalysisResult tRes : tAlign.getResults())
		{
			AlignmentAnnotation vAnno = (AlignmentAnnotation) mapper
					.getVamsasObject(tRes);

			if (vAnno == null)
			{

				vAnno = new AlignmentAnnotation();
				int start = 0;
				int end = tAlign.getSequenceSet().getLength();
				Seg seg = new Seg();
				seg.setStart(start);
				seg.setEnd(end);
				seg.setInclusive(true);
				vAnno.setSeg(new Seg[]
				{ seg });

				if (tRes instanceof PDMResult)
				{
					PDMResult result = (PDMResult) tRes;
					vAnno.setType("PDMResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.glbData, "glbData");
					addGraph(vAnno, result.locData, "locData");
					addData(vAnno, result.thresholds, "thresholds");
					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}

				else if (tRes instanceof HMMResult)
				{
					HMMResult result = (HMMResult) tRes;
					vAnno.setType("HMMResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.data1, "data1");
					addGraph(vAnno, result.data2, "data2");
					addGraph(vAnno, result.data3, "data3");
					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				} 
				
				else if (tRes instanceof DSSResult)
				{
					DSSResult result = (DSSResult) tRes;
					vAnno.setType("DSSResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.data, "data");
					addData(vAnno, result.thresholds, "thresholds");
					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				} 
				
				else if (tRes instanceof LRTResult)
				{
					LRTResult result = (LRTResult) tRes;
					vAnno.setType("LRTResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.data, "data");
					addData(vAnno, result.thresholds, "thresholds");
					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				} 
				
				else if (tRes instanceof CodeMLResult)
				{
					CodeMLResult result = (CodeMLResult)tRes;
					vAnno.setType("CodeMLResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					vAnno.setProvenance(getDummyProvenance());
					for(int i=0; i<result.models.size(); i++) {
						float[][] graph = result.models.get(i).getGraph();
						if(graph!=null) {
							addGraph(vAnno, graph, result.models.get(i).model);
						}
					}
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				} 
				
				else if(tRes instanceof MGResult) {
					MGResult result = (MGResult) tRes;
					vAnno.setType("MGResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(false);
					vAnno.setProvenance(getDummyProvenance());
					
					for(SubstitutionModel m : result.models) {
						AnnotationElement el = new AnnotationElement();
						el.setPosition(-1);
						el.setDescription(m.getName());
						el.setValue(new float[] {(float)m.getLnl(), (float)m.getAic1(), (float)m.getAic2(), (float)m.getBic()});
						vAnno.addAnnotationElement(el);
					}
					
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}
				
				else if(tRes instanceof CodonWResult) {
					CodonWResult result = (CodonWResult)tRes;
					vAnno.setType("CodonWResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(false);
					
					AnnotationElement el = new AnnotationElement();
					el.setPosition(-1);
					el.setDescription(result.result);
					el.setValue(new float[] {});
					vAnno.addAnnotationElement(el);
					
					vAnno.setProvenance(getDummyProvenance());
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}
				
				else if (tRes instanceof TreeResult)
				{
					TreeResult result = (TreeResult)tRes;
					Tree vTree = new Tree();
					vTree.setTitle(result.getTitle());
					Newick nw = new Newick();
					nw.setContent(result.getTreeStr());
					vTree.addNewick(nw);
					
					Property p1 = new Property();
					p1.setType("int");
					p1.setName("start");
					p1.setContent(""+result.getPartitionStart());
					vTree.addProperty(p1);
					
					Property p2 = new Property();
					p2.setType("int");
					p2.setName("end");
					p2.setContent(""+result.getPartitionEnd());
					vTree.addProperty(p2);
					
					vTree.setTitle(result.guiName);
					
					vTree.setProvenance(getDummyProvenance());
					mapper.registerObjects(tRes, vTree);
					vAlign.addTree(vTree);
				}

			}

		}
	}

	private void writeAnnotations(AlignmentData tAlign, Alignment vAlign) {
		PartitionAnnotations partAnnos = (PartitionAnnotations)tAlign.getTopaliAnnotations().getAnnotations(PartitionAnnotations.class);
		for(Region region : partAnnos) {
			AlignmentAnnotation vAnno = (AlignmentAnnotation)mapper.getVamsasObject(region);
			if(vAnno==null) {
				vAnno = new AlignmentAnnotation();
				vAnno.setProvenance(getDummyProvenance());
				vAnno.setType("Partition");
				vAnno.setGraph(false);
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setStart(start);
				seg.setEnd(end);
				seg.setInclusive(true);
				vAnno.setSeg(new Seg[]
				{ seg });
				
				mapper.registerObjects(region, vAnno);
				vAlign.addAlignmentAnnotation(vAnno);
			}
		}
		
		CDSAnnotations cdsAnno = (CDSAnnotations)tAlign.getTopaliAnnotations().getAnnotations(CDSAnnotations.class);
		for(Region region : cdsAnno) {
			AlignmentAnnotation vAnno = (AlignmentAnnotation)mapper.getVamsasObject(region);
			if(vAnno==null) {
				vAnno = new AlignmentAnnotation();
				vAnno.setProvenance(getDummyProvenance());
				vAnno.setType("Coding Region");
				vAnno.setGraph(false);
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setStart(start);
				seg.setEnd(end);
				seg.setInclusive(true);
				vAnno.setSeg(new Seg[]
				{ seg });
				
				mapper.registerObjects(region, vAnno);
				vAlign.addAlignmentAnnotation(vAnno);
			}
		}
	}
	
	private void addGraph(AlignmentAnnotation anno, float[][] data, String desc)
	{
		for (int i = 0; i < data.length; i++)
		{
			int pos = (int) (data[i][0]);
			float value = data[i][1];

			AnnotationElement el = new AnnotationElement();
			el.setDescription(desc);
			el.setPosition(pos);
			el.setValue(new float[]
			{ value });
			anno.addAnnotationElement(el);
		}
	}

	private void addData(AlignmentAnnotation anno, float data, String desc)
	{
		addData(anno, new float[]
		{ data }, desc);
	}

	private void addData(AlignmentAnnotation anno, float[] data, String desc)
	{
		AnnotationElement el = new AnnotationElement();
		el.setDescription(desc);
		el.setPosition(-1);
		el.setValue(data);
		anno.addAnnotationElement(el);
	}

	// --------------------
	// some dummy methods:

	private Provenance getDummyProvenance()
	{
		return getDummyProvenance(null);
	}

	private Provenance getDummyProvenance(String action)
	{
		Provenance p = new Provenance();
		p.addEntry(getDummyEntry(action));

		return p;
	}

	private Entry getDummyEntry(String action)
	{
		Entry e = new Entry();

		e.setApp(VamsasManager.app.getClientUrn());
		e.setUser(VamsasManager.user.getFullName());
		e.setDate(new Date());

		if (action != null)
			e.setAction(action);
		else
			e.setAction("created.");

		return e;
	}
}