// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.util.*;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import topali.analyses.MakeNA;
import topali.data.*;
import topali.data.Sequence;
import topali.fileio.AlignmentLoadException;
import topali.gui.Project;
import uk.ac.vamsas.client.IClientDocument;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

public class VamsasDocumentHandler
{
	Logger log = Logger.getLogger(this.getClass());
	
	private Project project;
	private IClientDocument doc;
	
	//Shortcut to VamsasManager.mapper
	ObjectMapper map;
	
	//These fields are dynamically set during reading/writing process (be carefull!)
	Alignment currentVAlignment = null;
	AlignmentData currentTAlignment = null;
	LinkedList<AlignmentData> cdnaDatasets = new LinkedList<AlignmentData>();
	SequenceSet cdnaSS = new SequenceSet();
	
	public VamsasDocumentHandler(Project proj, IClientDocument doc)
	{
		this.project = proj;
		this.doc = doc;
		VamsasManager.mapper.registerClientDocument(doc);
		map = VamsasManager.mapper;
	}
	
	public void read() {
		for(VAMSAS vamsas : doc.getVamsasRoots()) {
			for(DataSet dataset : vamsas.getDataSet()) {
				for(Alignment alignment : dataset.getAlignment()) {
					readAlignment(alignment);
				}
			}
		}
		
		for(AlignmentData cdnas : cdnaDatasets) {
			if(!this.project.containsDataset(cdnas)) {
				int option = JOptionPane.showConfirmDialog(null, "Found DNA with a corresponding protein alignment. Create a protein guided DNA alignment?", "Guided alignment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(option==JOptionPane.YES_OPTION) 
					this.project.addDataSet(cdnas);
			}
		}
		cdnaDatasets.clear();
	}
	
	public void write() {
		for(AlignmentData align : project.getDatasets()) {
			currentTAlignment = align;
			writeAlignment();
		}
	}
	
	private void readAlignment(Alignment vAlign) {
		AlignmentData tAlign = (AlignmentData)map.getTopaliObject(vAlign.getId());
		if(tAlign==null) {
			tAlign = new AlignmentData();
			String alignName = "VAMSAS alignment";
			Property[] props = vAlign.getProperty();
			for (Property prop : props)
			{
				if (prop.getName().endsWith("itle")) {
					alignName = prop.getContent();
					break;
				}
			}
			tAlign.name = alignName;
			
			SequenceSet ss = new SequenceSet();
			ss.setOverview("");
			for(AlignmentSequence vSeq : vAlign.getAlignmentSequence()) {
				Sequence tSeq = readSequence(vSeq);
				ss.addSequence(tSeq);
				map.registerObjects(tSeq, vSeq);
			}
			try
			{
				ss.checkValidity();
			} catch (AlignmentLoadException e)
			{
				log.warn("Alignment '"+tAlign.name+"' is not aligned!", e);
				return;
			}
			tAlign.setSequenceSet(ss);
			
			if(!this.project.containsDataset(tAlign)) {
				map.registerObjects(tAlign, vAlign);
				this.project.addDataSet(tAlign);
			}
			
			if (cdnaSS.getSize() > 0)
			{
					String name = getAlignmentName(vAlign);
					MakeNA mna = new MakeNA(cdnaSS, ss, name+" (cDNA)");
					log.info("Generating protein guided alignment for "+name);
					if(mna.doConversion(false, true)) {
						AlignmentData cdnaData = mna.getAlignmentData();
						for(Sequence s : cdnaData.getSequenceSet().getSequences()) {
							s.setName(s.getName()+"_cDNA");
						}
						cdnaDatasets.add(cdnaData);
					}
					else
						log.warn("Could not create guided alignment!");
			}
		}
		
		//The alignment already exists, check for changes
		else {
			for(AlignmentSequence vSeq : vAlign.getAlignmentSequence()) {
				Sequence tSeq = (Sequence)map.getTopaliObject(vSeq);
				if(tSeq==null) {
					String seq = vSeq.getSequence().replaceAll("\\W", "-");
					String name = vSeq.getName().replaceAll("\\s+", "_");
					tSeq = new Sequence();
					tSeq.setName(name);
					tSeq.setSequence(seq);
					tAlign.getSequenceSet().addSequence(tSeq);
					map.registerObjects(tSeq, vSeq);
				}
				else {
					String seq = vSeq.getSequence().replaceAll("\\W", "-");
					String name = vSeq.getName().replaceAll("\\s+", "_");
					tSeq.setName(name);
					tSeq.setSequence(seq);
				}
			}
		}
	}

	private Sequence readSequence(AlignmentSequence vSeq) {
		//check the referenced dataset sequence, if we deal with a
		// protein sequence.
		// if so, check if we have a corresponding cdna for it
		Object tmp = vSeq.getRefid();
		if (tmp != null)
		{
			uk.ac.vamsas.objects.core.Sequence refSeq = (uk.ac.vamsas.objects.core.Sequence) tmp;
			if (refSeq.getDictionary()
					.equals(SymbolDictionary.STANDARD_AA))
			{
				readCDNA(refSeq, vSeq);
			}
		}
		
		String seq = vSeq.getSequence().replaceAll("\\W", "-");
		String name = vSeq.getName().replaceAll("\\s+", "_");
		Sequence tSeq = new Sequence();
		tSeq.setName(name);
		tSeq.setSequence(seq);
		return tSeq;
	}
	
	private void writeAlignment() {
		Alignment vAlign = (Alignment) map.getVamsasObject(currentTAlignment);
		if(vAlign==null) {
			vAlign = new Alignment();
			currentVAlignment = vAlign;
			vAlign.setGapChar("-");
			vAlign.setProvenance(getProvenance(null));
			Property title = new Property();
			title.setName("title");
			title.setType("string");
			title.setContent(currentTAlignment.name);
			vAlign.addProperty(title);
			
			for(Sequence seq : currentTAlignment.getSequenceSet().getSequences()) {
				writeSequence(seq);
			}
			for(AnalysisResult res : currentTAlignment.getResults()) {
				writeResult(res);
			}
	
			getDataset().addAlignment(vAlign);
			map.registerObjects(currentTAlignment, vAlign);		
		}
	}

	private void writeSequence(Sequence seq) {
		uk.ac.vamsas.objects.core.Sequence vdsSeq = new uk.ac.vamsas.objects.core.Sequence();
		vdsSeq.setName(seq.getName());
		vdsSeq.setSequence(seq.getSequence().replaceAll("\\W", ""));
		vdsSeq.setStart(1);
		vdsSeq.setEnd(vdsSeq.getSequence().length());
		if (currentTAlignment.getSequenceSet().getParams().isDNA())
			vdsSeq.setDictionary(SymbolDictionary.STANDARD_NA);
		else
			vdsSeq.setDictionary(SymbolDictionary.STANDARD_AA);
		getDataset().addSequence(vdsSeq);
		map.registerObjects(seq, vdsSeq);
		
		AlignmentSequence valSeq = new AlignmentSequence();
		valSeq.setName(seq.getName());
		valSeq.setSequence(seq.getSequence());
		valSeq.setStart(1);
		valSeq.setEnd(valSeq.getSequence().length());
		valSeq.setRefid(vdsSeq);
		map.registerObjects(seq, valSeq);
		currentVAlignment.addAlignmentSequence(valSeq);
	}
	
	private void writeResult(AnalysisResult res) {
		
		Object tmp = map.getVamsasObject(res);
		if(tmp == null) {
			AlignmentAnnotation vAnno = null;
			Tree vTree = null;
			
			if(res instanceof DSSResult) {
				DSSResult dss = (DSSResult)res;
				vAnno = getAnnotation();
				vAnno.setType("DSSResult");
				vAnno.setDescription(dss.guiName);
				vAnno.setGraph(true);
				Property p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, dss.data);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
			}
			else if(res instanceof HMMResult) {
				HMMResult hmm = (HMMResult)res;
				vAnno = getAnnotation();
				vAnno.setType("HMMResult");
				vAnno.setDescription(hmm.guiName+" (Topology 1)");
				vAnno.setGraph(true);
				Property p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, hmm.data1);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
				
				vAnno = getAnnotation();
				vAnno.setType("HMMResult");
				vAnno.setDescription(hmm.guiName+" (Topology 2)");
				vAnno.setGraph(true);
				p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, hmm.data2);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
				
				vAnno = getAnnotation();
				vAnno.setType("HMMResult");
				vAnno.setDescription(hmm.guiName+" (Topology 3)");
				vAnno.setGraph(true);
				p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, hmm.data3);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
			}
			else if(res instanceof LRTResult) {
				LRTResult lrt = (LRTResult)res;
				vAnno = getAnnotation();
				vAnno.setType("LRTResult");
				vAnno.setDescription(lrt.guiName);
				vAnno.setGraph(true);
				Property p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, lrt.data);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
			}
			else if(res instanceof PDMResult) {
				PDMResult pdm = (PDMResult)res;
				vAnno = getAnnotation();
				vAnno.setType("PDMResult");
				vAnno.setDescription(pdm.guiName+" (Global)");
				vAnno.setGraph(true);
				Property p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, pdm.glbData);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
				
				vAnno = getAnnotation();
				vAnno.setType("PDMResult");
				vAnno.setDescription(pdm.guiName+" (Local)");
				vAnno.setGraph(true);
				p = new Property();
				p.setName("continuous");
				p.setType("boolean");
				vAnno.addProperty(p);
				addGraph(vAnno, pdm.locData);
				currentVAlignment.addAlignmentAnnotation(vAnno);
				map.registerObjects(res, vAnno);
			}
			
			else if(res instanceof CodeMLResult) {
				CodeMLResult cml = (CodeMLResult)res;
				for(CMLModel model : cml.models) {
					if(map.getVamsasObject(model)!=null)
						continue;
					
					vAnno = getAnnotation();
					vAnno.setType("CodeMLResult");
					vAnno.setDescription("SiteModels");
					vAnno.setLabel(cml.guiName+":"+model.model);
					vAnno.setGraph(true);
					float[][] graph = model.getGraph();
					if(graph!=null) {
						addGraph(vAnno, graph);
						currentVAlignment.addAlignmentAnnotation(vAnno);
						map.registerObjects(res, vAnno);
					}
				}
			}
			
			else if(res instanceof TreeResult) {
				TreeResult tree = (TreeResult) res;
				vTree = new Tree();
				vTree.setTitle(tree.getTitle());
				try
				{
					Newick nw = new Newick();
					String treeString = tree.getTreeStrActual(currentTAlignment.getSequenceSet());			
					nw.setContent(treeString);
					vTree.addNewick(nw);
				} catch (Exception e)
				{
					log.warn("Problem writing newick tree to VAMSAS document.",
							e);
				}
				
				Property p1 = new Property();
				p1.setType("int");
				p1.setName("start");
				p1.setContent("" + tree.getPartitionStart());
				vTree.addProperty(p1);

				Property p2 = new Property();
				p2.setType("int");
				p2.setName("end");
				p2.setContent("" + tree.getPartitionEnd());
				vTree.addProperty(p2);

				vTree.setTitle(tree.guiName);

				// vTree.setProvenance(getDummyProvenance());
				vTree.setProvenance(getProvenance(null));
				currentVAlignment.addTree(vTree);
				VamsasManager.mapper.registerObjects(res, vTree);
			}
		}
	}
	
	private AlignmentAnnotation getAnnotation() {
		AlignmentAnnotation vAnno = new AlignmentAnnotation();
		int start = 1;
		int end = currentTAlignment.getSequenceSet().getLength();
		Seg seg = new Seg();
		seg.setStart(start);
		seg.setEnd(end);
		seg.setInclusive(true);
		vAnno.setSeg(new Seg[]
		{ seg });
		vAnno.setProvenance(getProvenance(null));
		return vAnno;
	
	}
	
	private void readCDNA(uk.ac.vamsas.objects.core.Sequence dsProtSeq, AlignmentSequence protSeq)
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
							String seqString = dnaSeq.getSequence().replaceAll("\\W", "-"); 
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
						String seqString = dna.getSequence().replaceAll("\\W", "-"); 
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
	
	private void addGraph(AlignmentAnnotation anno, float[][] data)
	{
		for (int i = 0; i < data.length; i++)
		{
			int pos = (int) (data[i][0]);
			float[] values = new float[data[i].length - 1];
			for (int j = 1; j < data[i].length; j++)
				values[j - 1] = data[i][j];

			AnnotationElement el = new AnnotationElement();
			el.setPosition(pos);
			el.setValue(values);
			anno.addAnnotationElement(el);
		}
	}
	
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
	
	private DataSet getDataset() {
		for(VAMSAS vamsas : doc.getVamsasRoots()) {
			for(DataSet ds : vamsas.getDataSet()) {
				return ds;
			}
		}
		
		DataSet ds = new DataSet();
		ds.setProvenance(getProvenance(null));
		doc.getVamsasRoots()[0].addDataSet(ds);
		return ds;
	}
	
	private Provenance getProvenance(String action)
	{
		Provenance p = new Provenance();
		p.addEntry(getEntry(action));

		return p;
	}
	
	private Entry getEntry(String action)
	{
		Entry e = new Entry();

		e.setApp(VamsasManager.client.getClientUrn());
		e.setUser(VamsasManager.user.getFullName());
		e.setDate(new Date());

		if (action != null)
			e.setAction(action);
		else
			e.setAction("created");

		return e;
	}
	
}
