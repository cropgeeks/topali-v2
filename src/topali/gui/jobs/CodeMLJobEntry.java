package topali.gui.jobs;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import topali.cluster.jobs.AnalysisJob;
import topali.gui.JobsPanel;
import topali.gui.JobsPanelEntry;

public class CodeMLJobEntry extends JobsPanelEntry {
	
	JCheckBox m0, m1, m2, m3, m4, m5, m6, m7, m8;
	
	public CodeMLJobEntry(AnalysisJob job) {
		super(job);
	}
	
	@Override
	public JComponent getProgressComponent() {
		m0 = getCheckBox("M0");
		m1 = getCheckBox("M1");
		m2 = getCheckBox("M2");
		m3 = getCheckBox("M3");
		m4 = getCheckBox("M4");
		m5 = getCheckBox("M5");
		m6 = getCheckBox("M6");
		m7 = getCheckBox("M7");
		m8 = getCheckBox("M8");

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setBackground(Color.WHITE);
		
		p.add(Box.createHorizontalGlue());
		p.add(m0);
		p.add(Box.createHorizontalGlue());
		p.add(m1);
		p.add(Box.createHorizontalGlue());
		p.add(m2);
		p.add(Box.createHorizontalGlue());
		p.add(m3);
		p.add(Box.createHorizontalGlue());
		p.add(m4);
		p.add(Box.createHorizontalGlue());
		p.add(m5);
		p.add(Box.createHorizontalGlue());
		p.add(m6);
		p.add(Box.createHorizontalGlue());
		p.add(m7);
		p.add(Box.createHorizontalGlue());
		p.add(m8);
		p.add(Box.createHorizontalGlue());
		
		return p;
	}

	@Override
	public void setProgress(float progress, String text) {
		if(text==null)
			return;
		
		StringTokenizer tok = new StringTokenizer(text, ",");
		while(tok.hasMoreElements()) {
			String[] tmp = tok.nextToken().split("=");
			boolean b = Boolean.parseBoolean(tmp[1]);
			if(tmp[0].equals("m0")) 
				m0.setSelected(b);
			else if(tmp[0].equals("m1")) 
				m1.setSelected(b);
			else if(tmp[0].equals("m2")) 
				m2.setSelected(b);
			else if(tmp[0].equals("m3")) 
				m3.setSelected(b);
			else if(tmp[0].equals("m4")) 
				m4.setSelected(b);
			else if(tmp[0].equals("m5")) 
				m5.setSelected(b);
			else if(tmp[0].equals("m6")) 
				m6.setSelected(b);
			else if(tmp[0].equals("m7")) 
				m7.setSelected(b);
			else if(tmp[0].equals("m8")) 
				m8.setSelected(b);
		}
		repaint();
	}

	private JCheckBox getCheckBox(String txt) {
		JCheckBox check = new JCheckBox(txt);
		check.setBackground(Color.WHITE);
		MouseListener[] ml = check.getMouseListeners();
		for(MouseListener m : ml)
			check.removeMouseListener(m);
		return check;
	}
}
