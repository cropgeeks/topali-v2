// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.exolab.castor.xml.*;

import topali.cluster.jobs.mrbayes.MrBayesAnalysis;
import topali.data.*;
import topali.data.Sequence;
import topali.fileio.Castor;
import topali.gui.*;
import uk.ac.vamsas.client.*;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.core.AnnotationElement;
import uk.ac.vamsas.objects.utils.trees.*;

public class VAMSASUtils
{
	final static boolean debug = false;
	
	static   Logger log = Logger.getLogger(VAMSASUtils.class);
	
	public static boolean storeProject(Project project, IClientDocument cDoc) {	
		
		IClientAppdata data = cDoc.getClientAppdata();
		if(data==null) {
			log.warn("Could not get a IClientAppdata.");
			return false;
		}
		
		StringWriter out = new StringWriter();
		
		try
		{
			Marshaller m = new Marshaller(out);
			m.setMapping(Castor.getMapping());
			m.setEncoding("UTF-8");
			m.marshal(project);
			AppDataOutputStream os = data.getClientOutputStream();
			os.write(out.toString().getBytes());
			log.info("Wrote topali project to vamsas appdata.");
		} catch (Exception e)
		{
			log.warn("Marshalling failed.", e);
			return false;
		} 
		
		return true;
	}
	
	public static Project loadProject(IClientDocument cDoc) {
		IClientAppdata data = cDoc.getClientAppdata();
		if(data==null) {
			log.warn("Could not get a IClientAppdata.");
			return null;
		}

		if(!data.hasClientAppdata()) {
			log.info("No stored project found in VAMSAS document.");
			return null;
		}
		
		AppDataInputStream is = data.getClientInputStream();
		Vector<Byte> tmp = new Vector<Byte>();
		while(true) {
			try
			{
				tmp.add(is.readByte());
			} catch (IOException e)
			{
				break;
			}
		}
		
		byte[] bytes = new byte[tmp.size()];
		for(int i=0; i<tmp.size(); i++)
			bytes[i] = tmp.get(i).byteValue();
		
		if(VAMSASUtils.debug) {
			String debug = new String(bytes);
			System.out.println("\n"+debug.replaceAll(">", ">\n"));
		}
		
		InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(bytes));
		Unmarshaller u = new Unmarshaller();
		try
		{
			u.setMapping(Castor.getMapping());
			Project vProject = (Project)u.unmarshal(in);
			log.info("Got topali project from appdata.");
			return vProject;
		} catch (Exception e)
		{
			log.warn("Unmarshalling failed.", e);
			return null;
		}
	}
	
	public static Tree createVamsasTree(TreeResult res, AlignmentData align, ObjectMapper map) {
		try
		{
			TreeResult tree = (TreeResult) res;
			Tree vTree = new Tree();
			vTree.setTitle(tree.getTitle());
			String safeTreeString = tree.getTreeStr();
			NewickFile nwfile = new NewickFile(safeTreeString);
			nwfile.parse();
			Vector<Object> leaves = new Vector<Object>();
			nwfile.findLeaves(nwfile.getTree(), leaves);
			for(Object o : leaves) {
				SequenceNode node = (SequenceNode)o;
				for(Sequence seq : align.getSequenceSet().getSequences()) {
					if(node.getName().equals(seq.safeName)) {
						AlignmentSequence valSeq = (AlignmentSequence)map.getVamsasObject(seq);
						if(valSeq!=null) {
							node.setElement(valSeq);
							node.setName(valSeq.getName());
						}
					}
				}
			}
			
			Treenode[] treenodes = nwfile.makeTreeNodes();
			
			Newick nw = new Newick();
			nw.setContent(nwfile.print());
			vTree.addNewick(nw);
			vTree.setTreenode(treenodes);
			
			Provenance prov = new Provenance();
			Entry ent = new Entry();
			Input inp = new Input();
			Seg seg = new Seg();
			seg.setStart(tree.getPartitionStart());
			seg.setEnd(tree.getPartitionEnd());
			seg.setInclusive(true);
			inp.addSeg(seg);
			inp.setName("partition");
			ent.addInput(inp);
			
			if(tree instanceof PhymlResult) {
				PhymlResult phyml = (PhymlResult)tree;
				String[] params = phyml.phymlParameters;
				
				Param par = new Param();
				par.setType("string");
				par.setName("application");
				par.setContent("PhyML");
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("method");
				par.setContent("maximum likelihood");
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("sequenceType");
				par.setContent(params[2]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("fileFormat");
				par.setContent(params[3]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("NoOfDatasets");
				par.setContent(params[4]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("bootstraps");
				par.setContent(params[5]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("model");
				par.setContent(params[6]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("tstv");
				par.setContent(params[7]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("invariant");
				par.setContent(params[8]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("categories");
				par.setContent(params[9]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("gamma");
				par.setContent(params[10]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("startTree");
				par.setContent(params[11]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("optimiseTopology");
				par.setContent(params[12]);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("optimiseBranchLengths");
				par.setContent(params[13]);
				ent.addParam(par);
			}
			else if(tree instanceof MBTreeResult) {
				MBTreeResult mb = (MBTreeResult)tree;
				Param par = new Param();
				par.setType("string");
				par.setName("application");
				par.setContent("MrBayes");
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("version");
				par.setContent(MrBayesAnalysis.VERSION);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("method");
				par.setContent("bayes");
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("nexusCommands");
				par.setContent(mb.nexusCommands);
				ent.addParam(par);
			}
			
			else {
				Param par = new Param();
				par.setType("string");
				par.setName("application");
				par.setContent("TOPALi");
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("version");
				par.setContent(TOPALi.VERSION);
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("method");
				par.setContent("neighbour joining");
				ent.addParam(par);
				
				par = new Param();
				par.setType("string");
				par.setName("model");
				par.setContent("F84");
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("tstv");
				par.setContent("2");
				ent.addParam(par);
				
				par = new Param();
				par.setType("int");
				par.setName("categories");
				par.setContent("4");
				ent.addParam(par);
			}
			
			ent.setApp(VamsasManager.client.getClientUrn());
			ent.setUser(VamsasManager.user.getFullName());
			ent.setAction("created");
			ent.setDate(new Date());
			prov.addEntry(ent);
			vTree.setProvenance(prov);
			
			return vTree;
		} catch (Exception e)
		{
			log.warn("Problem creating VAMSAS tree.",
					e);
			return null;
		}
	}

	public static LinkedList<AlignmentAnnotation> createAlignmentAnnotation(AnalysisResult res, AlignmentData align) {
		
		LinkedList<AlignmentAnnotation> annotations = new LinkedList<AlignmentAnnotation>();
		
		AlignmentAnnotation vAnno;
		
		if (res instanceof DSSResult)
		{
			DSSResult dss = (DSSResult) res;
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("DSSResult");
			vAnno.setDescription(dss.guiName);
			vAnno.setGraph(true);
			Property p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, dss.data);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
		} else if (res instanceof HMMResult)
		{
			HMMResult hmm = (HMMResult) res;
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("HMMResult");
			vAnno.setDescription(hmm.guiName + " (Topology 1)");
			vAnno.setGraph(true);
			Property p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, hmm.data1);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
			
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("HMMResult");
			vAnno.setDescription(hmm.guiName + " (Topology 2)");
			vAnno.setGraph(true);
			p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, hmm.data2);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
			
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("HMMResult");
			vAnno.setDescription(hmm.guiName + " (Topology 3)");
			vAnno.setGraph(true);
			p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, hmm.data3);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
		} else if (res instanceof LRTResult)
		{
			LRTResult lrt = (LRTResult) res;
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("LRTResult");
			vAnno.setDescription(lrt.guiName);
			vAnno.setGraph(true);
			Property p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, lrt.data);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
		} else if (res instanceof PDMResult)
		{
			PDMResult pdm = (PDMResult) res;
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("PDMResult");
			vAnno.setDescription(pdm.guiName + " (Global)");
			vAnno.setGraph(true);
			Property p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, pdm.glbData);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
			
			vAnno = getRawAlignmentAnnotation(align);
			vAnno.setType("PDMResult");
			vAnno.setDescription(pdm.guiName + " (Local)");
			vAnno.setGraph(true);
			p = new Property();
			p.setName("continuous");
			p.setType("boolean");
			vAnno.addProperty(p);
			addGraph(vAnno, pdm.locData);
			vAnno.addProperty(createTIDProp(res));
			annotations.add(vAnno);
		}

		else if (res instanceof CodeMLResult)
		{
			CodeMLResult cml = (CodeMLResult) res;
			for (CMLModel model : cml.models)
			{
				vAnno = getRawAlignmentAnnotation(align);
				vAnno.setType("CodeMLResult");
				vAnno.setDescription("SiteModels");
				vAnno.setLabel(cml.guiName + ":" + model.model);
				vAnno.setGraph(true);
				float[][] graph = model.getGraph();
				if (graph != null)
				{
					addGraph(vAnno, graph);
					vAnno.addProperty(createTIDProp(res));
					annotations.add(vAnno);
				}
			}
		}
		
		return annotations;
	}
		
	private static AlignmentAnnotation getRawAlignmentAnnotation(AlignmentData align) {
		AlignmentAnnotation vAnno = new AlignmentAnnotation();
		int start = 1;
		int end = align.getSequenceSet().getLength();
		Seg seg = new Seg();
		seg.setStart(start);
		seg.setEnd(end);
		seg.setInclusive(true);
		vAnno.setSeg(new Seg[]
		{ seg });
		Provenance prov = new Provenance();
		Entry ent = new Entry();
		ent.setApp(VamsasManager.client.getClientUrn());
		ent.setUser(VamsasManager.user.getFullName());
		ent.setDate(new Date());
		ent.setAction("created");
		prov.addEntry(ent);
		vAnno.setProvenance(prov);
		return vAnno;
	}
	
	private static void addGraph(AlignmentAnnotation anno, float[][] data)
	{
		for (float[] element : data)
		{
			int pos = (int) (element[0]);
			float[] values = new float[element.length - 1];
			for (int j = 1; j < element.length; j++)
				values[j - 1] = element[j];

			AnnotationElement el = new AnnotationElement();
			el.setPosition(pos);
			el.setValue(values);
			anno.addAnnotationElement(el);
		}
	}
	
	private static Property createTIDProp(DataObject tObj) {
		Property tid = new Property();
		tid.setName("topaliID");
		tid.setType("int");
		tid.setContent(""+tObj.getID());
		return tid;
	}
}
