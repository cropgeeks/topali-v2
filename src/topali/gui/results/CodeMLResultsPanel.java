// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import topali.data.AlignmentData;
import topali.data.CodeMLModel;
import topali.data.CodeMLResult;
import topali.gui.Icons;

public class CodeMLResultsPanel extends JPanel
{
	private Vector<CodeMLModel> models;
	
	JTable table;
	
	public CodeMLResultsPanel(AlignmentData data, CodeMLResult result)
	{
		this.models = result.models;
		
		this.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		setLayout(new BorderLayout());
		table = getTable();
		add(new JScrollPane(table), BorderLayout.CENTER);
	}
		
	private JTable getTable() {
		TableModel tmodel = new myTableModel();
		JTable table = new JTable(tmodel);
		return table;
	}
	
	class myTableModel extends AbstractTableModel {
		
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
			switch(columnIndex) {
			case 0 : res = ""+model.name; break;
			case 1 : res = ""+model.params;break;
			case 2 : res = ""+model.likelihood;break;
			case 3 : res = ""+model.dnDS;break;
			case 4 : res = ""+model.w;break;
			case 5 : res =  ""+model.p0;break;
			case 6 : res = ""+model.p1;break;
			case 7 : res =  ""+model.p2;break;
			case 8 : res = ""+model.w0;break;
			case 9 : res =  ""+model.w1;break;
			case 10 : res =  ""+model.w2;break;
			case 11 : res =  ""+model.p;break;
			case 12 : res =  ""+model.q;break;
			case 13 : res = ""+model._w;break;
			case 14 : res = countPSS(model); break; 
			default: res = "";
			}
			if(res.startsWith("-1"))
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
			switch(column) {
			case 0 : return "Model"; 
			case 1 : return "Free parameters";
			case 2 : return "Likelihood";
			case 3 : return "dN/dS";
			case 4 : return "W";
			case 5 : return "P0";
			case 6 : return "P1";
			case 7 : return "P2";
			case 8 : return "W0";
			case 9 : return "W1";
			case 10 : return "W2";
			case 11 : return "P";
			case 12 : return "Q";
			case 13 : return "W";
			case 14 : return "PSS";
			default: return "";
			}
		}	
		
		private String countPSS(CodeMLModel m) {
			int a=0, b=0;
			if(!m.pss.equals("")) {
				for(String s : m.pss.split("\\s")) {
					String[] tmp = s.split("\\|");
					float w = Float.parseFloat(tmp[2]);
					float p = Float.parseFloat(tmp[3]);
					if(w>1) {
						if(p>0.95)
							a++;
						if(p>0.99)
							b++;
					}
				}
				if(b>0)
					return a+"("+b+")";
				else
					return a+"";
			}
			return "-";
		}
	}
	
}