// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import topali.data.*;
import topali.gui.atv.ATV;

public class CodeMLBranchResultPanel extends JPanel
{

	AlignmentData data;
	CodeMLResult result;
	
	public CodeMLBranchResultPanel(AlignmentData data, CodeMLResult result)
	{
		this.data = data;
		this.result = result;
		
		this.setLayout(new BorderLayout());
		this.add(getHeaderPanel(), BorderLayout.NORTH);
		this.add(getLikelihoodTable(), BorderLayout.SOUTH);
	}
	
	private JPanel getHeaderPanel() {
		JPanel p = new JPanel();
		p.add(new JLabel("Hypothesis:"));
		for(int i=0; i<result.hypos.size(); i++) {
			CMLHypothesis h = result.hypos.get(i);
			JLabel l = new HypoLabel(i, h.omegaTree);
			l.setForeground(Color.BLUE);
			p.add(l);
		}
		return p;
	}
	
	private JTable getLikelihoodTable() {
		String[] clNames = {"Hypothesis", "Likelihood"};
		Object[][] data = new Object[result.hypos.size()][2];
		for(int i=0; i<result.hypos.size(); i++) {
			data[i][0] = "H"+i;
			data[i][1] = result.hypos.get(i).likelihood;
		}
		return new JTable(data, clNames);
	}
	
	class HypoLabel extends JLabel implements MouseListener{
		int i;
		String tree;
		
		public HypoLabel(int i, String tree) {
			this.i = i;
			this.tree = tree;
			this.setText("<html><u>H"+i+"</u></html>");
			this.addMouseListener(this);
		}
		
		public void mouseClicked(MouseEvent e)
		{	
			ATV atv = new ATV(tree, "H"+i, null);
			SwingUtilities.invokeLater(atv);
		}

		public void mouseEntered(MouseEvent e)
		{	
			setCursor(new Cursor(Cursor.HAND_CURSOR));
		}

		public void mouseExited(MouseEvent e)
		{	
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		public void mousePressed(MouseEvent e)
		{	
		}

		public void mouseReleased(MouseEvent e)
		{
		}
	}
}
