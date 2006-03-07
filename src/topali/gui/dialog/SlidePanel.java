package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import topali.data.*;

import doe.*;

class SlidePanel extends JPanel
{
	private JLabel label1, label2;
	private SpinnerNumberModel winModel, stepModel;
	private JSpinner winSpin, stepSpin;
	
	SlidePanel(AlignmentData data, int window, int step)
	{
		int length = data.getSequenceSet().getLength();
		
		// Check settings for validity
		if (window < 2 || window > length)
			window = (int) length / 3;
		if (step < 1 || step > length)
			step = 1;
		
		winModel = new SpinnerNumberModel(window, 2, length, 1);
		stepModel = new SpinnerNumberModel(step, 1, length, 1);
		winSpin = new JSpinner(winModel);
		stepSpin = new JSpinner(stepModel);
		
		((JSpinner.NumberEditor)winSpin.getEditor()).getTextField()
			.setToolTipText("Window size (in nucleotides) to use when analyzing the alignment");
		((JSpinner.NumberEditor)stepSpin.getEditor()).getTextField()
			.setToolTipText("Step size (in nucleotides) to use when moving along the alignment");
		
		label1 = new JLabel("Step size: ");
		label1.setDisplayedMnemonic(KeyEvent.VK_S);
		label1.setLabelFor(((JSpinner.NumberEditor)stepSpin.getEditor()).getTextField());
		label2 = new JLabel("Window size: ");
		label2.setDisplayedMnemonic(KeyEvent.VK_W);
		label2.setLabelFor(((JSpinner.NumberEditor)winSpin.getEditor()).getTextField());
		
		
		DoeLayout layout = new DoeLayout();
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(layout.getPanel());
		
		layout.add(label1, 0, 0, 0, 1, new Insets(0, 15, 0, 5));
		layout.add(stepSpin, 1, 0, 0, 1, new Insets(0, 5, 0, 5));
		layout.add(new JLabel(" "), 2, 0, 1, 1, new Insets(0, 0, 5, 5));
		
		layout.add(label2, 0, 1, 0, 1, new Insets(5, 15, 0, 5));
		layout.add(winSpin, 1, 1, 0, 1, new Insets(5, 5, 0, 5));
		layout.add(new JLabel(" "), 2, 1, 1, 1, new Insets(5, 5, 5, 5));
	}
	
	int getStepSize()
		{ return stepModel.getNumber().intValue(); }
	
	int getWindowSize()
		{ return winModel.getNumber().intValue(); }
}