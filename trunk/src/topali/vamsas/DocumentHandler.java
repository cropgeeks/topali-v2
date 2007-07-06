package topali.vamsas;

import java.lang.reflect.*;
import java.util.*;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import topali.analyses.MakeNA;
import topali.cluster.JobStatus;
import topali.data.*;
import topali.data.Sequence;
import topali.data.RegionAnnotations.Region;
import topali.fileio.AlignmentLoadException;
import topali.gui.Project;
import uk.ac.vamsas.client.IClientDocument;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.utils.SymbolDictionary;


class DocumentHandler
{
	Logger log = Logger.getLogger(this.getClass());

	private Project project;

	private IClientDocument doc;

	private DataSet dataset;

	private LinkedList<AlignmentData> cdnaAlignments = new LinkedList<AlignmentData>();
	
	private String tGapChar = "-";
	private String tGapCharEscaped = "-";
	private String vGapChar = ".";
	private String vGapCharEscaped = "\\.";
	
	public DocumentHandler(Project proj, IClientDocument doc)
	{
		this.project = proj;
		this.doc = doc;

		if (doc.getVamsasRoots()[0].getDataSetCount() < 1)
		{
			
			this.dataset = new DataSet();
			this.dataset.setProvenance(getDummyProvenance());
			doc.getVamsasRoots()[0].addDataSet(this.dataset);
		}
		else
			this.dataset = doc.getVamsasRoots()[0].getDataSet(0);

		VamsasManager.mapper.registerClientDocument(doc);

	}

	/**
	 * Write changes to the VAMSAS document
	 * 
	 * @throws Exception
	 */
	void writeToDocument() throws Exception
	{
		LinkedList<AlignmentData> tDatasets = project.getDatasets();

		for (AlignmentData tAlign : tDatasets)
		{
			writeAlignment(tAlign);
		}

	}

	/**
	 * Read changes from the VAMSAS document
	 * 
	 * @throws Exception
	 */
	void readFromDocument() throws Exception
	{
			Alignment[] tDatasets = dataset.getAlignment();
			for (Alignment vAlign : tDatasets)
			{
				AlignmentData tAlign = (AlignmentData) VamsasManager.mapper
						.getTopaliObject(vAlign);
				if (tAlign == null) // it's a new alignment
				{
					tAlign = new AlignmentData();
					readAlignment(vAlign, tAlign);
					// can't add a non aligned dataset
					if (!tAlign.getSequenceSet().getParams().isAligned())
						continue;

					tAlign.setActiveRegion(1, tAlign.getSequenceSet()
							.getLength());
					VamsasManager.mapper.registerObjects(tAlign, vAlign);
					project.addDataSet(tAlign);
				} else
				// it's a existing alignment, update it
				{
					readAlignment(vAlign, tAlign);
				}
			}

			// we've found some corresponding cdna alignments, add them
			if (cdnaAlignments.size() > 0)
			{
				int r = JOptionPane.showConfirmDialog(null, "Found some corresponding protein-dna sequences. Do you want me to create a protein guided cDNA alignment?", "cDNA found", JOptionPane.YES_NO_OPTION);
				if(r==JOptionPane.YES_OPTION) {
					for (AlignmentData cdna : cdnaAlignments)
					{
						project.addDataSet(cdna);
					}
				}
				
				cdnaAlignments.clear();
			}
	}

	// ----------
	// Vamsas -> TOPALi methods:

	/**
	 * Synchronizes a vamsas alignment with a topali alignment
	 */
	private void readAlignment(Alignment vAlign, AlignmentData tAlign)
			throws Exception
	{
		String name = getAlignmentName(vAlign);

		this.vGapChar = vAlign.getGapChar();
		if(vGapChar.equals("."))
			vGapCharEscaped = "\\"+vGapChar;
		else
			vGapCharEscaped = vGapChar;
		
		log.info("Read alignment " + name);

		tAlign.name = name;
		SequenceSet ss = tAlign.getSequenceSet();
		if (ss == null)
		{
			ss = new SequenceSet();
			ss.setOverview("");
			tAlign.setSequenceSet(ss);
		}
		readSequences(ss, vAlign);

		try
		{
			ss.checkValidity();
		} catch (AlignmentLoadException e)
		{
			log.info(tAlign.name+" is not aligned.");
			ss.getParams().setAligned(false);
		}

		readAnalysisResults(tAlign, vAlign);

		readAnnotations(tAlign, vAlign);
	}

	/**
	 * Extracts all sequences from a vamsas alignment and adds them to a topali
	 * sequenceset
	 * 
	 * @param ss
	 * @param vAlign
	 */
	private void readSequences(SequenceSet ss, Alignment vAlign)
	{
		SequenceSet cdnaSS = new SequenceSet();

		for (AlignmentSequence vSeq : vAlign.getAlignmentSequence())
		{
			Sequence tSeq = (Sequence) VamsasManager.mapper.getTopaliObject(vSeq);

			if (tSeq == null)
			{
				tSeq = new Sequence();
				// actual sequence must be set before adding to sequenceset
				tSeq.setSequence(vSeq.getSequence().replaceAll(vGapCharEscaped, tGapChar));
				tSeq.setName(vSeq.getName().replaceAll("\\s+", "_"));

				log.info("Read sequence " + vSeq.getName());

				VamsasManager.mapper.registerObjects(tSeq, vSeq);
				ss.addSequence(tSeq);

				// check the referenced dataset sequence, if we deal with a
				// protein sequence.
				// if so, check if we have a corresponding cdna for it
				Object tmp = vSeq.getRefid();
				if (tmp != null)
				{
					uk.ac.vamsas.objects.core.Sequence refSeq = (uk.ac.vamsas.objects.core.Sequence) tmp;
					if (refSeq.getDictionary()
							.equals(SymbolDictionary.STANDARD_AA))
					{
						readCDNA(cdnaSS, refSeq, vSeq);
					}
				}

			} else
			{
				tSeq.setSequence(vSeq.getSequence());
				tSeq.setName(vSeq.getName().replaceAll("\\s+", "_"));
			}
		}

		if (cdnaSS.getSize() > 0)
		{
			String name = getAlignmentName(vAlign);
			MakeNA mna = new MakeNA(cdnaSS, ss, name+" (cDNA)");
			log.info("Generating protein guided alignment for "+name);
			if(mna.doConversion(false, VamsasManager.tDNAProtMapping)) {
				AlignmentData cdnaData = mna.getAlignmentData();
				for(Sequence s : cdnaData.getSequenceSet().getSequences()) {
					s.setName(s.getName()+"_cDNA");
				}
				cdnaAlignments.add(cdnaData);
			}
			else
				log.warn("Could not create guided alignment!");
		}
	}
	
	/**
	 * Looks for a corresponding cDNA and adds it to the given SequenceSet
	 * 
	 * @param cdnaSS
	 * @param dsProtSeq
	 */
	private void readCDNA(SequenceSet cdnaSS,
			uk.ac.vamsas.objects.core.Sequence dsProtSeq, AlignmentSequence protSeq)
	{
		Sequence tSeq = null;

		log.info("Try to get cdna for " + dsProtSeq.getName());

		for (DataSet dataset : doc.getVamsasRoots()[0].getDataSet())
		{
			// either find corresponding DNA dbref
			boolean found = false;
			for (DbRef dbref : dsProtSeq.getDbRef())
			{
				for (uk.ac.vamsas.objects.core.Sequence dnaSeq : dataset
						.getSequence())
				{
					if (!dnaSeq.getDictionary().equals(
							SymbolDictionary.STANDARD_NA))
						continue;
					for (DbRef dnadbref : dnaSeq.getDbRef())
					{
						//compare the dbref with dnadbref
						if (compareDBRef(dnadbref, dbref))
						{
							int start;
							int end;
							try
							{
								start = dnadbref.getMap()[0].getLocal().getSeg(0).getStart()-1;
								end = dnadbref.getMap()[0].getLocal().getSeg(0).getEnd()-3;
							} catch (Exception e)
							{
								continue;
							}
							tSeq = new Sequence();
							String seqString = dnaSeq.getSequence().replaceAll(vGapCharEscaped, tGapChar); 
							tSeq.setSequence(seqString.substring(start, end));
							tSeq.setName(protSeq.getName().replaceAll("\\s+", "_"));
							found = true;
							log.info("Found matching dbrefs: "+dnaSeq.getName());
							break;
						}
					}

					if (found)
						break;
				}

				if (found)
					break;
			}

			if (found)
				break;
			
			if (!found)
			{
				// or search sequence mapping
				for (SequenceMapping smap : dataset.getSequenceMapping())
				{
					uk.ac.vamsas.objects.core.Sequence map = (uk.ac.vamsas.objects.core.Sequence) smap.getMap();
					if (map.equals(dsProtSeq))
					{
						log.info("Found matching sequence mapping: "+dsProtSeq.getName());
						uk.ac.vamsas.objects.core.Sequence dna = (uk.ac.vamsas.objects.core.Sequence) smap
								.getLoc();
						int s1 = (int)dna.getStart();
						int s2 = smap.getLocal().getSeg()[0].getStart();
						//int e1 = (int)dna.getEnd(); 
						int e2 = smap.getLocal().getSeg()[0].getEnd();
						int start = s2-s1;
						int end = start+(e2-s2)+1; 
						String seqString = dna.getSequence().replaceAll(vGapCharEscaped, tGapChar); 
						String sequence = seqString.substring(start, end);
						tSeq = new Sequence();
						tSeq.setSequence(sequence);
						tSeq.setName(protSeq.getName().replaceAll("\\s+", "_"));
						found = true;
					}
					
					if(found)
						break;
				}
			}
		}

		// if a cdna was found, add it to the sequenceset
		if (tSeq != null) {
			cdnaSS.addSequence(tSeq);
		}
	}

	private boolean compareDBRef(DbRef ref1, DbRef ref2)
	{
		if (ref1 == null || ref2 == null)
			return false;
		else
			return (ref1.getSource().equals(ref2.getSource()) && ref1
					.getAccessionId().equals(ref2.getAccessionId()));
	}

	/**
	 * Gets all analysis results from vamsas alignment and adds them to topali
	 * alignment data
	 * 
	 * @param tAlign
	 * @param vAlign
	 */
	private void readAnalysisResults(AlignmentData tAlign, Alignment vAlign)
	{
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			AnalysisResult tResult;
			try
			{
				tResult = (AnalysisResult) VamsasManager.mapper.getTopaliObject(vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
				}

				else if (vAnno.getType().equals("CodeMLResult"))
				{
					CodeMLResult res = getCodeMLResult(vAnno);
					readResultProvenance(vAnno.getProvenance(), res);
					tAlign.addResult(res);
					VamsasManager.mapper.registerObjects(res, vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
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
					VamsasManager.mapper.registerObjects(res, vAnno);
				}
			}
		}

		Tree[] vTrees = vAlign.getTree();
		for (Tree vTree : vTrees)
		{
			TreeResult tTree = (TreeResult) VamsasManager.mapper.getTopaliObject(vTree);
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
					else if (prop.getName().equals("safeNames"))
						tTree.setTreeStr(prop.getContent());
				}

				readResultProvenance(vTree.getProvenance(), tTree);
				tAlign.addResult(tTree);
				VamsasManager.mapper.registerObjects(tTree, vTree);
			}
		}
	}

	/**
	 * Transforms codeml result information contained in a vamsas annotation to
	 * a CodeMLResult
	 * 
	 * @param vAnno
	 * @return
	 */
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

	/**
	 * Reads annotations from a vamsas alignment and adds them to topali
	 * alignment data
	 * 
	 * @param tAlign
	 * @param vAlign
	 */
	private void readAnnotations(AlignmentData tAlign, Alignment vAlign)
	{
		for (AlignmentAnnotation vAnno : vAlign.getAlignmentAnnotation())
		{
			Object tmp = VamsasManager.mapper.getTopaliObject(vAnno);
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
					VamsasManager.mapper.registerObjects(pAnnos, vAnno);
				} else if (vAnno.getType().equals("CodingRegions"))
				{
					CDSAnnotations cAnnos = (CDSAnnotations) tAlign
							.getTopaliAnnotations().getAnnotations(
									CDSAnnotations.class);
					for (Seg s : vAnno.getSeg())
					{
						cAnnos.addRegion(new Region(s.getStart(), s.getEnd()));
					}
					VamsasManager.mapper.registerObjects(cAnnos, vAnno);
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

	/**
	 * Extracts graph values from vamsas annotation
	 * 
	 * @param vAnno
	 * @param desc
	 * @return
	 */
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

	/**
	 * Get certain values from a vamsas annotation
	 * 
	 * @param vAnno
	 * @param desc
	 * @return
	 */
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

	/**
	 * Find the name of a certain vamsas alignment
	 * 
	 * @param vAlign
	 * @return
	 */
	private String getAlignmentName(Alignment vAlign)
	{
		Property[] props = vAlign.getProperty();
		for (Property prop : props)
		{
			if (prop.getName().endsWith("itle"))
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
		Alignment vAlign = (Alignment) VamsasManager.mapper.getVamsasObject(tAlign);
		
		if (vAlign == null)
		{
			log.info("Creating new vamsas alignment");

			// Create a new vamsas alignment
			vAlign = new Alignment();
			vAlign.setProvenance(getDummyProvenance("added"));
			vAlign.setGapChar(vGapChar);
			Property title = new Property();
			title.setName("title");
			title.setType("string");
			title.setContent(tAlign.name);
			vAlign.addProperty(title);

			dataset.addAlignment(vAlign);

			// Link it with the TOPALi data set
			VamsasManager.mapper.registerObjects(tAlign, vAlign);
		}

		SequenceSet tSequenceSet = tAlign.getSequenceSet();
		writeAlignmentSequences(tSequenceSet, vAlign);

		writeAnalysisResults(tAlign, vAlign);

		writeAnnotations(tAlign, vAlign);
	}

	/**
	 * Adds topali sequences to vamsas alignment
	 * 
	 * @param tSequenceSet
	 * @param vAlign
	 */
	private void writeAlignmentSequences(SequenceSet tSequenceSet,
			Alignment vAlign)
	{
		for (Sequence tSeq : tSequenceSet.getSequences())
		{
			AlignmentSequence vSeq = (AlignmentSequence) VamsasManager.mapper
					.getVamsasObject(tSeq);

			uk.ac.vamsas.objects.core.Sequence vDSSequence = null;

			String seqString = tSeq.getSequence().replaceAll(tGapCharEscaped, vGapChar);
			
			if (vSeq == null)
			{
				vSeq = new AlignmentSequence();
				VamsasManager.mapper.registerObjects(tSeq, vSeq);
				vAlign.addAlignmentSequence(vSeq);

				// Add sequence to vamsas dataset:
				vDSSequence = new uk.ac.vamsas.objects.core.Sequence();
				vDSSequence.setSequence(seqString);
				vDSSequence.setName(tSeq.getName());
				vDSSequence.setStart(0);
				vDSSequence.setEnd(tSeq.getLength() - 1);
				if (tSequenceSet.getParams().isDNA())
					vDSSequence.setDictionary(SymbolDictionary.STANDARD_NA);
				else
					vDSSequence.setDictionary(SymbolDictionary.STANDARD_AA);
				dataset.addSequence(vDSSequence);

				if(VamsasManager.tDNAProtMapping.containsKey(tSeq)) {
					AlignmentSequence seq1 = (AlignmentSequence)VamsasManager.mapper.getVamsasObject(tSeq);
					Sequence tmp = VamsasManager.tDNAProtMapping.get(tSeq);
					if(tmp==null)
						break;
					AlignmentSequence seq2 = (AlignmentSequence)VamsasManager.mapper.getVamsasObject(tmp);
					SequenceMapping mapping = new SequenceMapping();
					mapping.setLoc(seq1);
					mapping.setMap(seq2);
					Local local = new Local();
					Seg seg = new Seg();
					seg.setStart(1);
					seg.setEnd(tSeq.getLength());
					seg.setInclusive(true);
					local.addSeg(seg);
					mapping.setLocal(local);
					Mapped mapped = new Mapped();
					Seg seg2 = new Seg();
					seg2.setStart(1);
					seg2.setEnd(seq2.getSequence().length());
					seg2.setInclusive(true);
					mapped.addSeg(seg2);
					mapping.setMapped(mapped);
					mapping.setProvenance(getDummyProvenance("Protein guided cDNA alignment"));
					dataset.addSequenceMapping(mapping);
					VamsasManager.tDNAProtMapping.remove(tSeq);
				}
				
				VamsasManager.mapper.registerObjects(tSeq, vDSSequence);
				VamsasManager.mapper.registerObjects(tSeq, vSeq);
			}

			vSeq.setSequence(seqString);
			vSeq.setName(tSeq.getName());
			vSeq.setStart(0);
			vSeq.setEnd(tSeq.getLength() - 1);
			// new sequences have to be linked to the corresponding dataset
			// sequences
			if (vDSSequence != null)
				vSeq.setRefid(vDSSequence);
			else
			{
				// if sequence already exists, we also have to update the
				// reference dataset sequence
//				vDSSequence = (uk.ac.vamsas.objects.core.Sequence) vSeq
//						.getRefid();
//				vDSSequence.setSequence(tSeq.getSequence());
//				vDSSequence.setName(tSeq.getName());
//				vDSSequence.setStart(0);
//				vDSSequence.setEnd(tSeq.getLength() - 1);
			}
		}
	}

	/**
	 * Adds topali analysis results to a vamsas alignment
	 * 
	 * @param tAlign
	 * @param vAlign
	 */
	private void writeAnalysisResults(AlignmentData tAlign, Alignment vAlign)
	{
		for (AnalysisResult tRes : tAlign.getResults())
		{
			// the corresponding vamsas object to tRes could be either a
			// AlignmentAnnotation or a Tree
			Object tmp = VamsasManager.mapper.getVamsasObject(tRes);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
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

					VamsasManager.mapper.registerObjects(tRes, vAnno);
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
					VamsasManager.mapper.registerObjects(tRes, vAnno);
					vAlign.addAlignmentAnnotation(vAnno);
				}
			}

			if (tRes instanceof TreeResult && vTree == null)
			{
				TreeResult result = (TreeResult) tRes;
				vTree = new Tree();
				vTree.setTitle(result.getTitle());
				try
				{
					Newick nw = new Newick();
					// Jalview needs real names
					String treeString = result.getTreeStrActual(tAlign.getSequenceSet());			
					nw.setContent(treeString);
					vTree.addNewick(nw);
				} catch (Exception e)
				{
					log.warn("Problem writing newick tree to VAMSAS document.",
							e);
				}

				// topali needs safe names
				Property p = new Property();
				p.setType("newick");
				p.setName("safeNames");
				;
				p.setContent(result.getTreeStr());
				vTree.addProperty(p);

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
				VamsasManager.mapper.registerObjects(tRes, vTree);
				vAlign.addTree(vTree);
			}
		}
	}

	/**
	 * Adds topali annotations to vamsas alignment
	 * 
	 * @param tAlign
	 * @param vAlign
	 */
	private void writeAnnotations(AlignmentData tAlign, Alignment vAlign)
	{
		PartitionAnnotations partAnnos = (PartitionAnnotations) tAlign
				.getTopaliAnnotations().getAnnotations(
						PartitionAnnotations.class);
		AlignmentAnnotation vAnno = (AlignmentAnnotation) VamsasManager.mapper
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
				VamsasManager.mapper.registerObjects(partAnnos, vAnno);
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
		vAnno = (AlignmentAnnotation)VamsasManager. mapper.getVamsasObject(cdsAnnos);
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
				VamsasManager.mapper.registerObjects(cdsAnnos, vAnno);
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

	/**
	 * Add graph values to a vamsas alignment annotation
	 * 
	 * @param anno
	 * @param data
	 * @param desc
	 */
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

	/**
	 * Add a value to a vamsas alignment annotation
	 * 
	 * @param anno
	 * @param data
	 * @param desc
	 */
	private void addData(AlignmentAnnotation anno, float data, String desc)
	{
		addData(anno, new float[]
		{ data }, desc);
	}

	/**
	 * Add values to a vamsas alignment annotation
	 * 
	 * @param anno
	 * @param data
	 * @param desc
	 */
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

	/**
	 * Reads all public fields from a topali analysis result and creates a
	 * provencance entry from this information
	 * 
	 * @param result
	 * @return
	 */
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
					log.warn("Problem creating VAMSAS provenance entry", e);
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

	/**
	 * Restore all public fields of a topali analysis result from a vamsas
	 * provenance entry
	 * 
	 * @param prov
	 * @param result
	 */
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
				log.warn("Problem reading VAMSAS provenance entry", e);
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