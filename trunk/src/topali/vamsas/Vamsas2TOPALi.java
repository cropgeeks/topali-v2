package topali.vamsas;

import topali.fileio.*;

import org.vamsas.objects.core.*;

class Vamsas2TOPALi
{
	Vamsas2TOPALi()
	{
	}
	
	topali.data.AlignmentData[] createTOPALi(VAMSAS vVamsas)
		throws AlignmentLoadException
	{
		// TODO: Deal with more than one DataSet per VAMSAS file?
		DataSet vDataSet = vVamsas.getDataSet(0);
		
		// Create a TOPALi dataset for each VAMSAS data set found
		topali.data.AlignmentData[] tAlignmentData =
			new topali.data.AlignmentData[vDataSet.getAlignmentCount()];
		
		for (int i = 0; i < tAlignmentData.length; i++)
		{
			Alignment vAlignment = vDataSet.getAlignment(i);			
			tAlignmentData[i] = createAlignmentData(vAlignment);
			tAlignmentData[i].name = "vamsas " + (i+1);
		}
		
		System.out.println("Returning " + tAlignmentData.length + " alignment datasets");
		return tAlignmentData;
	}
	
	topali.data.AlignmentData createAlignmentData(Alignment vAlignment)
		throws AlignmentLoadException
	{
		topali.data.AlignmentData tAlignmentData = new topali.data.AlignmentData();
		
		// Retrieve the sequences and create a SequenceSet object from them
		AlignmentSequence[] vAlignmentSequences = vAlignment.getAlignmentSequence();
		tAlignmentData.setSequenceSet(createSequenceSet(vAlignmentSequences));
		
//		tAlignmentData.getPartitionInfo().createDefaultPartition(tAlignmentData);
				
		return tAlignmentData;
	}
	
	topali.data.SequenceSet createSequenceSet(AlignmentSequence[] vAlignmentSequences)
		throws AlignmentLoadException
	{
		topali.data.SequenceSet tSequenceSet = new topali.data.SequenceSet();
		
		for (int i = 0; i < vAlignmentSequences.length; i++)
			tSequenceSet.addSequence(createSequence(vAlignmentSequences[i]));
		
		tSequenceSet.checkValidity();
		System.out.println("ss szie is " + tSequenceSet.getSize() + " " + tSequenceSet.getLength());
		return tSequenceSet;
	}
	
	topali.data.Sequence createSequence(AlignmentSequence vAlignmentSequence)
	{
		topali.data.Sequence tSequence = new topali.data.Sequence();
		
		tSequence.name = vAlignmentSequence.getName();
		tSequence.setSequence(vAlignmentSequence.getSequence());
		
		return tSequence;
	}
}
