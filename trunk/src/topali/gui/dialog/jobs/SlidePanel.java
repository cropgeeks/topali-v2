// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs;

import java.awt.*;
import java.awt.event.KeyEvent;

import javax.swing.*;

import topali.data.AlignmentData;
import topali.i18n.Text;
import scri.commons.gui.DoeLayout;

/* Simple panel containing oft-used Window and Step size settings controls */
class SlidePanel extends JPanel
{

	private JLabel label1, label2, label3;

	private SpinnerNumberModel winModel, stepModel;

	private JSpinner winSpin, stepSpin;

	private JCheckBox variable;
	
	SlidePanel(AlignmentData data, int window, int step, int varWinSize)
	{
	    //varWinSize:
	    //-1 = don't display
	    //0  = display, unselected
	    //1  = display, selected
	    
		int length = data.getSequenceSet().getLength();

		// Check settings for validity
		if (window < 2 || window > length)
			window = length / 3;
		if (step < 1 || step > length)
			step = 1;

		winModel = new SpinnerNumberModel(window, 2, length, 1);
		stepModel = new SpinnerNumberModel(step, 1, length, 1);
		winSpin = new JSpinner(winModel);
		stepSpin = new JSpinner(stepModel);
		
		variable = new JCheckBox();

		((JSpinner.NumberEditor) winSpin.getEditor())
				.getTextField()
				.setToolTipText(
						java.util.ResourceBundle.getBundle("topali/i18n/i18n").getString("SlidePanel.1"));
		((JSpinner.NumberEditor) stepSpin.getEditor())
				.getTextField()
				.setToolTipText(
						java.util.ResourceBundle.getBundle("topali/i18n/i18n").getString("SlidePanel.2"));

		label1 = new JLabel(java.util.ResourceBundle.getBundle("topali/i18n/i18n").getString("SlidePanel.3"));
		label1.setDisplayedMnemonic(KeyEvent.VK_S);
		label1.setLabelFor(((JSpinner.NumberEditor) stepSpin.getEditor())
				.getTextField());
		label2 = new JLabel(java.util.ResourceBundle.getBundle("topali/i18n/i18n").getString("SlidePanel.4"));
		label2.setDisplayedMnemonic(KeyEvent.VK_W);
		label2.setLabelFor(((JSpinner.NumberEditor) winSpin.getEditor())
				.getTextField());
		
		label3 = new JLabel(Text.getString("variable_window_size"));

		DoeLayout layout = new DoeLayout();
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(layout.getPanel());
		
		layout.add(label1, 0, 0, 0, 1, new Insets(0, 15, 0, 5));
		layout.add(stepSpin, 1, 0, 0, 1, new Insets(0, 5, 0, 5));
		layout.add(new JLabel(" "), 2, 0, 1, 1, new Insets(0, 0, 5, 5));

		layout.add(label2, 0, 1, 0, 1, new Insets(5, 15, 0, 5));
		layout.add(winSpin, 1, 1, 0, 1, new Insets(5, 5, 0, 5));
		layout.add(new JLabel(" "), 2, 1, 1, 1, new Insets(5, 5, 5, 5));
		
		if(varWinSize>=0) {
		    variable.setSelected(varWinSize==1);
		    layout.add(label3, 0, 2, 0, 1, new Insets(0, 15 ,0, 5));
		    layout.add(variable, 1, 2, 0, 1, new Insets(0, 15 ,0, 5));
		}
	}

	int getStepSize()
	{
		return stepModel.getNumber().intValue();
	}

	int getWindowSize()
	{
		return winModel.getNumber().intValue();
	}
	
	boolean getVarWinSize() {
	    return variable.isSelected();
	}
}