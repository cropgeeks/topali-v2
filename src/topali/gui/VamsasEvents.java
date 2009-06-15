package topali.gui;

import topali.data.*;
import topali.vamsas.*;
import uk.ac.vamsas.client.picking.*;

public class VamsasEvents
{
	private WinMain winMain;
	private ObjectMapper mapper;

	public VamsasEvents(WinMain winMain, VamsasManager man)
	{
		this.winMain = winMain;
		this.mapper = man.mapper;
	}

	void sendAlignmentPanelMouseOverEvent(Sequence seq, int pos)
	{
		if(mapper==null)
			return;

		String id = mapper.getVorbaID(seq);
		if(id!=null) {
			MouseOverMessage message = new MouseOverMessage(id, pos-1);
			winMain.vamsas.msgHandler.sendMessage(message);
		}
	}

	public void sendSelectedSequencesEvent(SequenceSet ss)
	{
		if (mapper==null)
			return;

		try
		{
			// Get an array of all the VorbaIDs for the selected sequences
			int[] selected = ss.getSelectedSequences();
			String[] ids = new String[selected.length];

			for (int i = 0; i < ids.length; i++)
				ids[i] = mapper.getVorbaID(ss.getSequence(selected[i]));

			// Now create the message to send
			SelectionMessage message;
			if (ids.length == 0)
				message = new SelectionMessage(null, null, null);
			else
				message = new SelectionMessage(null, ids, null);

			winMain.vamsas.msgHandler.sendMessage(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void processAlignmentPanelMouseOverEvent(Sequence sequence, int position)
	{
		for (AlignmentData data : winMain.getProject().getDatasets())
		{
			int i = 0;
			for (Sequence seq : data.getSequenceSet().getSequences())
			{
				if (seq.equals(sequence))
				{
					AlignmentPanel panel = WinMain.navPanel.getCurrentAlignmentPanel(data);

					panel.highlight(i, position, false);

					break;
				}

				i++;
			}
		}
	}

	public void processSelectionEvent(Sequence[] seqs)
	{
		SequenceSet ss = null;
		int[] indices = new int[seqs.length];

		for (AlignmentData data : winMain.getProject().getDatasets())
		{
			ss = data.getSequenceSet();

			int seqIndex = 0;
			int selIndex = 0;

			// Search for the sequences in this dataset
			for (Sequence seq : data.getSequenceSet().getSequences())
			{
				for (Sequence toSel: seqs)
				{
					if (seq.equals(toSel))
					{
						indices[selIndex] = seqIndex;
						selIndex++;
					}
				}

				seqIndex++;
			}
		}

		// Once we have their indices, set their selection state
		ss.setSelectedSequences(indices);
		TOPALi.winMain.menuViewDisplaySettings(true);
	}
}