// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.jobs;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.gui.JobsPanel;
import topali.gui.JobsPanelEntry;

public class ProgressBarJobEntry extends JobsPanelEntry {

	JProgressBar pb;
	
	public ProgressBarJobEntry(AnalysisJob job) {
		super(job);
	}
	
	@Override
	public JComponent getProgressComponent() {
		pb = new JProgressBar();
		pb.setStringPainted(true);
		pb.setValue(0);
		pb.setPreferredSize(new Dimension(50, 20));
		pb.setMaximum(100);
		pb.setBorderPainted(false);
		pb.setForeground(new Color(140, 165, 214));
		return pb; 
	}

	@Override
	public void setJobStatus(JobStatus status) {
		pb.setValue((int)status.progress);
	}

}
