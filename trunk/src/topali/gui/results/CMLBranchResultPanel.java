// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.Printable;
import java.util.Vector;

import javax.swing.*;

import pal.statistics.ChiSquareDistribution;
import topali.data.*;
import topali.gui.atv.ATV;
import topali.var.NHTreeUtils;

/**
 * Panel for displaying codeml branch model results
 */
public class CMLBranchResultPanel extends ResultPanel
{

	TablePanel p1, p2;

	public CMLBranchResultPanel(AlignmentData data, CodeMLResult result)
	{
		super(data, result);

		p1 = createTable1();
		p2 = createTable2();

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Color.WHITE);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		panel.add(getHeaderPanel(), c);
		c.gridy = 1;
		panel.add(p1, c);
		c.gridy = 2;
		panel.add(p2, c);
		
		addContent(panel, false);
	}

	private JPanel getHeaderPanel()
	{
		CodeMLResult result = (CodeMLResult) super.result;

		JPanel p = new JPanel();
		JLabel l0 = new JLabel("Hypothesis tested:");
		l0.setBackground(Color.WHITE);
		p.add(l0);
		p.setBorder(BorderFactory.createTitledBorder(""));
		p.setBackground(Color.WHITE);

		CMLHypothesis h0 = result.hypos.get(0);
		if (h0.omegas != null)
		{
			double omega = h0.omegas[0];
			String tree = tree2ATV(h0.tree, omega);
			p.add(new HypoLabel(0, tree, this));
		}

		for (int i = 1; i < result.hypos.size(); i++)
		{
			CMLHypothesis h = result.hypos.get(i);
			String tree = tree2ATV(h.omegaTree);
			JLabel l = new HypoLabel(i, tree, this);
			l.setBackground(Color.WHITE);
			p.add(l);
		}
		return p;
	}

	private TablePanel createTable1()
	{
		CodeMLResult result = (CodeMLResult) super.result;
		
		Vector<String> names = new Vector<String>();
		names.add("Hypothesis");
		int n = 0;
		for (CMLHypothesis hypo : result.hypos)
		{
			if (hypo.omegas.length > n)
				n = hypo.omegas.length;
		}
		for (int i = 0; i < n; i++)
			names.add("w" + i);
		names.add("Likelihood");

		Vector<Vector> data = new Vector<Vector>();
		for (int i = 0; i < result.hypos.size(); i++)
		{
			Vector<String> row = new Vector<String>();
			CMLHypothesis hypo = result.hypos.get(i);
			row.add("H" + i);
			for (int j = 0; j < n; j++)
			{
				if (j < hypo.omegas.length)
					row.add(ResultPanel.omegaFormat.format(hypo.omegas[j]));
				else
					row.add("");
			}
			row.add(ResultPanel.likelihoodFormat
					.format(result.hypos.get(i).likelihood));
			data.add(row);
		}

		TablePanel p = new TablePanel(data, names, TablePanel.RIGHT);
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createTitledBorder("Likelihood/Omega values"));
		return p;
	}

	private TablePanel createTable2()
	{
		CodeMLResult result = (CodeMLResult) super.result;
		Vector<String> names = new Vector<String>();

		names.add("");
		for (int i = 0; i < result.hypos.size(); i++)
			names.add("H" + i);

		Vector<Vector> data = new Vector<Vector>();
		for (int i = 0; i < result.hypos.size(); i++)
		{
			Vector<String> row = new Vector<String>();
			row.add("H" + i);
			for (int j = 0; j < result.hypos.size(); j++)
			{
				if (i == j)
				{
					row.add("--");
				} else
				{
					double l1 = result.hypos.get(i).likelihood;
					double l2 = result.hypos.get(j).likelihood;
					double lr = (l1 > l2) ? 2 * (l1 - l2) : 2 * (l2 - l1);
					int df = result.hypos.get(i).omegas.length
							- result.hypos.get(j).omegas.length;
					if (df < 0)
						df *= -1;

					if (df == 0)
					{
						row.add("--");
					} else
					{
						double lrt = 1 - ChiSquareDistribution.cdf(lr, df);
						if (lrt < 0.001)
							row.add("<0.001");
						else if (lrt < 0.01)
							row.add("<0.01");
						else if (lrt < 0.05)
							row.add("<0.05");
						else
							row.add("NS");
					}
				}
			}
			data.add(row);
		}

		TablePanel p = new TablePanel(data, names,
				TablePanel.RIGHT);
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createTitledBorder("LRT - p values"));
		return p;
	}

	/**
	 * Converts the omega values (paml) to branch lengths, so that they can be
	 * visualized in ATV.
	 * 
	 * @param tree
	 * @return
	 */
	private String tree2ATV(String tree)
	{
		tree = NHTreeUtils.removeBranchLengths(tree);

		// transform omega values to branch lengths
		StringBuffer sb = new StringBuffer();
		String[] tmp = tree.split("\\s+");
		for (String s : tmp)
		{
			if (s.matches("#\\d+\\.\\d+.*"))
			{
				s = s.replaceFirst("#", ": ");
			}
			sb.append(s);
			sb.append(' ');
		}
		return sb.toString();
	}

	/**
	 * Converts the omega values (paml) to branch lengths, so that they can be
	 * visualized in ATV.
	 * 
	 * @param tree
	 * @param w fixed omega value, added to each node
	 * @return
	 */
	private String tree2ATV(String tree, double w)
	{
		tree = NHTreeUtils.removeBranchLengths(tree);

		StringBuffer sb = new StringBuffer();
		for (char c : tree.toCharArray())
		{
			// before each ) and ,
			if (c == ')' || c == ',')
			{
				sb.append(" : ");
				sb.append(w);
			}

			sb.append(c);
		}

		return sb.toString();
	}

	@Override
	public String getAnalysisInfo()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Analysis type: Branch model\n\n");

		CodeMLResult res = (CodeMLResult) result;
		for (int i = 0; i < res.hypos.size(); i++)
		{
			sb.append("Hypothesis H" + i + "\n");
			sb.append(res.hypos.get(i));
			sb.append("\n");
		}

		sb.append("\nSelected sequences:\n");
		for (String seq : result.selectedSeqs)
			sb.append("\n  " + data.getSequenceSet().getNameForSafeName(seq));

		return sb.toString();
	}

	@Override
	public void setThreshold(double t)
	{
		// There is no threshold to set
	}

	@Override
	public Printable[] getPrintables()
	{
		Printable[] p = new Printable[2];
		p[0] = p1;
		p[1] = p2;
		return p;
	}

	/**
	 * Clickable HTML label
	 */
	class HypoLabel extends JLabel implements MouseListener
	{
		int i;

		String tree;

		Component parent;

		public HypoLabel(int i, String tree, Component parent)
		{
			this.i = i;
			this.tree = tree;
			this.parent = parent;
			this.setText("<html><u>H" + i + "</u></html>");
			this.setForeground(Color.BLUE);
			this.addMouseListener(this);
			this.setToolTipText("Show tree");
		}

		public void mouseClicked(MouseEvent e)
		{
			// display wait cursor, atv will reset to default cursor after
			// started up.
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			ATV atv = new ATV(tree, "H" + i, parent, null);
			atv.showBranchLengths(true);
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
