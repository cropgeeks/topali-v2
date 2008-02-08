// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.*;

/**
 * Table which supports multiple line cells (with auto-linebreak),
 * export to CSV and PNG, is printable, and can be set (not) editable.
 */
public class CustomTable extends JTable
{
	boolean editable = false;
	
	Vector<String> headerToolTips;
	
	public CustomTable(Vector<Vector<Object>> rowData, Vector<String> columnNames)
	{
		this.setModel(new MyTablemodel(rowData, columnNames));
		init();
	}

	/**
	 * If the table should be editable or not.
	 * @param b 
	 */
	public void setEditable(boolean b) {
		this.editable = b;
	}
	
	public void setHeaderToolTips(Vector<String> tt) {
		if(tt.size()!=getColumnCount())
			return;
		
		this.headerToolTips = tt;
		
		JTableHeader th = new JTableHeader(columnModel) {
            @Override
	    public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                return headerToolTips.get(realIndex);
            }
        };
        
        this.setTableHeader(th);
	}
	
	private void init()
	{
		setSize(getSize().width, 100000);
		
		for (int i = 0; i < getColumnModel().getColumnCount(); i++)
		{
			TableColumn c = getColumnModel().getColumn(i);
			c.setCellRenderer(new MyRenderer());
			c.setCellEditor(new MyEditor());
		}
		
		adjustRowHeight();
	}

	/**
	 * Adjusts the row height according to the highest cell
	 */
	public void adjustRowHeight() {
		for(int i=0; i<getRowCount(); i++) {
			int hmax = 0;
			for(int j=0; j<getColumnCount(); j++) {
				TableCellRenderer rend = getCellRenderer(i, j);
				Component c = rend.getTableCellRendererComponent(this, this.dataModel.getValueAt(i, j), false, false, i, j);
				//This strange thing has to be done for correct rendering:
				c.setSize(columnModel.getColumn(j).getWidth(), 100000);
				if(c.getPreferredSize().height>hmax) {
					hmax = c.getPreferredSize().height;
				}
			}
			setRowHeight(i, hmax);
		}
	}
	
	/**
	 * 
	 * @return PNG encoded image of the table
	 */
	public BufferedImage getBufferedImage() {
		int selected = getSelectedRow();
		getSelectionModel().clearSelection();
		
		BufferedImage bi = new BufferedImage(getSize().width, getSize().height+getTableHeader().getSize().height,BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g2d = bi.createGraphics();
		getTableHeader().paint(g2d);
		g2d.translate(0, getTableHeader().getSize().height);
		paint(g2d);
		
		getSelectionModel().setSelectionInterval(selected, selected);
		
		return bi;
	}
	
	/**
	 * 
	 * @return Table data in "comma separated values" format
	 */
	public String getCSV()
	{
		String nl = System.getProperty("line.separator");

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < getColumnCount() - 1; i++)
		{
			sb.append(getModel().getColumnName(i));
			sb.append(',');
		}
		if (getColumnCount() > 0)
		{
			sb.append(getModel().getColumnName(getColumnCount() - 1));
			sb.append(nl);
		}

		for (int i=0; i<getRowCount(); i++)
		{
			for (int j = 0; j < getColumnCount() - 1; j++)
			{
				String tmp = getValueAt(i, j).toString();
				sb.append(tmp.replaceAll("\\<.*\\>", ""));
				sb.append(',');
			}
			if (getColumnCount() > 0)
			{
				String tmp = getValueAt(i, getColumnCount()-1).toString();
				sb.append(tmp.replaceAll("\\<.*\\>", ""));
				sb.append(nl);
			}
		}

		return sb.toString();
	}

	class MyTablemodel extends DefaultTableModel
	{

		public MyTablemodel(Object[][] data, Object[] columnNames)
		{
			super(data, columnNames);
		}

		public MyTablemodel(Vector<Vector<Object>> data, Vector<String> columnNames)
		{
			super(data, columnNames);
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return editable;
		}
		
	}

	class MyRenderer extends MyTextArea implements TableCellRenderer {
		
		public MyRenderer() {
			Font font = getTableHeader().getFont();
			setFont(font);
		}
		
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			String text = value.toString();
			
			//determine background color
			Color selColor = UIManager.getColor("Table.selectionBackground");
			Color color = UIManager.getColor("Table.background");
			if(text.matches(".*\\<color.+\\>.*")) {
				int i = text.indexOf("<color");
				int s = text.indexOf('=', i);
				int e = text.indexOf('>', i);
				String[] rgb = text.substring(s+1, e).split(",");
				color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			}
			if(isSelected) 
				setBackground(mixColor(color, selColor));
			else 
				setBackground(color);

			//determine tooltip text
			if(text.matches(".*\\<tooltip.+\\>.*")) {
				int i = text.indexOf("<tooltip");
				int s = text.indexOf('"', i);
				int e = text.indexOf('"', s+1);
				String tt = text.substring(s+1, e);
				setToolTipText(tt);
			}
			else
				setToolTipText(null);
			
			if(text.matches(".*\\<b\\>.*")) 
				setFont(getFont().deriveFont(Font.BOLD));
			else
				setFont(getFont().deriveFont(Font.PLAIN));
			
			//remove all markup tags
			text = text.replaceAll("\\<\\D+.*\\>", "");
			
			setText(text);
		    
			return this;
		}
		
		private Color mixColor(Color c1, Color c2) {
			int r = (c1.getRed()+c2.getRed())/2;
			int g = (c1.getGreen()+c2.getGreen())/2;
			int b = (c1.getBlue()+c2.getBlue())/2;
			return new Color(r,g,b);
		}
	}
	
	class MyEditor extends AbstractCellEditor implements TableCellEditor {

		MyTextArea ta;
		
		public MyEditor() {
			ta = new MyTextArea();
		}
			
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			ta.setText(value.toString());
		    JScrollPane sp = new JScrollPane(ta);
		    sp.setBorder(null);
			return sp;
		}

		public Object getCellEditorValue()
		{
			return ta.getText();
		}
	}
	
	class MyTextArea extends JTextArea {

		public MyTextArea() {
			super(0,0);
			setLineWrap(true);
			
			Font font = getTableHeader().getFont();		
			setFont(font);
			
			setWrapStyleWord(true);
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}
		
		
	}
}
