// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.jobs;

import java.awt.Color;

import javax.swing.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.gui.JobsPanelEntry;

public class NoTrackingJobEntry extends JobsPanelEntry
{

	public NoTrackingJobEntry(AnalysisJob job)
	{
		super(job);
	}

	@Override
	public JComponent getProgressComponent()
	{

		JPanel p = new JPanel();
		p.setBackground(Color.WHITE);
		p.setAlignmentX(CENTER_ALIGNMENT);
		p.setBorder(BorderFactory.createTitledBorder(""));
		p.add(new JLabel("No progress tracking available."));
		return p;
	}

	@Override
	public void setJobStatus(JobStatus status)
	{
		super.setJobStatus(status);
	}

}
