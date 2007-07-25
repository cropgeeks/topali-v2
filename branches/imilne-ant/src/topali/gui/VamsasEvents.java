package topali.gui;

import topali.data.*;
import topali.vamsas.*;
import uk.ac.vamsas.client.picking.MouseOverMessage;

public class VamsasEvents
{
	private WinMain winMain;
	
	public VamsasEvents(WinMain winMain)
	{
		this.winMain = winMain;
	}
	
	void sendAlignmentPanelMouseOverEvent(Sequence seq, int pos)
	{
		String id = VamsasManager.mapper.getVorbaID(seq);
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