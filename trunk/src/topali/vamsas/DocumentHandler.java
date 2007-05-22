package topali.vamsas;

import java.util.Date;
import java.util.LinkedList;

import topali.data.*;
import topali.data.Sequence;
import topali.gui.Project;
import uk.ac.vamsas.client.IClientDocument;
import uk.ac.vamsas.objects.core.*;
import uk.ac.vamsas.objects.utils.SymbolDictionary;

class DocumentHandler
{
	private Project project;

	private ObjectMapper mapper;

	private DataSet dataset;

	//TODO: General problem: Mapping between one topali object and several vamsas objects
	// e.g. tSequence -> vSequence and vDataSetSequence
	//      tAnalysisResult -> several vAnnotations
	
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
				tAlign.setActiveRegion(0, tAlign.getSequenceSet().getLength());
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

	private void writeAnalysisResults(AlignmentData tAlign, Alignment vAlign) {
		for(AnalysisResult tRes : tAlign.getResults()) {
			AlignmentAnnotation vAnno = (AlignmentAnnotation)mapper.getVamsasObject(tRes);
			
			if(vAnno==null) {
				if(tRes instanceof PDMResult) {
					
				}
				else if(tRes instanceof HMMResult) {
					
				}
				else if(tRes instanceof DSSResult) {
					
				}
				else if(tRes instanceof LRTResult) {
					
				}
				else if(tRes instanceof CodeMLResult) {
					
				}
				else if(tRes instanceof TreeResult) {
					
				}
				mapper.registerObjects(tRes, vAnno);
			}
			
		}
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
		e.setDate(new org.exolab.castor.types.Date(new Date()));

		if (action != null)
			e.setAction(action);
		else
			e.setAction("created.");

		return e;
	}
}