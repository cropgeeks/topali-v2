package topali.gui.jobs;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import topali.cluster.jobs.AnalysisJob;
import topali.gui.JobsPanel;
import topali.gui.JobsPanelEntry;

public class NoTrackingJobEntry extends JobsPanelEntry {
	
	public NoTrackingJobEntry(AnalysisJob job) {
		super(job);
	}

	@Override
	public JComponent getProgressComponent() {
		
		JPanel p = new JPanel();
		p.setBackground(Color.WHITE);
		p.setAlignmentX(CENTER_ALIGNMENT);
		p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		p.add(new JLabel("No progress tracking available."));
		return p;
	}

	@Override
	public void setProgress(float progress, String text) {
	}

	
}
