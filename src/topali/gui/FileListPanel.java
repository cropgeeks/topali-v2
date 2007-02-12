// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import topali.data.AlignmentData;
import topali.data.AlignmentFileStat;
import topali.gui.dialog.ImportDataSetDialog;
import doe.MsgBox;

public class FileListPanel extends JPanel implements ListSelectionListener
{
	private LinkedList<AlignmentFileStat> refs;

	private JTable table;

	private AlignmentTableModel model;

	private PanelToolBar toolbar;

	public FileListPanel(AlignmentData data)
	{
		refs = data.getReferences();

		setLayout(new BorderLayout());
		add(createControls());
		add(toolbar = new PanelToolBar(), BorderLayout.EAST);
	}

	private JPanel createControls()
	{
		table = new JTable(model = new AlignmentTableModel());
		table.sizeColumnsToFit(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.getSelectionModel().addListSelectionListener(this);

		// http://forum.java.sun.com/thread.jspa?forumID=57&threadID=726667
		// Widths to very large values, but proportional values, so it has the
		// effect of setting the column widths as a percentage of the total
		// width of the table. It's a bit of a hack, but it works.
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(52000);
		columnModel.getColumn(1).setPreferredWidth(12000);
		columnModel.getColumn(2).setPreferredWidth(12000);
		columnModel.getColumn(3).setPreferredWidth(12000);
		columnModel.getColumn(4).setPreferredWidth(12000);

		table.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() != 2)
					return;

				loadAlignment(table.rowAtPoint(e.getPoint()));
			}
		});

		JScrollPane sp = new JScrollPane(table);

		JPanel p1 = new JPanel(new BorderLayout());
		// p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(sp);

		return p1;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting())
			return;

		toolbar.bImport.setEnabled(table.getSelectedRowCount() == 1);
		toolbar.bRemove.setEnabled(table.getSelectedRowCount() > 0);
	}

	// Loads the currently selected alignment into TOPALi "properly".
	private void loadAlignment(int tableRow)
	{
		Object obj = table.getValueAt(tableRow, 0);
		if (obj == null)
			return;

		AlignmentFileStat stat = (AlignmentFileStat) obj;

		File file = new File(stat.filename);
		new ImportDataSetDialog(TOPALi.winMain).loadAlignment(file);
	}

	private void removeAlignment(int[] rows)
	{
		if (MsgBox.yesno("Are you sure you want to remove the selected "
				+ "alignments from this dataset?", 1) == JOptionPane.YES_OPTION)
		{

			for (int index = rows.length - 1; index >= 0; index--)
				model.removeAlignment(rows[index]);

			WinMainMenuBar.aFileSave.setEnabled(true);
		}
	}

	private class AlignmentTableModel extends DefaultTableModel
	{
		public String getColumnName(int col)
		{
			switch (col)
			{
			case 0:
				return "Alignment Filename";
			case 1:
				return "Sequences";
			case 2:
				return "Length";
			case 3:
				return "Type";
			case 4:
				return "Size on Disk";
			}

			return null;
		}

		public int getColumnCount()
		{
			return 5;
		}

		public int getRowCount()
		{
			return refs.size();
		}

		public Object getValueAt(int row, int col)
		{
			AlignmentFileStat stat = refs.get(row);

			switch (col)
			{
			case 0:
				return stat;
			case 1:
				return stat.size;
			case 2:
				return stat.length;
			case 3:
				return stat.isDna ? "DNA" : "Protein";
			case 4:
				return (stat.fileSize / 1024) + "KB";
			}

			return null;
		}

		void removeAlignment(int row)
		{
			refs.remove(row);
			removeRow(row);
		}
	}

	private class PanelToolBar extends JToolBar implements ActionListener
	{
		private JButton bImport, bRemove;

		PanelToolBar()
		{
			setFloatable(false);
			setBorderPainted(false);
			setOrientation(VERTICAL);
			setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

			bImport = (JButton) WinMainToolBar.getButton(false, null, "list01",
					Icons.TABLE_IMPORT, null);
			bImport.setEnabled(false);
			bImport.addActionListener(this);

			bRemove = (JButton) WinMainToolBar.getButton(false, null, "list02",
					Icons.TABLE_REMOVE, null);
			bRemove.setEnabled(false);
			bRemove.addActionListener(this);

			add(bImport);
			add(bRemove);
		}

		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == bImport)
				loadAlignment(table.getSelectedRow());
			else if (e.getSource() == bRemove)
				removeAlignment(table.getSelectedRows());
		}
	}
}