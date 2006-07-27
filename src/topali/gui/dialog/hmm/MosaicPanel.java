// (C) 2003-2004 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.hmm;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import topali.data.*;

import doe.*;

class MosaicPanel extends JPanel implements ActionListener
{
	private SequenceSet ss;
	
	private JTable table;
	private DefaultTableModel model = new BreakpointTableModel();
	
	private TableEditListener listen1 = new TableEditListener();
	
	private JButton bAdd, bDel, bReset;
	
	private MosaicDisplay display;

	// Length of the alignment (in nucleotides)
	private int length;
	
	private String top1, top2, top3;
	

	MosaicPanel(AlignmentData data, HMMResult iResult)
	{
		ss = data.getSequenceSet();
		length = ss.getLength();		
		createTopologyStrings();
		
		// Table
		table = new JTable();
		table.setModel(model);
	
		String[] columnNames = { "Breakpoint", "Topology Before Breakpoint" };
		model.setColumnIdentifiers(columnNames);
		
		JScrollPane sp = new JScrollPane(table);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		sp.setPreferredSize(new Dimension(10, table.getRowHeight() * 5));
		
		TableColumn column = table.getColumnModel().getColumn(1);
		JComboBox comboBox = new JComboBox();
		comboBox.addItem(getTopology(1));
		comboBox.addItem(getTopology(2));
		comboBox.addItem(getTopology(3));
		column.setCellEditor(new DefaultCellEditor(comboBox));		

		// Buttons
		bAdd = new JButton("Add Breakpoint");
		bAdd.addActionListener(this);
		bAdd.setToolTipText("Add a new breakpoint to the topology structure "
			+ "list");
		bDel = new JButton("Remove Breakpoint");
		bDel.addActionListener(this);
		bDel.setToolTipText("Remove a breakpoint from the topology structure "
			+ "list");
		bReset = new JButton("Regenerate");
		bReset.addActionListener(this);
		bReset.setToolTipText("Regenerate the topology structure so it "
			+ "contains values suitable for the current sequences");
				
		JPanel p1 = new JPanel(new GridLayout(3, 1, 0, 5));
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		p1.add(bAdd);
		p1.add(bDel);
		p1.add(bReset);
		
		display = new MosaicDisplay(length);
		JPanel p4 = new JPanel(new BorderLayout());
		p4.add(display, BorderLayout.CENTER);
		p4.setBorder(BorderFactory.createLoweredBevelBorder());
		
		JPanel p3 = new JPanel(new BorderLayout());
		p3.add(p4, BorderLayout.CENTER);
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(5, 5));
		add(p3, BorderLayout.NORTH);
		add(sp, BorderLayout.CENTER);
		add(p2, BorderLayout.EAST);
		
		// Initial state...
		if (iResult != null)
			setBreakPointArray(iResult.bpArray);
		else
			populateTable(null);

		checkData(false);
	}
	
	
	class TableEditListener implements CellEditorListener
	{
		public void editingCanceled(ChangeEvent e) {}
	
		public void editingStopped(ChangeEvent e)
		{
			checkData(false);
		}
	}
	
	private Vector<BreakPoint> checkData(boolean write)
	{
		Vector<BreakPoint> breakpoints = new Vector<BreakPoint>();
	
		for (int i = 0; i < model.getRowCount(); i++)
		{
			try
			{
				// Is the data valid?
				int bp = Integer.parseInt((String) model.getValueAt(i, 0));
//				int tp = Integer.parseInt((String) model.getValueAt(i, 1)) - 1;
				int tp = getTopology((String)model.getValueAt(i, 1)) - 1;
								
				// Check all rows apart from the last one (which is fixed)
				if (i != model.getRowCount()-1)
					if (bp < 1 || bp > length-1)
						throw new Exception();
				
				// Does it already exist?
				boolean ok = true;
				for (int j = 0; j < breakpoints.size(); j++)
				{
					BreakPoint point = breakpoints.elementAt(j);				
					if (point.breakpoint == bp)
					{
						ok = false;
						break;
					}
				}
				
				// Add it
				if (ok)
					breakpoints.addElement(new BreakPoint(bp, tp));
			}
			catch (Exception e)	{
				System.out.println(i + " " + e);
				continue;
			}
		}
		
		// Now sort the list into ascending order
		Vector<BreakPoint> sorted = new Vector<BreakPoint>();
		for (int i = 0; i < breakpoints.size(); i++)
		{
			BreakPoint p1 = breakpoints.elementAt(i);
			
			int insertAt = 0;
			for (; insertAt < sorted.size(); insertAt++)
			{
				BreakPoint p2 = sorted.elementAt(insertAt);
				if (p1.breakpoint < p2.breakpoint)
					break;
			}
			
			sorted.insertElementAt(p1, insertAt);
		}
				
		if (write)
			return sorted;
		
		display.setBreakpoints(sorted);
		return null;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bAdd)
		{
			Object[] data = { null, getTopology(1) };
			model.insertRow(model.getRowCount()-1, data);

			table.setEditingRow(table.getRowCount()-1);
			table.getCellEditor(table.getRowCount()-1, 0).
				addCellEditorListener(listen1);
			table.getCellEditor(table.getRowCount()-1, 1).
				addCellEditorListener(listen1);
		}
		
		else if (e.getSource() == bDel)
		{
			if (table.getSelectedRow() == model.getRowCount()-1)
				MsgBox.msg("The final breakpoint for this topology structure "
					+ "cannot be removed.", MsgBox.ERR);
			else if (table.getSelectedRow() != -1)
			{
				model.removeRow(table.getSelectedRow());
				checkData(false);
			}
		}
		
		else if (e.getSource() == bReset)
		{
			if (MsgBox.yesno("This will reset and regenerate the current "
				+ "topology structure so that it will contain values suitable "
				+ "for the sequences in question.\nIt is advisable to "
				+ "regenerate the structure if the selected sequences have "
				+ "changed, or you wish to discard any edits made."
				+ "\n\nContinue with regeneration?", 0) == JOptionPane.YES_OPTION)
			{
				model.setRowCount(0);
				populateTableFromTDS();
				checkData(false);
			}
		}
	}
	
	private void populateTable(Vector<BreakPoint> breakpoints)
	{
		// If no information was found
		if (breakpoints == null)
		{
			model.setRowCount(0);
			populateTableFromTDS();
			
//			String[] endBP = { "" + length, getTopology(1) };
//			model.addRow(endBP);
			
//			table.getCellEditor(0, 1).addCellEditorListener(listen1);
//			table.setEditingRow(0);
		}
		else
		{
			for (int i = 0; i < breakpoints.size(); i++)
			{
				BreakPoint point = breakpoints.elementAt(i);
				String[] bp = { "" + point.breakpoint, "" +	getTopology(point.topology+1) };
				model.addRow(bp);
				
				table.setEditingRow(table.getRowCount()-1);
				table.getCellEditor(table.getRowCount()-1, 0).
					addCellEditorListener(listen1);
				table.getCellEditor(table.getRowCount()-1, 1).
					addCellEditorListener(listen1);
			}
		}
	}
	
	private void populateTableFromTDS()
	{
		System.out.println("Populating via TDS...");
		
		int[] indices = ss.getSelectedSequences();
		StringBuffer[] buffers = new StringBuffer[indices.length];
		for (int i = 0; i < indices.length; i++)
			buffers[i] = ss.getSequence(indices[i]).getBuffer();
		
		int length = buffers[0].length();
		
		// The current topology
		int currTop = -1;
		
		// Scan every column
		for (int pos = 2, newTop = -1; pos <= length; pos++)
		{
			char s1 = buffers[0].charAt(pos-1);
			char s2 = buffers[1].charAt(pos-1);
			char s3 = buffers[2].charAt(pos-1);
			char s4 = buffers[3].charAt(pos-1);
			
			// Topology 1
			if (s1 == s2 && s3 == s4 && s1 != s3)
				newTop = 1;
			// Topology 2
			else if (s1 == s3 && s2 == s4 && s1 != s2)
				newTop = 2;
			// Topology 3
			else if (s1 == s4 && s2 == s3 && s1 != s3)
				newTop = 3;
			
			// Was there a significant column?
			if (newTop != -1 || pos == length)
			{				
				// Can we add it (ie, is it different?)
				if (currTop == -1 || currTop != newTop || pos == length)
				{
					// Special case for the first block (as it isn't defined 
					// until the first TDS region is found)
					if (currTop == -1) currTop = newTop;
					
					String[] row = { "" + pos, getTopology(currTop) };
					model.addRow(row);
					
					currTop = newTop;
				}
			}
		}
		
		// Ensure the last element is always in the table
		if (model.getRowCount() == 0)
		{
			String[] endBP = { "" + length, getTopology(1) };
			model.addRow(endBP);
		}

		table.setEditingRow(table.getRowCount()-1);
		table.getCellEditor(table.getRowCount()-1, 0).
			addCellEditorListener(listen1);
		table.getCellEditor(table.getRowCount()-1, 1).
			addCellEditorListener(listen1);
	}
	
	int[][] getBreakpointArray()
	{
		Vector<BreakPoint> breakpoints = checkData(true);
		
		// 2D array [0] = breakpoint postition, [1] = topology at that position
		int[][] bpArray = new int[breakpoints.size()][2];
		
		for (int i = 0; i < bpArray.length; i++)
		{
			BreakPoint bp = breakpoints.elementAt(i);
			bpArray[i][0] = bp.breakpoint;
			bpArray[i][1] = bp.topology;
		}
		
		return bpArray;
	}
	
	// Takes the data from a previous run and repopulates the table with it
	// (after first converting between array and Vector)
	void setBreakPointArray(int[][] bpArray)
	{
		if (bpArray == null)
			return;
		
		Vector<BreakPoint> breakpoints = new Vector<BreakPoint>(bpArray.length);
		
		for (int i = 0; i < bpArray.length; i++)
			breakpoints.addElement(new BreakPoint(bpArray[i][0], bpArray[i][1]));
		
		populateTable(breakpoints);
	}
	
	private void createTopologyStrings()
	{
		Sequence[] seqArray = ss.getSequencesArray(ss.getSelectedSequences());
		
		top1 = "1: (" + seqArray[0].name + ", " + seqArray[1].name +
			"), (" + seqArray[2].name + ", " + seqArray[3].name + ")";
			
		top2 = "2: (" + seqArray[0].name + ", " + seqArray[2].name +
			"), (" + seqArray[1].name + ", " + seqArray[3].name + ")";
			
		top3 = "3: (" + seqArray[0].name + ", " + seqArray[3].name +
			"), (" + seqArray[1].name + ", " + seqArray[2].name + ")";
	}
	
	private int getTopology(String str)
	{
		if (str.equals(top1)) return 1;
		else if (str.equals(top2)) return 2;
		else if (str.equals(top3)) return 3;
		
		return 0;
	}
	
	private String getTopology(int i)
	{
		if (i == 1) return top1;
		else if (i == 2) return top2;
		else if (i == 3) return top3;
		
		return null;
	}
	
	class BreakpointTableModel extends DefaultTableModel
	{
		public boolean isCellEditable(int row, int col)
        { 
        	if (row == getRowCount()-1 && col == 0)
        		return false;
        	else
        		return true;
        }
    }
}


