// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.print.Printable;

import javax.swing.JPanel;

import topali.data.*;
import topali.gui.GradientPanel;

public class CodonWResultPanel extends ResultPanel
{

	TextPanel panel = null;

	public CodonWResultPanel(AlignmentData data, CodonWResult result)
	{
		super(data, result);
		panel = new TextPanel(null);
		panel.setText(result.result);

		GradientPanel gp = new GradientPanel("CodonW Output");
		gp.setStyle(GradientPanel.OFFICE2003);
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(gp, BorderLayout.NORTH);
		p1.add(panel);

		addContent(p1, false);
	}

	
	public String getAnalysisInfo()
	{
		CodonWResult res = (CodonWResult)result;

		StringBuffer sb = new StringBuffer();
		sb.append("\nRuntime: " + ((result.endTime - result.startTime) / 1000)+ " seconds\n\n");
		sb.append("Selected sequences:");
		for (String seq : res.selectedSeqs)
			sb.append("\n  " + data.getSequenceSet().getNameForSafeName(seq));

		sb.append("\n\nGenetic Code: "+res.geneticCode+"\n\n");

		sb.append("Additional CodonW parameters:\n\n" +
				"-gc3s\n" +
				"-enc\n" +
				"-cai\n" +
				"-cbi\n" +
				"-fop\n\n");

		sb.append("\n\nApplication: CodonW (Version 1.4.4)\n");
		sb.append("John F Peden (1999), Analysis of Codon Usage");
		return sb.toString();
	}

	
	public Printable[] getPrintables()
	{
		return new Printable[] {panel.getPrintable()};
	}

	
	public void setThreshold(double t)
	{
		//no threshold to set
	}

}
