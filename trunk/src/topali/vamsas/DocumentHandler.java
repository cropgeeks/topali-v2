package topali.vamsas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.LinkedList;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.data.Sequence;
import topali.data.RegionAnnotations.Region;
import topali.gui.Project;
import uk.ac.vamsas.client.IClientDocument;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

class DocumentHandler
{

	private Project project;

	private ObjectMapper mapper;

	private DataSet dataset;

	public DocumentHandler(Project proj, ObjectMapper mapper,
			IClientDocument doc)
	{
		this.project = proj;
		this.mapper = mapper;

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
				tSeq.setName(vSeq.getName());
				mapper.registerObjects(tSeq, vSeq);
				ss.addSequence(tSeq);
			} else
			{
				tSeq.setSequence(vSeq.getSequence());
				tSeq.setName(vSeq.getName());
			}
		}
	}

	private void readAnalysisResults(AlignmentData tAlign, Alignment vAlign)
	{
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			AnalysisResult tResult;
			try
			{
				tResult = (AnalysisResult) mapper.getTopaliObject(vAnno);
			} catch (RuntimeException e)
			{
				// Can throw class cast exception, because
				// Vamsas Annotation can be either Topali AnalyisResult or
				// Annotation!
				continue;
			}

			if (tResult == null)
			{

				if (vAnno.getType().equals("PDMResult"))
				{
					PDMResult res = new PDMResult();
					readResultProvenance(vAnno.getProvenance(), res);
					res.guiName = vAnno.getDescription();
					float[][] glbData = getGraph(vAnno, "glbData");
					float[][] lclData = getGraph(vAnno, "locData");
					float[][] histo = getGraph(vAnno, "histograms");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.glbData = glbData;
					res.locData = lclData;
					res.histograms = histo;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("HMMResult"))
				{
					HMMResult res = new HMMResult();
					readResultProvenance(vAnno.getProvenance(), res);
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
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("LRTResult"))
				{
					LRTResult res = new LRTResult();
					readResultProvenance(vAnno.getProvenance(), res);
					res.guiName = vAnno.getDescription();
					float[][] data = getGraph(vAnno, "data");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.data = data;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("DSSResult"))
				{
					DSSResult res = new DSSResult();
					readResultProvenance(vAnno.getProvenance(), res);
					res.guiName = vAnno.getDescription();
					float[][] data = getGraph(vAnno, "data");
					float[] thresholds = getData(vAnno, "thresholds");
					float threshold = getData(vAnno, "threshold")[0];
					res.data = data;
					res.thresholds = thresholds;
					res.threshold = threshold;
					res.status = JobStatus.COMPLETED;
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("CodeMLResult"))
				{
					CodeMLResult res = getCodeMLResult(vAnno);
					readResultProvenance(vAnno.getProvenance(), res);
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("MGResult"))
				{
					MGResult res = new MGResult();
					readResultProvenance(vAnno.getProvenance(), res);
					res.guiName = vAnno.getDescription();
					res.status = JobStatus.COMPLETED;
					for (AnnotationElement el : vAnno.getAnnotationElement())
					{
						SubstitutionModel mod = new SubstitutionModel();
						float[] data = el.getValue();
						mod.setName(el.getDescription());
						mod.setLnl(data[0]);
						mod.setAic1(data[1]);
						mod.setAic2(data[2]);
						mod.setBic(data[3]);
						res.models.add(mod);
					}
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("CodonWResult"))
				{
					CodonWResult res = new CodonWResult();
					readResultProvenance(vAnno.getProvenance(), res);
					res.guiName = vAnno.getDescription();
					res.status = JobStatus.COMPLETED;
					AnnotationElement el = vAnno.getAnnotationElement(0);
					// unmask the new line characters
					res.result = decodeString(el.getDescription());
					tAlign.addResult(res);
					mapper.registerObjects(res, vAnno);
				}
			}
		}
		
		Tree[] vTrees = vAlign.getTree();
		for (Tree vTree : vTrees)
		{
			TreeResult tTree = (TreeResult) mapper.getTopaliObject(vTree);
			if (tTree == null)
			{
				tTree = new TreeResult();
				tTree.setTreeStr(vTree.getNewick(0).getContent());
				tTree.guiName = vTree.getTitle();
				tTree.status = JobStatus.COMPLETED;

				for (Property prop : vTree.getProperty())
				{
					if (prop.getName().equals("start"))
						tTree.setPartitionStart(Integer.parseInt(prop
								.getContent()));
					else if (prop.getName().equals("end"))
						tTree.setPartitionEnd(Integer.parseInt(prop
								.getContent()));
				}

				readResultProvenance(vTree.getProvenance(), tTree);
				tAlign.addResult(tTree);
				mapper.registerObjects(tTree, vTree);
			}
		}
	}

	private CodeMLResult getCodeMLResult(AlignmentAnnotation vAnno)
	{
		CodeMLResult res = new CodeMLResult();
		res.status = JobStatus.COMPLETED;
		res.guiName = vAnno.getDescription();
		res.threshold = getData(vAnno, "threshold")[0];

		// deal with site models
		String[] models = new String[]
		{ CMLModel.MODEL_M0, CMLModel.MODEL_M1, CMLModel.MODEL_M1a,
				CMLModel.MODEL_M2, CMLModel.MODEL_M2a, CMLModel.MODEL_M3,
				CMLModel.MODEL_M7, CMLModel.MODEL_M8 };
		for (String m : models)
		{
			res.type = CodeMLResult.TYPE_SITEMODEL;
			CMLModel cmlModel = new CMLModel(m);
			boolean hasData = false;
			boolean hasGraph = false;

			float[][] graph = getGraph(vAnno, m);
			float[] data = getData(vAnno, m + "|parameters");
			if (data != null && data.length > 8)
			{
				cmlModel.likelihood = data[0];
				cmlModel.p = data[1];
				cmlModel.p0 = data[2];
				cmlModel.p1 = data[3];
				cmlModel.p2 = data[4];
				cmlModel.q = data[5];
				cmlModel.w0 = data[6];
				cmlModel.w1 = data[7];
				cmlModel.w2 = data[8];
				hasData = true;
			}
			if (graph != null)
			{
				cmlModel.setGraph(graph);
				hasGraph = true;
			}

			if (hasData || hasGraph)
				res.models.add(cmlModel);
		}

		// deal with branch models
		float[] data = null;
		int h = 0;
		while ((data = getData(vAnno, "H" + h + "|omegas")) != null)
		{
			res.type = CodeMLResult.TYPE_BRANCHMODEL;
			CMLHypothesis hypo = new CMLHypothesis();
			double[] omegas = new double[data.length];
			for (int j = 0; j < data.length; j++)
				omegas[j] = (double) data[j];
			hypo.omegas = omegas;
			hypo.likelihood = getData(vAnno, "H" + h + "|likelihood")[0];

			for (Property p : vAnno.getProperty())
			{
				if (p.getName().equals("H" + h + "|tree"))
				{
					hypo.tree = p.getContent();
				} else if (p.getName().equals("H" + h + "|omegatree"))
				{
					hypo.omegaTree = p.getContent();
				}
			}
			res.hypos.add(hypo);
			h++;
		}

		return res;
	}

	private void readAnnotations(AlignmentData tAlign, Alignment vAlign)
	{
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			Object tmp = mapper.getTopaliObject(vAnno);
			if (tmp == null)
			{
				if (vAnno.getType().equals("Partitions"))
				{
					PartitionAnnotations pAnnos = (PartitionAnnotations) tAlign
							.getTopaliAnnotations().getAnnotations(
									PartitionAnnotations.class);
					for (Seg s : vAnno.getSeg())
					{
						pAnnos.addRegion(new Region(s.getStart(), s.getEnd()));
					}
					mapper.registerObjects(pAnnos, vAnno);
				} else if (vAnno.getType().equals("CodingRegions"))
				{
					CDSAnnotations cAnnos = (CDSAnnotations) tAlign
							.getTopaliAnnotations().getAnnotations(
									CDSAnnotations.class);
					for (Seg s : vAnno.getSeg())
					{
						cAnnos.addRegion(new Region(s.getStart(), s.getEnd()));
					}
					mapper.registerObjects(cAnnos, vAnno);
				}
			} else
			{
				if (tmp instanceof PartitionAnnotations)
				{
					PartitionAnnotations pAnnos = (PartitionAnnotations) tmp;
					pAnnos.deleteAll();
					for (Seg s : vAnno.getSeg())
					{
						pAnnos.addRegion(new Region(s.getStart(), s.getEnd()));
					}
				} else if (tmp instanceof CDSAnnotations)
				{
					CDSAnnotations cAnnos = (CDSAnnotations) tmp;
					cAnnos.deleteAll();
					for (Seg s : vAnno.getSeg())
					{
						cAnnos.addRegion(new Region(s.getStart(), s.getEnd()));
					}
				}
			}
		}
	}

	private float[][] getGraph(AlignmentAnnotation vAnno, String desc)
	{

		LinkedList<Float> positions = new LinkedList<Float>();
		LinkedList<float[]> values = new LinkedList<float[]>();

		AnnotationElement[] els = vAnno.getAnnotationElement();

		for (int i = 0; i < els.length; i++)
		{
			int pos = (int) els[i].getPosition();
			float[] value = els[i].getValue();
			String description = els[i].getDescription();
			if (description.equals(desc))
			{
				positions.add((float) pos);
				values.add(value);
			}
		}

		// if there is no data matching the desc return null
		if (positions.size() <= 0)
			return null;

		// determine the max. size of the values array
		int maxSize = 0;
		for (int j = 0; j < positions.size(); j++)
		{
			if (values.get(j).length > maxSize)
				maxSize = values.get(j).length;
		}

		float[][] result = new float[positions.size()][maxSize + 1];
		for (int j = 0; j < positions.size(); j++)
		{
			result[j][0] = positions.get(j);

			for (int k = 0; k < values.get(j).length; k++)
				result[j][k + 1] = values.get(j)[k];
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
				vDSSequence.setName(tSeq.getName());
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
			vSeq.setName(tSeq.getName());
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
			// the corresponding vamsas object to tRes could be either a
			// AlignmentAnnotation or a Tree
			Object tmp = mapper.getVamsasObject(tRes);
			AlignmentAnnotation vAnno = null;
			Tree vTree = null;
			if (tmp != null)
			{
				if (tmp instanceof AlignmentAnnotation)
					vAnno = (AlignmentAnnotation) tmp;
				else if (tmp instanceof Tree)
					vTree = (Tree) tmp;
			}

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
				vAnno.setProvenance(getResultProvenance(tRes));

				if (tRes instanceof PDMResult)
				{
					PDMResult result = (PDMResult) tRes;
					vAnno.setType("PDMResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					// vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.glbData, "glbData");
					addGraph(vAnno, result.locData, "locData");
					addGraph(vAnno, result.histograms, "histograms");
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
					// vAnno.setProvenance(getDummyProvenance());
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
					// vAnno.setProvenance(getDummyProvenance());
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
					// vAnno.setProvenance(getDummyProvenance());
					addGraph(vAnno, result.data, "data");
					addData(vAnno, result.thresholds, "thresholds");
					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}

				else if (tRes instanceof CodeMLResult)
				{
					CodeMLResult result = (CodeMLResult) tRes;
					vAnno.setType("CodeMLResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(true);
					// vAnno.setProvenance(getDummyProvenance());
					// applies for site models
					for (int i = 0; i < result.models.size(); i++)
					{
						CMLModel mod = result.models.get(i);
						float[][] graph = mod.getGraph();
						if (graph != null)
						{
							addGraph(vAnno, graph, mod.model);
						}
						if (mod.likelihood > -1 || mod.p > -1 || mod.p0 > -1
								|| mod.p1 > -1 || mod.p2 > -1 || mod.q > -1
								|| mod.w0 > -1 || mod.w1 > -1 || mod.w2 > -1)
						{
							float[] data = new float[]
							{ (float) mod.likelihood, (float) mod.p,
									(float) mod.p0, (float) mod.p1,
									(float) mod.p2, (float) mod.q,
									(float) mod.w0, (float) mod.w1,
									(float) mod.w2 };
							addData(vAnno, data, mod.model + "|parameters");
						}
					}
					// applies for branch models
					for (int i = 0; i < result.hypos.size(); i++)
					{
						CMLHypothesis hypo = result.hypos.get(i);
						addData(vAnno, (float) hypo.likelihood, "H" + i
								+ "|likelihood");
						float[] data = new float[hypo.omegas.length];
						for (int j = 0; j < data.length; j++)
							data[j] = (float) hypo.omegas[j];
						addData(vAnno, data, "H" + i + "|omegas");

						Property prop = new Property();
						prop.setName("H" + i + "|tree");
						prop.setType("tree");
						prop.setContent(hypo.tree);
						vAnno.addProperty(prop);

						Property prop2 = new Property();
						prop2.setName("H" + i + "|omegatree");
						prop2.setType("tree");
						prop2.setContent(hypo.omegaTree);
						vAnno.addProperty(prop2);
					}

					addData(vAnno, (float) result.threshold, "threshold");
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}

				else if (tRes instanceof MGResult)
				{
					MGResult result = (MGResult) tRes;
					vAnno.setType("MGResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(false);
					// vAnno.setProvenance(getDummyProvenance());

					for (SubstitutionModel m : result.models)
					{
						AnnotationElement el = new AnnotationElement();
						el.setPosition(-1);
						el.setDescription(m.getName());
						el.setValue(new float[]
						{ (float) m.getLnl(), (float) m.getAic1(),
								(float) m.getAic2(), (float) m.getBic() });
						vAnno.addAnnotationElement(el);
					}

					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}

				else if (tRes instanceof CodonWResult)
				{
					CodonWResult result = (CodonWResult) tRes;
					vAnno.setType("CodonWResult");
					vAnno.setDescription(tRes.guiName);
					vAnno.setGraph(false);

					AnnotationElement el = new AnnotationElement();
					el.setPosition(-1);
					// mask new line characters in some way, so that they are
					// not removed
					String res = encodeString(result.result);
					el.setDescription(res);
					el.setValue(new float[]
					{});
					vAnno.addAnnotationElement(el);

					// vAnno.setProvenance(getDummyProvenance());
					mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}
			}

			if (tRes instanceof TreeResult && vTree == null)
			{
				TreeResult result = (TreeResult) tRes;
				vTree = new Tree();
				vTree.setTitle(result.getTitle());
				Newick nw = new Newick();
				nw.setContent(result.getTreeStr());
				vTree.addNewick(nw);

				Property p1 = new Property();
				p1.setType("int");
				p1.setName("start");
				p1.setContent("" + result.getPartitionStart());
				vTree.addProperty(p1);

				Property p2 = new Property();
				p2.setType("int");
				p2.setName("end");
				p2.setContent("" + result.getPartitionEnd());
				vTree.addProperty(p2);

				vTree.setTitle(result.guiName);

				// vTree.setProvenance(getDummyProvenance());
				vTree.setProvenance(getResultProvenance(tRes));
				mapper.registerObjects(tRes, vTree);
				vAlign.addTree(vTree);
			}
		}
	}

	private void writeAnnotations(AlignmentData tAlign, Alignment vAlign)
	{
		PartitionAnnotations partAnnos = (PartitionAnnotations) tAlign
				.getTopaliAnnotations().getAnnotations(
						PartitionAnnotations.class);
		AlignmentAnnotation vAnno = (AlignmentAnnotation) mapper
				.getVamsasObject(partAnnos);
		if (vAnno == null)
		{
			vAnno = new AlignmentAnnotation();
			vAnno.setProvenance(getDummyProvenance());
			vAnno.setType("Partitions");
			vAnno.setGraph(false);
			boolean added = false;
			for (Region region : partAnnos)
			{
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setInclusive(true);
				seg.setStart(start);
				seg.setEnd(end);
				vAnno.addSeg(seg);
				added = true;
			}
			if (added)
			{
				mapper.registerObjects(partAnnos, vAnno);
				vAlign.addAlignmentAnnotation(vAnno);
			}
		} else
		{
			vAnno.removeAllSeg();
			for (Region region : partAnnos)
			{
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setInclusive(true);
				seg.setStart(start);
				seg.setEnd(end);
				vAnno.addSeg(seg);
			}
		}

		CDSAnnotations cdsAnnos = (CDSAnnotations) tAlign
				.getTopaliAnnotations().getAnnotations(CDSAnnotations.class);
		vAnno = (AlignmentAnnotation) mapper.getVamsasObject(cdsAnnos);
		if (vAnno == null)
		{
			vAnno = new AlignmentAnnotation();
			vAnno.setProvenance(getDummyProvenance());
			vAnno.setType("CodingRegions");
			vAnno.setGraph(false);
			boolean added = false;
			for (Region region : cdsAnnos)
			{
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setInclusive(true);
				seg.setStart(start);
				seg.setEnd(end);
				vAnno.addSeg(seg);
				added = true;
			}
			if (added)
			{
				mapper.registerObjects(cdsAnnos, vAnno);
				vAlign.addAlignmentAnnotation(vAnno);
			}
		} else
		{
			vAnno.removeAllSeg();
			for (Region region : cdsAnnos)
			{
				int start = region.getS();
				int end = region.getE();
				Seg seg = new Seg();
				seg.setInclusive(true);
				seg.setStart(start);
				seg.setEnd(end);
				vAnno.addSeg(seg);
			}
		}
	}

	private void addGraph(AlignmentAnnotation anno, float[][] data, String desc)
	{
		for (int i = 0; i < data.length; i++)
		{
			int pos = (int) (data[i][0]);
			float[] values = new float[data[i].length - 1];
			for (int j = 1; j < data[i].length; j++)
				values[j - 1] = data[i][j];

			AnnotationElement el = new AnnotationElement();
			el.setDescription(desc);
			el.setPosition(pos);
			el.setValue(values);
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

	private Provenance getResultProvenance(AnalysisResult result)
	{
		Provenance p = new Provenance();

		Entry entry = new Entry();
		entry.setApp(VamsasManager.app.getClientUrn());
		entry.setUser(VamsasManager.user.getFullName());
		entry.setDate(new Date());
		entry.setAction(result.getClass().getName());
		Class c = result.getClass();
		for (Field field : c.getFields())
		{
			int mod = field.getModifiers();
			// don't set protected variables:
			if (Modifier.isFinal(mod) || Modifier.isPrivate(mod)
					|| Modifier.isProtected(mod) || Modifier.isStatic(mod))
				continue;

			if (field.getType().isPrimitive()
					|| field.getType() == String.class)
			{
				try
				{
					Object o = field.get(result);
					if (o == null)
						continue;
					Property prop = new Property();
					prop.setName(field.getName());
					prop.setType(field.getType().getName());
					prop.setContent(encodeString(o.toString()));
					entry.addProperty(prop);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		p.addEntry(entry);

		if (result instanceof AlignmentResult)
		{
			entry = new Entry();
			entry.setApp(VamsasManager.app.getClientUrn());
			entry.setUser(VamsasManager.user.getFullName());
			entry.setDate(new Date());
			entry.setAction("Sequences");
			for (String seq : ((AlignmentResult) result).selectedSeqs)
			{
				Property prop = new Property();
				prop.setName("Sequence");
				prop.setType("String");
				prop.setContent(seq);
				entry.addProperty(prop);
			}
			p.addEntry(entry);
		}

		return p;
	}

	private void readResultProvenance(Provenance prov, AnalysisResult result)
	{
		Entry entry = prov.getEntry(0);

		Class c;
		try
		{
			c = Class.forName(entry.getAction());
		} catch (ClassNotFoundException e1)
		{
			e1.printStackTrace();
			return;
		}

		for (Property prop : entry.getProperty())
		{
			try
			{
				Field field = c.getField(prop.getName());
				int mod = field.getModifiers();
				// don't set protected variables:
				if (Modifier.isFinal(mod) || Modifier.isPrivate(mod)
						|| Modifier.isProtected(mod) || Modifier.isStatic(mod))
					continue;

				Class type = field.getType();
				Object value = null;
				if (type.isPrimitive())
				{
					if (field.getGenericType() == Boolean.TYPE)
						value = Boolean.parseBoolean(prop.getContent());
					else if (field.getGenericType() == Character.TYPE)
						value = new Character(prop.getContent().charAt(0));
					else if (field.getGenericType() == Integer.TYPE)
						value = Integer.parseInt(prop.getContent());
					else if (field.getGenericType() == Long.TYPE)
						value = Long.parseLong(prop.getContent());
					else if (field.getGenericType() == Float.TYPE)
						value = Float.parseFloat(prop.getContent());
					else if (field.getGenericType() == Double.TYPE)
						value = Double.parseDouble(prop.getContent());
				} else
					value = type.cast((Object) prop.getContent());

				if (value instanceof String)
					value = decodeString((String) value);

				field.set(result, value);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (result instanceof AlignmentResult)
		{
			entry = prov.getEntry(1);
			LinkedList<String> tmp = new LinkedList<String>();
			for (Property p : entry.getProperty())
			{
				tmp.add(p.getContent());
			}
			String[] seqs = new String[tmp.size()];
			seqs = tmp.toArray(seqs);
			((AlignmentResult) result).selectedSeqs = seqs;
		}
	}

	private String encodeString(String s)
	{
		s = s.replaceAll("\n", "{n}");
		s = s.replaceAll("\t", "{t}");
		return s;
	}

	private String decodeString(String s)
	{
		s = s.replaceAll("\\{n\\}", "\n");
		s = s.replaceAll("\\{t\\}", "\t");
		return s;
	}
}