// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;

import topali.gui.*;

import doe.*;

/* Displays a dialog containing options for choosing the colours and fonts
 * used to display the sequence data within the main display.
 */
public class DisplaySettingsDialog extends JDialog implements ActionListener, ChangeListener
{
	private WinMain winMain;
	
	// Controls used by the colors
	private JList list;
	private JScrollPane sp;
	
	// Controls used by other settings
	private SpinnerNumberModel fontModel, colorModel;
	private JSpinner fontSpin, colorSpin;
	private JCheckBox showText, boldFont, showCols, smooth, line, tooltip, tree;
	private JCheckBox dimSeqs;
	private JCheckBox showVertHighlight, showHorzHighlight;
	
	private JButton close, defaults, help;

	public DisplaySettingsDialog(WinMain winMain)
	{
		super(winMain, Text.GuiDiag.getString("DisplaySettingsDialog.gui01"), true);
		this.winMain = winMain;
		
		close = new JButton(Text.Gui.getString("close"));
		close.addActionListener(this);
		defaults = new JButton(Text.Gui.getString("defaults"));
		defaults.addActionListener(this);
		help = TOPALiHelp.getHelpButton("display_settings");
		
		Utils.addCloseHandler(this, close);
	
		JPanel p1 = getColorControls();
		JPanel p2 = getFontControls();
		
		JPanel p3 = new JPanel(new GridLayout(1, 2, 5, 5));
		p3.add(p1);
		p3.add(p2);
		
		JPanel p4A = new JPanel(new GridLayout(1, 3, 5, 5));
		p4A.add(close);
		p4A.add(defaults);
		p4A.add(help);
		
		JPanel p4 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p4.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 0));
		p4.add(p4A);
		
		JPanel p5 = new JPanel(new BorderLayout(5, 5));
		p5.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
		p5.add(p3, BorderLayout.CENTER);
		p5.add(p4, BorderLayout.SOUTH);		
				
		getContentPane().add(p5, BorderLayout.CENTER);
		getRootPane().setDefaultButton(close);
	
		setStartupState();
		
		pack();			
		setResizable(false);
		setLocationRelativeTo(winMain);
		setVisible(true);
	}
	
	private void setStartupState()
	{	
		showText.setSelected(Prefs.gui_seq_show_text);
		boldFont.setSelected(Prefs.gui_seq_font_bold);
		showCols.setSelected(Prefs.gui_seq_show_colors);
		smooth.setSelected(Prefs.gui_graph_smooth);
		line.setSelected(Prefs.gui_graph_line);
		tooltip.setSelected(Prefs.gui_seq_tooltip);
		tree.setSelected(Prefs.gui_tree_unique_cols);
		dimSeqs.setSelected(Prefs.gui_seq_dim);
		showVertHighlight.setSelected(Prefs.gui_show_vertical_highlight);
		showHorzHighlight.setSelected(Prefs.gui_show_horizontal_highlight);
		
		fontSpin.setValue(new Integer(Prefs.gui_seq_font_size));
		colorSpin.setValue(new Integer(Prefs.gui_color_seed));
		
		createListColours();
	}
	
	private void createListColours()
	{
		DefaultListModel m = new DefaultListModel();
		m.addElement(new ColorObject(1,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui04"),
			Prefs.gui_seq_color_text));
		m.addElement(new ColorObject(2,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui05"),
			Prefs.gui_seq_color_a));
		m.addElement(new ColorObject(3,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui06"),
			Prefs.gui_seq_color_c));
		m.addElement(new ColorObject(4,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui07"),
			Prefs.gui_seq_color_g));
		m.addElement(new ColorObject(5,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui08"),
			Prefs.gui_seq_color_t));
		m.addElement(new ColorObject(6,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui09"),
			Prefs.gui_seq_color_gpst));
		m.addElement(new ColorObject(7,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui10"),
			Prefs.gui_seq_color_hkr));
		m.addElement(new ColorObject(8,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui11"),
			Prefs.gui_seq_color_fwy));
		m.addElement(new ColorObject(9,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui11"),
			Prefs.gui_seq_color_ilmv));
		m.addElement(new ColorObject(10,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui12"),
			Prefs.gui_seq_color_gaps));
		m.addElement(new ColorObject(11, Text.GuiDiag.getString("DisplaySettingsDialog.gui47"), Prefs.gui_seq_highlight));
		m.addElement(new ColorObject(12,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui13"),
			Prefs.gui_graph_threshold));
		m.addElement(new ColorObject(13,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui14"),
			Prefs.gui_graph_window));
		m.addElement(new ColorObject(14,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui15"),
			Prefs.gui_cardle_line));
		m.addElement(new ColorObject(15,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui16"),
			Prefs.gui_graph_background));
		m.addElement(new ColorObject(16,
			Text.GuiDiag.getString("DisplaySettingsDialog.gui17"),
			Prefs.gui_histo_background));
		
		list.setModel(m);
	}
		
	private void changeColor()
	{
		ColorObject co = (ColorObject) list.getSelectedValue();
		if (co != null)
			co.runColorChooser();
	}
	
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == fontSpin)
			Prefs.gui_seq_font_size = fontModel.getNumber().byteValue();
		else if (e.getSource() == colorSpin)
			Prefs.gui_color_seed = colorModel.getNumber().intValue();
			
		winMain.menuViewDisplaySettings(true);
	}
	
	private void defaultClicked()
	{
		String msg = Text.GuiDiag.getString("DisplaySettingsDialog.msg01");
		if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
			return;
	
		Prefs.setDisplayDefaults();
		setStartupState();
		
		winMain.menuViewDisplaySettings(true);
		winMain.ovDialog.createImage();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == defaults)
			defaultClicked();
		
		else if (e.getSource() == close)
			setVisible(false);
		
		else
		{
			Prefs.gui_seq_show_text = showText.isSelected();
			Prefs.gui_seq_font_bold = boldFont.isSelected();
			Prefs.gui_seq_show_colors = showCols.isSelected();
			Prefs.gui_graph_smooth = smooth.isSelected();
			Prefs.gui_graph_line = line.isSelected();
			Prefs.gui_seq_tooltip = tooltip.isSelected();
			Prefs.gui_tree_unique_cols = tree.isSelected();
			Prefs.gui_seq_dim = dimSeqs.isSelected();
			Prefs.gui_show_horizontal_highlight = showHorzHighlight.isSelected();
			Prefs.gui_show_vertical_highlight = showVertHighlight.isSelected();
		
			winMain.menuViewDisplaySettings(true);
		}
	}
	
	private JPanel getColorControls()
	{
		list = new JList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui19"));
		
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {			
				changeColor();
			}
		});
		
		list.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					changeColor();
			}
		});
		
		sp = new JScrollPane(list);
	
		createListColours();
		list.setCellRenderer(new ColorListRenderer());
		
		JPanel p1 = new JPanel(new BorderLayout(5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p1.add(sp, BorderLayout.CENTER);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createTitledBorder(
			Text.GuiDiag.getString("DisplaySettingsDialog.gui20")));
		p2.add(p1);
		
		return p2;
	}
	
	private JPanel getFontControls()
	{
		fontModel = new SpinnerNumberModel(Prefs.gui_seq_font_size, 1, 50, 1);
		fontSpin = new JSpinner(fontModel);
		fontSpin.addChangeListener(this);
		((JSpinner.NumberEditor)fontSpin.getEditor()).getTextField()
			.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui21"));
		
		colorModel = new SpinnerNumberModel(Prefs.gui_color_seed, 0, 999999, 1);
		colorSpin = new JSpinner(colorModel);
		colorSpin.addChangeListener(this);
		((JSpinner.NumberEditor)colorSpin.getEditor()).getTextField()
			.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui22"));
		
		showText = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui23"));
		showText.addActionListener(this);
		showText.setMnemonic(KeyEvent.VK_N);
		showText.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui24"));
			
		boldFont = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui25"));
		boldFont.addActionListener(this);
		boldFont.setMnemonic(KeyEvent.VK_B);
		boldFont.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui26"));
			
		showCols = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui27"));
		showCols.addActionListener(this);
		showCols.setMnemonic(KeyEvent.VK_C);
		showCols.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui28"));
		
		dimSeqs = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui41"));
		dimSeqs.addActionListener(this);
		dimSeqs.setMnemonic(KeyEvent.VK_D);
		dimSeqs.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui42"));
		
		showHorzHighlight = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui44"));
		showHorzHighlight.addActionListener(this);
		showHorzHighlight.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui46"));
		
		showVertHighlight = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui43"));
		showVertHighlight.addActionListener(this);
		showVertHighlight.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui45"));
		
		smooth = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui29"));
		smooth.addActionListener(this);
		smooth.setMnemonic(KeyEvent.VK_S);
		smooth.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui30"));
		
		line = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui31"));
		line.addActionListener(this);
		line.setMnemonic(KeyEvent.VK_G);
		line.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui32"));
		
		tooltip = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui33"));
		tooltip.addActionListener(this);
		tooltip.setMnemonic(KeyEvent.VK_A);
		tooltip.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui34"));
		
		tree = new JCheckBox(Text.GuiDiag.getString("DisplaySettingsDialog.gui35"));
		tree.addActionListener(this);
		tree.setMnemonic(KeyEvent.VK_U);
		tree.setToolTipText(Text.GuiDiag.getString("DisplaySettingsDialog.gui36"));
		
		JLabel label2 = new JLabel(Text.GuiDiag.getString("DisplaySettingsDialog.gui37"));
		label2.setDisplayedMnemonic('F');
		label2.setLabelFor(((JSpinner.NumberEditor)fontSpin.getEditor())
			.getTextField());
		JLabel label3 = new JLabel(Text.GuiDiag.getString("DisplaySettingsDialog.gui38"));
		label3.setDisplayedMnemonic('S');
		label3.setLabelFor(((JSpinner.NumberEditor)colorSpin.getEditor())
			.getTextField());
		
		DoeLayout layout = new DoeLayout();
		
		layout.add(label2, 0, 0, 1, 1, new Insets(5, 5, 5, 5));
		layout.add(fontSpin, 1, 0, 1, 1, new Insets(5, 5, 5, 5));
		layout.add(label3, 0, 2, 1, 1, new Insets(5, 5, 5, 5));
		layout.add(colorSpin, 1, 2, 0, 1, new Insets(5, 5, 5, 5));
		
		layout.add(showText, 0, 3, 1, 2, new Insets(5, 1, 0, 5));
		layout.add(showCols, 0, 4, 1, 2, new Insets(0, 1, 0, 5));
		layout.add(dimSeqs, 0, 5, 1, 2, new Insets(0, 1, 0, 5));
		layout.add(boldFont, 0, 6, 1, 2, new Insets(0, 1, 0, 5));
//		layout.add(smooth, 0, 7, 1, 2, new Insets(0, 1, 0, 5));
//		layout.add(line, 0, 8, 1, 2, new Insets(0, 1, 0, 5));
		layout.add(tooltip, 0, 9, 1, 2, new Insets(0, 1, 0, 5));
//		layout.add(tree, 0, 10, 1, 2, new Insets(0, 1, 5, 5));
		layout.add(showVertHighlight, 0, 11, 1, 2, new Insets(0,1,0,5));
		layout.add(showHorzHighlight, 0, 12, 1, 2, new Insets(0,1,0,5));
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createTitledBorder(
			Text.GuiDiag.getString("DisplaySettingsDialog.gui39")));
		p1.add(layout.getPanel(), BorderLayout.CENTER);
		
		return p1;
	}

	class ColorListRenderer extends JLabel implements ListCellRenderer 
	{
		public ColorListRenderer()
		{
			// Don't paint behind the component
			setOpaque(true);
		}
		
		// Set the attributes of the class and return a reference
		public Component getListCellRendererComponent(JList list, Object o,
			int i, boolean iss, boolean chf)
		{
			ColorObject co = (ColorObject) o;
		
			// Set the font
			setFont(list.getFont());
			
			// Set the text
		    setText(co.name);
		    		    
			// Set the icon	    
		    BufferedImage image = new BufferedImage(20, 10,
		    	BufferedImage.TYPE_INT_RGB);
			Graphics g = image.createGraphics();
					
			g.setColor(co.color);
			g.fillRect(0, 0, 20, 10);
			g.setColor(Color.black);
			g.drawRect(0, 0, 20, 10);
			g.dispose();
			
			setIcon(new ImageIcon(image));
			
	
		    // Set background/foreground colours
			if (iss)
			{ 
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else
			{
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
	
			return this;
		}
		
		public Insets getInsets(Insets i)
		{
			return new Insets(0, 3, 0, 0);
		}
	}

	class ColorObject
	{
		int index = 0;
		String name = null;
		Color color = null;
		int value   = -1;
			
		ColorObject(int index, String name, Color color)
		{
			this.index = index;
			this.name = name;
			this.color = color;
		}
			
		void runColorChooser()
		{
			color = getColor(color);
			
			switch (index)
			{
			case 1: Prefs.gui_seq_color_text = color; break;
			case 2: Prefs.gui_seq_color_a = color; break;
			case 3: Prefs.gui_seq_color_c = color; break;
			case 4: Prefs.gui_seq_color_g = color; break;
			case 5: Prefs.gui_seq_color_t = color; break;
			case 6: Prefs.gui_seq_color_gpst = color; break;
			case 7: Prefs.gui_seq_color_hkr = color; break;
			case 8: Prefs.gui_seq_color_fwy = color; break;
			case 9: Prefs.gui_seq_color_ilmv = color; break;
			case 10: Prefs.gui_seq_color_gaps = color; break;
			case 11: Prefs.gui_seq_highlight = color; break;
			case 12: Prefs.gui_graph_threshold = color; break;
			case 13: Prefs.gui_graph_window = color; break;
			case 14: Prefs.gui_cardle_line = color; break;
			case 15: Prefs.gui_graph_background = color; break;
			case 16: Prefs.gui_histo_background = color; break;
			}
			
			winMain.menuViewDisplaySettings(true);
			winMain.ovDialog.createImage();
		}
		
		Color getColor(Color old)
		{
			Color newColor = JColorChooser.showDialog(winMain,
				Text.GuiDiag.getString("DisplaySettingsDialog.gui40"), old);
	
			if (newColor != null)
				return newColor;
			else
				return old;
		}
		
		public String toString()
		{
			return name;
		}
	}
}
