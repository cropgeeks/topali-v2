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
import uk.ac.vamsas.objects.core.Map;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

public class VamsasDocumentHandler
{
	 Logger log = Logger.getLogger(this.getClass());

	private Project project;

	private IClientDocument doc;

	ObjectMapper map;

	// These fields are dynamically set during reading/writing process (be
	// carefull!)
	Alignment currentVAlignment = null;

	AlignmentData currentTAlignment = null;

	// tmp stuff for cdnas
	LinkedList<AlignmentData> cdnaDatasets = new LinkedList<AlignmentData>();

	SequenceSet cdnaSS = new SequenceSet();

	public VamsasDocumentHandler(Project proj, IClientDocument doc)
	{
		this.project = proj;
		this.doc = doc;
		
		if(proj.getVamsasMapper()==null)
			proj.setVamsasMapper(new ObjectMapper());
		
		proj.getVamsasMapper().registerClientDocument(doc);
		
		this.map = proj.getVamsasMapper();
	}

	public void read()
	{
		for (VAMSAS vamsas : doc.getVamsasRoots())
			for (DataSet dataset : vamsas.getDataSet())
				for (Alignment alignment : dataset.getAlignment())
					readAlignment(alignment);

		for (AlignmentData cdnas : cdnaDatasets)
			if (project.containsDatasetBySeqs(cdnas)==null)
			{
				int option = JOptionPane
						.showConfirmDialog(
								null,
								"Found DNA with a corresponding protein alignment. Create a protein guided DNA alignment?",
								"Guided alignment", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);
				if (option == JOptionPane.YES_OPTION)
					project.addDataSet(cdnas);
			}
		cdnaDatasets.clear();
	}

	public void write()
	{
		for (AlignmentData align : project.getDatasets())
		{
			currentTAlignment = align;
			writeAlignment();
		}
	}

	private void readAlignment(Alignment vAlign)
	{
		AlignmentData tAlign = (AlignmentData) map.getTopaliObject(vAlign
				.getId());
		if (tAlign == null)
		{
			log.info("Creating new topali alignment for "+vAlign);
			
			tAlign = new AlignmentData();
			String alignName = "VAMSAS alignment";
			Property[] props = vAlign.getProperty();
			for (Property prop : props)
				if (prop.getName().endsWith("itle"))
				{
					alignName = prop.getContent();
					break;
				}
			tAlign.name = alignName;

			SequenceSet ss = new SequenceSet();
			ss.setOverview("");
			for (AlignmentSequence vSeq : vAlign.getAlignmentSequence())
			{
				Sequence tSeq = readSequence(vSeq);
				ss.addSequence(tSeq);
				map.registerObjects(tSeq, vSeq);
			}
			try
			{
				ss.checkValidity();
			} catch (AlignmentLoadException e)
			{
				log.warn("Alignment '" + tAlign.name + "' is not aligned!", e);
				return;
			}
			tAlign.setSequenceSet(ss);

			project.addDataSet(tAlign);
			map.registerObjects(tAlign, vAlign);

			if (cdnaSS.getSize() > 0)
			{
				String name = getAlignmentName(vAlign);
				MakeNA mna = new MakeNA(cdnaSS, ss, name + " (cDNA)");
				log.info("Generating protein guided alignment for " + name);
				if (mna.doConversion(false, map.linkedObjects))
				{
					AlignmentData cdnaData = mna.getAlignmentData();
					for (Sequence s : cdnaData.getSequenceSet().getSequences())
						s.setName(s.getName() + "_cDNA");
					cdnaDatasets.add(cdnaData);
				} else
					log.warn("Could not create guided alignment!");
				cdnaSS = new SequenceSet();
			}
		} else {
			log.info("Topali alignment ("+tAlign+")already exists, updating alignment.");
			for (AlignmentSequence vSeq : vAlign.getAlignmentSequence())
			{
				Sequence tSeq = (Sequence) map.getTopaliObject(vSeq);
				if (tSeq == null)
				{
					log.info("Adding new topali sequence to existing topali alignment ("+vSeq+")");
					String seq = vSeq.getSequence().replaceAll("\\W", "-");
					String name = vSeq.getName().replaceAll("\\s+", "_");
					tSeq = new Sequence();
					tSeq.setName(name);
					tSeq.setSequence(seq);
					tAlign.getSequenceSet().addSequence(tSeq);
					map.registerObjects(tSeq, vSeq);
				} else
				{
					log.info("Updating topali sequence "+tSeq);
					String seq = vSeq.getSequence().replaceAll("\\W", "-");
					String name = vSeq.getName().replaceAll("\\s+", "_");
					tSeq.setName(name);
					tSeq.setSequence(seq);
				}
			}
		}
	}

	private Sequence readSequence(AlignmentSequence vSeq)
	{
		log.info("Creating new topali sequence for "+vSeq);
		Object tmp = vSeq.getRefid();
		uk.ac.vamsas.objects.core.Sequence refSeq = null;
		if (tmp != null)
		{
			refSeq = (uk.ac.vamsas.objects.core.Sequence) tmp;
			// check the referenced dataset sequence, if we deal with a
			// protein sequence.
			if (refSeq.getDictionary().equals(SymbolDictionary.STANDARD_AA))
				// if so, check if we have a corresponding cdna for it
				readCDNA(refSeq, vSeq);
		}

		String seq = vSeq.getSequence().replaceAll("\\W", "-");
		String name = vSeq.getName().replaceAll("\\s+", "_");
		Sequence tSeq = new Sequence();
		tSeq.setName(name);
		tSeq.setSequence(seq);

		// if the sequence is associated with a DatasetSequence, take a note of
		// that
		if (refSeq != null)
			map.linkedObjects.put(tSeq, refSeq);

		return tSeq;
	}
	
	private void writeAlignment()
	{
		//Check if this alignment already exists in the vamsas doc
		System.out.println(currentTAlignment.hashCode());
		currentVAlignment = (Alignment) map.getVamsasObject(currentTAlignment);
		
		// create new Alignment
		if (currentVAlignment == null)
		{
			currentVAlignment = new Alignment();
			currentVAlignment.setGapChar("-");
			currentVAlignment.setProvenance(getProvenance(null));
			Property title = new Property();
			title.setName("title");
			title.setType("string");
			title.setContent(currentTAlignment.name);
			currentVAlignment.addProperty(title);
			//currentVAlignment.addProperty(createIDProperty(currentTAlignment));
			for (Sequence seq : currentTAlignment.getSequenceSet()
					.getSequences())
				writeSequence(seq);

			for (AnalysisResult res : currentTAlignment.getResults())
				writeResult(res);

			getDataset().addAlignment(currentVAlignment);
			map.registerObjects(currentTAlignment, currentVAlignment);
		}

		// Alignment already exists
		else
		{	
			//just modifiy sequences if alignment is not locked
			if(currentVAlignment.getModifiable()==null)
			{
				for (Sequence seq : currentTAlignment.getSequenceSet().getSequences())
					writeSequence(seq);
			}

			for (AnalysisResult res : currentTAlignment.getResults())
				writeResult(res);
			
			map.registerObjects(currentTAlignment, currentVAlignment);
		}
	}

	private void writeSequence(Sequence seq)
	{

		AlignmentSequence valSeq = (AlignmentSequence) map.getVamsasObject(seq);

		if (valSeq == null)
		{
			log.info("Creating new vamsas sequence for "+seq);
			// If neccessary create a new DatasetSequence
			uk.ac.vamsas.objects.core.Sequence vdsSeq = null;
			for(Object tmp : map.linkedObjects.get(seq)) {
				if(tmp instanceof uk.ac.vamsas.objects.core.Sequence) {
					vdsSeq = (uk.ac.vamsas.objects.core.Sequence)tmp;
					break;
				}
			}
			if (vdsSeq == null)
			{
				log.info("No DatasetSequence found, creating a new one.");
				vdsSeq = new uk.ac.vamsas.objects.core.Sequence();
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
				map.linkedObjects.put(seq, vdsSeq);
			} else
			{
				log.info("DatasetSequence for this AlignmentSequence exists.");
			}

			// Create the alignment sequence
			valSeq = new AlignmentSequence();
			valSeq.setName(seq.getName());
			valSeq.setSequence(seq.getSequence());
			valSeq.setStart(1);
			valSeq.setEnd(valSeq.getSequence().length());
			valSeq.setRefid(vdsSeq);
			map.registerObjects(seq, valSeq);
			currentVAlignment.addAlignmentSequence(valSeq);

			// Check if there is a linked sequence
			if (map.linkedObjects.contains(seq))
			{
				LinkedList<Object> linked = map.linkedObjects.getAll(seq);
				Sequence sequence = null;
				//one mapping for seq. -> dna seq. and one for seq. -> protein seq.
				SequenceMapping map1 = null;
				SequenceMapping map2 = null;

				for (Object o : linked)
					if (o instanceof Sequence)
						sequence = (Sequence) o;
					else if ((o instanceof SequenceMapping) && (map1 == null))
						map1 = (SequenceMapping) o;
					else if ((o instanceof SequenceMapping) && (map2 == null))
						map2 = (SequenceMapping) o;

				if (sequence != null)
				{
					if (map1 != null)
					{
						map1.setLoc(vdsSeq);
						getDataset().addSequenceMapping(map1);
					}
					if (map2 != null)
					{
						map2.setLoc(vdsSeq);
						getDataset().addSequenceMapping(map2);
					}
					map.linkedObjects.remove(seq);
				}
			}
		} else
		{
			log.info("Vamsas sequence already exists. Updating sequence "+valSeq);
			valSeq.setName(seq.getName());
		}
	}

	private void writeResult(AnalysisResult res)
	{

		Object tmp = map.getVamsasObject(res);
		if (tmp == null)
		{
			log.info("Creating new vamsas annotation for "+res);
			
			if(res instanceof TreeResult) {
				TreeResult tree = (TreeResult) res;
				Tree vTree = VAMSASUtils.createVamsasTree(tree, currentTAlignment, map);
				currentVAlignment.addTree(vTree);
				map.registerObjects(res, vTree);
			}
			
			else {
				LinkedList<AlignmentAnnotation> annos = VAMSASUtils.createAlignmentAnnotation(res, currentTAlignment);
				for(AlignmentAnnotation anno : annos) {
					currentVAlignment.addAlignmentAnnotation(anno);
					map.registerObjects(res, anno);
				}
			}
			
		}
		
		//if there are results based on this alignment, lock it
		if(currentVAlignment.getModifiable()==null)
			currentVAlignment.setModifiable(VamsasManager.client.getClientUrn());
	}

	private void readCDNA(uk.ac.vamsas.objects.core.Sequence dsProtSeq,
			AlignmentSequence protSeq)
	{
		Sequence tSeq = null;

		log.info("Try to get cdna for " + dsProtSeq);

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
						// compare the dbref with dnadbref
						if (compareDBRef(dnadbref, dbref))
						{
							if (dnadbref.getMap().length < 1)
								continue;

							Map map = dnadbref.getMap()[0];

							int start;
							int end;
							String complSeq = dnaSeq.getSequence().replaceAll(
									"\\W", "-");
							StringBuffer concatSeq = new StringBuffer();

							SequenceMapping dnaMapping = new SequenceMapping();
							dnaMapping.setMap(dnaSeq);
							Mapped dnaMap = new Mapped();
							dnaMap.setUnit(3);
							dnaMapping
									.setProvenance(getProvenance("Protein guided cDNA alignment"));

							for (Seg seg : map.getLocal().getSeg())
							{
								start = seg.getStart() - 1;
								end = seg.getEnd() - 3;
								concatSeq
										.append(complSeq.substring(start, end));
								Seg segm = new Seg();
								segm.setInclusive(true);
								segm.setStart(seg.getStart());
								segm.setEnd(seg.getEnd());
								dnaMap.addSeg(segm);
							}
							dnaMapping.setMapped(dnaMap);
							Local dnaLoc = new Local();
							dnaLoc.setUnit(3);
							Seg segm = new Seg();
							segm.setInclusive(true);
							segm.setStart(1);
							segm.setEnd(concatSeq.length());
							dnaLoc.addSeg(segm);
							dnaMapping.setLocal(dnaLoc);

							SequenceMapping protMapping = new SequenceMapping();
							protMapping.setMap(dsProtSeq);
							Local protLoc = new Local();
							protLoc.setUnit(3);
							protMapping
									.setProvenance(getProvenance("Protein guided cDNA alignment"));
							segm = new Seg();
							segm.setInclusive(true);
							segm.setStart(1);
							segm.setEnd(concatSeq.length());
							protLoc.addSeg(segm);
							protMapping.setLocal(protLoc);
							Mapped protMap = new Mapped();
							protMap.setUnit(1);
							segm = new Seg();
							segm.setInclusive(true);
							segm.setStart(1);
							segm.setEnd(dsProtSeq.getSequence().length());
							protMap.addSeg(segm);
							protMapping.setMapped(protMap);

							if (concatSeq.length() > 0)
							{
								tSeq = new Sequence();
								tSeq.setSequence(concatSeq.toString());
								tSeq.setName(protSeq.getName().replaceAll(
										"\\s+", "_"));

								this.map.linkedObjects.put(tSeq, dnaMapping);
								this.map.linkedObjects.put(tSeq, protMapping);
								found = true;
								log.info("Found matching dbrefs: "
										+ dnaSeq.getName());
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
				// or search sequence mapping
				for (SequenceMapping smap : dataset.getSequenceMapping())
				{
					uk.ac.vamsas.objects.core.Sequence map = (uk.ac.vamsas.objects.core.Sequence) smap
							.getMap();
					if (map.equals(dsProtSeq))
					{
						log.info("Found matching sequence mapping: "
								+ dsProtSeq);
						uk.ac.vamsas.objects.core.Sequence dna = (uk.ac.vamsas.objects.core.Sequence) smap
								.getLoc();
						int s1 = (int) dna.getStart();
						int s2 = smap.getLocal().getSeg()[0].getStart();
						// int e1 = (int)dna.getEnd();
						int e2 = smap.getLocal().getSeg()[0].getEnd();
						int start = s2 - s1;
						int end = start + (e2 - s2) + 1;
						String seqString = dna.getSequence().replaceAll("\\W",
								"-");
						String sequence = seqString.substring(start, end);
						tSeq = new Sequence();
						tSeq.setSequence(sequence);
						tSeq.setName(protSeq.getName().replaceAll("\\s+", "_"));
						found = true;
					}

					if (found)
						break;
				}
		}

		// if a cdna was found, add it to the sequenceset
		if (tSeq != null)
			cdnaSS.addSequence(tSeq);
	}

	private boolean compareDBRef(DbRef ref1, DbRef ref2)
	{
		if ((ref1 == null) || (ref2 == null))
			return false;
		else
			return (ref1.getSource().equals(ref2.getSource()) && ref1
					.getAccessionId().equals(ref2.getAccessionId()));
	}

	private String getAlignmentName(Alignment vAlign)
	{
		Property[] props = vAlign.getProperty();
		for (Property prop : props)
			if (prop.getName().endsWith("itle"))
				return prop.getContent();
		return "Vamsas";
	}

	private DataSet getDataset()
	{
		for (VAMSAS vamsas : doc.getVamsasRoots())
			for (DataSet ds : vamsas.getDataSet())
				return ds;

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
