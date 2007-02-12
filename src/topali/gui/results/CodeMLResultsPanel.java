// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import topali.data.*;
import topali.gui.Icons;

public class CodeMLResultsPanel extends JPanel
{
	private Vector<CodeMLModel> models;

	JTable table;

	final float minP = 0.95f;

	public CodeMLResultsPanel(CodeMLResult result)
	{
		this.models = result.models;

		this.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		setLayout(new BorderLayout());
		table = getTable();
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	private JTable getTable()
	{
		TableModel tmodel = new myTableModel();
		JTable table = new JTable(tmodel);
		return table;
	}

	class myTableModel extends AbstractTableModel
	{

		public int getColumnCount()
		{
			return 15;
		}

		public int getRowCount()
		{
			return models.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			String res = null;
			CodeMLModel model = models.get(rowIndex);
			switch (columnIndex)
			{
			case 0:
				res = "" + model.name;
				break;
			case 1:
				res = "" + model.params;
				break;
			case 2:
				res = "" + model.likelihood;
				break;
			case 3:
				res = "" + model.dnDS;
				break;
			case 4:
				res = "" + model.w;
				break;
			case 5:
				res = "" + model.p0;
				break;
			case 6:
				res = "" + model.p1;
				break;
			case 7:
				res = "" + model.p2;
				break;
			case 8:
				res = "" + model.w0;
				break;
			case 9:
				res = "" + model.w1;
				break;
			case 10:
				res = "" + model.w2;
				break;
			case 11:
				res = "" + model.p;
				break;
			case 12:
				res = "" + model.q;
				break;
			case 13:
				res = "" + model._w;
				break;
			case 14:
				res = getPSS(model);
				break;
			default:
				res = "";
			}
			if (res.startsWith("-1"))
				res = "";
			return res;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		@Override
		public String getColumnName(int column)
		{
			switch (column)
			{
			case 0:
				return "Model";
			case 1:
				return "Free parameters";
			case 2:
				return "Likelihood";
			case 3:
				return "dN/dS";
			case 4:
				return "W";
			case 5:
				return "P0";
			case 6:
				return "P1";
			case 7:
				return "P2";
			case 8:
				return "W0";
			case 9:
				return "W1";
			case 10:
				return "W2";
			case 11:
				return "P";
			case 12:
				return "Q";
			case 13:
				return "W";
			case 14:
				return "PSS";
			default:
				return "";
			}
		}

		private String getPSS(CodeMLModel m)
		{
			if (m.getPSS(-1f) == null)
				return "-";

			if (m.getPSS(minP).size() > 0)
			{
				StringBuffer res = new StringBuffer();
				res.append("<html>");
				for (CodeMLModel.PSS pss : m.getPSS(0.95f))
				{
					if (pss.p > 0.99f)
						res.append("<b>" + pss + "</b>");
					else
						res.append(pss);
					res.append(' ');
				}
				res.append("</html>");
				return res.toString();
			} else
				return "0";
		}

	}
}