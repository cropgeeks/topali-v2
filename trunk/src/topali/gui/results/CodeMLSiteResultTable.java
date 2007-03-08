// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.Component;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import topali.data.*;

public class CodeMLSiteResultTable extends JTable implements ListSelectionListener
{
	CodeMLSiteResultPanel panel;
	
	float threshold = 0.95f;
	
	Vector<CMLModel> models;
	
	public CodeMLSiteResultTable(CodeMLResult result, CodeMLSiteResultPanel panel) {
		this.panel = panel;
		this.models = result.models;
		
		TableModel tmodel = new MyTableModel();
		this.setModel(tmodel);
		this.getColumnModel().getColumn(14).setCellRenderer(
				new PSSCellRenderer());
		this.getColumnModel().getColumn(14).setCellEditor(
				new PSSCellEditor());
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setColumnSelectionAllowed(false);
		this.getSelectionModel().addListSelectionListener(this);
		
		this.getColumnModel().getColumn(0).setPreferredWidth(170);
	}
	
	public void setThreshold(float threshold) {
		this.threshold = threshold;
		repaint();
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		int row = getSelectedRow();
		//CMLModel model = models.get(row);
		//panel.setSelectedModel(model);
		panel.setSelectedModel(row);
		repaint();
	}

	class MyTableModel extends AbstractTableModel
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
			CMLModel model = models.get(rowIndex);
			switch (columnIndex)
			{
			case 0:
				res = "" + model.name;
				break;
			case 1:
				res = "" + model.nParameter;
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
				return model.getPSS(threshold);
			default:
				res = "";
			}
			if (res.trim().equals("-1.0"))
				res = "";
			return res;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			CMLModel m = models.get(rowIndex);
			if(columnIndex==14 && (m.getPSS(threshold)!=null && m.getPSS(threshold).size()>0))
				return true;
			else
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
	}

	class PSSCellRenderer implements TableCellRenderer
	{

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			CMLModel m = models.get(row);
			List<PSSite> pss = m.getPSS(threshold);
			
			if(pss == null) {
				JLabel l = new JLabel("-");
				l.setOpaque(true);
				if(row==table.getSelectedRow()) {
					l.setBackground(UIManager.getColor("Table.selectionBackground"));
					l.setForeground(UIManager.getColor("Table.selectionForeground"));
				}
				else {
					l.setBackground(UIManager.getColor("Table.background"));
					l.setForeground(UIManager.getColor("Table.foreground"));
				}
				
				if(table.getRowHeight(row)>20) // if-statement to prevent endless loops (setRowHeight calls revalidate/repaint which calls this method again...
					table.setRowHeight(row, 20);
				return l;
			}
			
			else if (pss.size()>0)
			{
				JList list = new JList(pss.toArray());
				if(row==table.getSelectedRow()) {
					list.setBackground(UIManager.getColor("Table.selectionBackground"));
					list.setForeground(UIManager.getColor("Table.selectionForeground"));
				}
				else {
					list.setBackground(UIManager.getColor("Table.background"));
					list.setForeground(UIManager.getColor("Table.foreground"));
				}
				
				if(table.getRowHeight(row)<40)
					table.setRowHeight(row, 40);
				return new JScrollPane(list);
			} 
			else
			{
				JLabel l = new JLabel("0");
				l.setOpaque(true);
				if(row==table.getSelectedRow()) {
					l.setBackground(UIManager.getColor("Table.selectionBackground"));
					l.setForeground(UIManager.getColor("Table.selectionForeground"));
				}
				else {
					l.setBackground(UIManager.getColor("Table.background"));
					l.setForeground(UIManager.getColor("Table.foreground"));
				}
				
				if(table.getRowHeight(row)>20)
					table.setRowHeight(row, 20);
				return l;
			}
		}
	}

	class PSSCellEditor extends AbstractCellEditor implements
			TableCellEditor
	{
		public PSSCellEditor()
		{
		}

		public Object[] getCellEditorValue()
		{
			return null;
		}

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column)
		{
			CMLModel m = models.get(row);
			List<PSSite> pss = m.getPSS(threshold);
			
			if(pss == null) {
				if(table.getRowHeight(row)>20)
					table.setRowHeight(row, 20);
				return new JLabel("-");
			}
			
			else if (pss.size()>0)
			{
				JList list = new JList(pss.toArray());
				if(row==table.getSelectedRow()) {
					list.setBackground(UIManager.getColor("Table.selectionBackground"));
					list.setForeground(UIManager.getColor("Table.selectionForeground"));
				}
				else {
					list.setBackground(UIManager.getColor("Table.background"));
					list.setForeground(UIManager.getColor("Table.foreground"));
				}
				
				if(table.getRowHeight(row)<40)
					table.setRowHeight(row, 40);
				return new JScrollPane(list);
			} 
			else
			{
				if(table.getRowHeight(row)>20)
					table.setRowHeight(row, 20);
				return new JLabel("0");
			}
		}
	}
}
