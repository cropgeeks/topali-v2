package topali.gui;

import topali.data.*;
import topali.vamsas.*;
import uk.ac.vamsas.client.picking.MouseOverMessage;

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
}