package topali.mod;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import javax.swing.border.MatteBorder;

import org.apache.log4j.Logger;

import topali.gui.Icons;
import topali.gui.dialog.PrinterDialog;
import doe.MsgBox;

/**
 * Based on Matthew Robinson, Pavel Vorobiev "Swing - Second Edition"
 */
public class PrintPreview extends JFrame implements ActionListener
{
	 Logger log = Logger.getLogger(this.getClass());
	
	protected int m_wPage;

	protected int m_hPage;

	protected Pageable m_target;

	protected JComboBox m_cbScale;

	protected PreviewContainer m_preview;

	JButton bPrint;
	JComboBox cbFormat;
	
	int scale = 25;
	
	public PrintPreview(Pageable target)
	{
		super("Print Preview");

		m_target = target;
		
		getContentPane().setLayout(new BorderLayout());

		JToolBar tb = new JToolBar();
		bPrint = new JButton("Print", Icons.PRINT);

		bPrint.addActionListener(this);
		bPrint.setAlignmentY(0.5f);
		bPrint.setMargin(new Insets(4, 6, 4, 6));
		tb.add(bPrint);

		cbFormat = new JComboBox(new String[]{"Portrait", "Landscape"});
		cbFormat.setMaximumSize(cbFormat.getPreferredSize());
		tb.addSeparator();
		tb.add(cbFormat);
		cbFormat.addActionListener(this);
		
		String[] scales =
		{ "10 %", "25 %", "50 %", "100 %" };
		m_cbScale = new JComboBox(scales);
		m_cbScale.setSelectedIndex(1);
		m_cbScale.addActionListener(this);
		m_cbScale.setMaximumSize(m_cbScale.getPreferredSize());
		tb.addSeparator();
		tb.add(m_cbScale);
		getContentPane().add(tb, BorderLayout.NORTH);

		m_preview = new PreviewContainer();

		JScrollPane ps = new JScrollPane(m_preview);
		getContentPane().add(ps, BorderLayout.CENTER);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		PageFormat pf = new PageFormat();
		pf.setOrientation(PageFormat.PORTRAIT);
		buildPreview(pf);
		
		setLocationRelativeTo(MsgBox.frm);
	}

	private void buildPreview(PageFormat pageFormat) {
		m_preview.removeAll();
		try
		{
			for (int pageIndex = 0; pageIndex < m_target.getNumberOfPages(); pageIndex++)
			{
				Printable printable = m_target.getPrintable(pageIndex);

				if (pageFormat.getHeight() == 0 || pageFormat.getWidth() == 0)
				{
					System.err.println("Unable to determine default page size");
					return;
				}
				m_wPage = (int) (pageFormat.getWidth());
				m_hPage = (int) (pageFormat.getHeight());
				int w = (m_wPage * scale / 100);
				int h = (m_hPage * scale / 100);

				BufferedImage img = new BufferedImage(m_wPage, m_hPage,
						BufferedImage.TYPE_INT_RGB);
				Graphics g = img.getGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, m_wPage, m_hPage);
				printable.print(g, pageFormat, 0);
				PagePreview pp = new PagePreview(w, h, img);
				m_preview.add(pp);
			}
		} catch (Exception e)
		{
			log.warn("Cannot print image\n",e);
		}
		m_preview.repaint();
	}
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource().equals(bPrint))
		{
			Printable[] toPrint = new Printable[m_target.getNumberOfPages()];
			for(int i=0; i<m_target.getNumberOfPages(); i++)
				toPrint[i] = m_target.getPrintable(i);
			
			PrinterDialog dlg = new PrinterDialog(toPrint);
			dlg.setVisible(true);
			
			requestFocus();
		}

		else if (e.getSource().equals(m_cbScale))
		{
			Thread runner = new Thread()
			{
				@Override
				public void run()
				{
					String str = m_cbScale.getSelectedItem().toString();
					if (str.endsWith("%"))
						str = str.substring(0, str.length() - 1);
					str = str.trim();
					try
					{
						scale = Integer.parseInt(str);
					} catch (NumberFormatException ex)
					{
						return;
					}
					int w = (m_wPage * scale / 100);
					int h = (m_hPage * scale / 100);

					Component[] comps = m_preview.getComponents();
					for (int k = 0; k < comps.length; k++)
					{
						if (!(comps[k] instanceof PagePreview))
							continue;
						PagePreview pp = (PagePreview) comps[k];
						pp.setScaledSize(w, h);
					}
					m_preview.doLayout();
					m_preview.getParent().getParent().validate();
				}
			};
			runner.start();
		}
		
		else if(e.getSource().equals(cbFormat)) {
			String str = cbFormat.getSelectedItem().toString();
			if(str.equals("Portrait")) {
				PageFormat pf = new PageFormat();
				pf.setOrientation(PageFormat.PORTRAIT);
				PrinterDialog.aset.remove(OrientationRequested.class);
				PrinterDialog.aset.add(OrientationRequested.PORTRAIT);
				buildPreview(pf);
			}
			else if(str.equals("Landscape")) {
				PageFormat pf = new PageFormat();
				pf.setOrientation(PageFormat.LANDSCAPE);
				PrinterDialog.aset.remove(OrientationRequested.class);
				PrinterDialog.aset.add(OrientationRequested.LANDSCAPE);
				buildPreview(pf);
			}
			m_preview.doLayout();
			m_preview.getParent().getParent().validate();
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(500, 400);
	}

	class PreviewContainer extends JPanel
	{
		protected int H_GAP = 16;

		protected int V_GAP = 10;

		@Override
		public Dimension getPreferredSize()
		{
			int n = getComponentCount();
			if (n == 0)
				return new Dimension(H_GAP, V_GAP);
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
				nRow++;

			int ww = nCol * (w + H_GAP) + H_GAP;
			int hh = nRow * (h + V_GAP) + V_GAP;
			Insets ins = getInsets();
			return new Dimension(ww + ins.left + ins.right, hh + ins.top
					+ ins.bottom);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public void doLayout()
		{
			Insets ins = getInsets();
			int x = ins.left + H_GAP;
			int y = ins.top + V_GAP;

			int n = getComponentCount();
			if (n == 0)
				return;
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
				nRow++;

			int index = 0;
			for (int k = 0; k < nRow; k++)
			{
				for (int m = 0; m < nCol; m++)
				{
					if (index >= n)
						return;
					comp = getComponent(index++);
					comp.setBounds(x, y, w, h);
					x += w + H_GAP;
				}
				y += h + V_GAP;
				x = ins.left + H_GAP;
			}
		}
	}

	class PagePreview extends JPanel
	{
		protected int m_w;

		protected int m_h;

		protected Image m_source;

		protected Image m_img;

		public PagePreview(int w, int h, Image source)
		{
			m_w = w;
			m_h = h;
			m_source = source;
			m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
			m_img.flush();
			setBackground(Color.white);
			setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
		}

		public void setScaledSize(int w, int h)
		{
			m_w = w;
			m_h = h;
			m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
			repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			Insets ins = getInsets();
			return new Dimension(m_w + ins.left + ins.right, m_h + ins.top
					+ ins.bottom);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public void paint(Graphics g)
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			g.drawImage(m_img, 0, 0, this);
			paintBorder(g);
		}
	}
}
