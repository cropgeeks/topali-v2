package topali.gui.jobs;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.*;
import topali.gui.*;

public class CodeMLJobEntry extends JobsPanelEntry
{
	private JLabel progressLabel;
	
	public CodeMLJobEntry(AnalysisJob job)
	{
		super(job);
	}
	
	@Override
	public JComponent getProgressComponent()
	{
		progressLabel = new JLabel("Completed models: ", JLabel.LEFT);
		
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(bgColor);
		p.setBorder(BorderFactory.createTitledBorder(""));
		p.add(progressLabel);
		
		return p;
	}

	@Override
	public void setJobStatus(JobStatus status)
	{
		if (status.text == null)
			return;
		
		System.out.println(status.text);
		
		String str = "<html>Completed models:&nbsp;&nbsp; ";
		
		StringTokenizer tok = new StringTokenizer(status.text, " ");
		while(tok.hasMoreElements())
		{
			String[] tmp = tok.nextToken().split("=");
			boolean b = Boolean.parseBoolean(tmp[1]);
			
			if (b)
				str += "[" + tmp[0] + "]";
			else
				str += "<font color='#aaaaaa'>[" + tmp[0] + "]</font>";
			
			str += "&nbsp;&nbsp;&nbsp;";
		}
		
		str += "</html>";
		
		progressLabel.setText(str);
		
		repaint();
	}
}
