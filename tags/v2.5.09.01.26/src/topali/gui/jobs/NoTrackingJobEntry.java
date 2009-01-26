// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.jobs;

import java.awt.*;

import javax.swing.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.gui.JobsPanelEntry;

public class NoTrackingJobEntry extends JobsPanelEntry
{
	JProgressBar pb;

	public NoTrackingJobEntry(AnalysisJob job)
	{
		super(job);
	}

	@Override
	public void setJobStatus(JobStatus status)
	{
		super.setJobStatus(status);
	}

	@Override
	public JComponent getProgressComponent()
	{
		pb = new JProgressBar();
		pb.setStringPainted(true);
		pb.setValue(0);
		pb.setPreferredSize(new Dimension(50, 20));
		pb.setMaximum(100);
		pb.setBorderPainted(false);
		pb.setForeground(new Color(140, 165, 214));
		pb.setString("No progress tracking available");
		return pb;
	}
}
