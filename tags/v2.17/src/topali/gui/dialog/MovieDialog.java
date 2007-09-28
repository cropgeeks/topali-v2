// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import pal.alignment.SimpleAlignment;
import pal.gui.*;
import pal.tree.Tree;
import topali.analyses.*;
import topali.data.SequenceSet;
import topali.gui.*;
import topali.var.Utils;
import doe.DoeLayout;

public class MovieDialog extends JDialog implements ActionListener,
		ChangeListener
{
	private static final int STOPPED = 0;

	private static final int PLAYING = 1;

	private Tree tree = null;

	private TreeCanvas canvas = null;

	private WindowCanvas winCan = null;

	private TreePainter painter = null;

	private SequenceSet ss = null;

	// Navigation controls
	private JButton bPlay, bStop, bRewind, bSBack, bSFore;

	private int playState = STOPPED;

	private JSlider slider;

	private JLabel sliderLabel;

	private JPanel navPanel, canPanel;

	private AdvancedPanel advancedPanel;

	private JButton bClose, bHelp;

	private JCheckBox checkCurrent, checkCircular;

	private int frames;

	private int currentFrame = 1;

	private int sNuc, eNuc;

	public MovieDialog(WinMain winMain, SequenceSet ss)
	{
		super(winMain, Text.GuiDiag.getString("MovieDialog.gui01"), true);
		this.ss = ss;

		// Create the controls
		setFrames();
		createControls();

		// Add window handlers
		Utils.addCloseHandler(this, bClose);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				onClose();
			}
		});

		// Lay out the dialog
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(canPanel, BorderLayout.CENTER);
		p1.add(navPanel, BorderLayout.SOUTH);

		getContentPane().add(p1, BorderLayout.CENTER);
		getContentPane().add(advancedPanel, BorderLayout.EAST);

		setSize(Prefs.gui_movie_width, Prefs.gui_movie_height);
		if (Prefs.gui_movie_x == -1)
			setLocationRelativeTo(winMain);
		else
			setLocation(Prefs.gui_movie_x, Prefs.gui_movie_y);

		setVisible(true);
	}

	private void createControls()
	{
		canPanel = new JPanel(new BorderLayout());
		canPanel.add(canvas = new TreeCanvas(), BorderLayout.CENTER);
		canPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(10, 10, 5, 10), BorderFactory
				.createLoweredBevelBorder()));

		JPanel winPanel = new JPanel(new BorderLayout());
		winPanel.add(winCan = new WindowCanvas(), BorderLayout.CENTER);
		winPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(5, 0, 5, 0), BorderFactory
				.createLoweredBevelBorder()));

		checkCurrent = new JCheckBox(Text.GuiDiag
				.getString("MovieDialog.gui02"), Prefs.gui_movie_current);
		checkCurrent.addActionListener(this);
		checkCurrent.setMnemonic(KeyEvent.VK_O);
		checkCurrent
				.setToolTipText(Text.GuiDiag.getString("MovieDialog.gui03"));

		checkCircular = new JCheckBox(Text.GuiDiag
				.getString("MovieDialog.gui04"), Prefs.gui_movie_circular);
		checkCircular.addActionListener(this);
		checkCircular.setMnemonic(KeyEvent.VK_C);
		checkCircular.setToolTipText(Text.GuiDiag
				.getString("MovieDialog.gui05"));

		sliderLabel = new JLabel("", SwingConstants.CENTER);

		bPlay = (JButton) WinMainToolBar.getButton(false, null, "movie01",
				Icons.PLAYER_PLAY, null);
		bStop = (JButton) WinMainToolBar.getButton(false, null, "movie02",
				Icons.PLAYER_STOP, null);
		bRewind = (JButton) WinMainToolBar.getButton(false, null, "movie03",
				Icons.PLAYER_REW, null);
		bSBack = (JButton) WinMainToolBar.getButton(false, null, "movie04",
				Icons.PLAYER_START, null);
		bSFore = (JButton) WinMainToolBar.getButton(false, null, "movie05",
				Icons.PLAYER_END, null);

		bPlay.addActionListener(this);
		// bPlay.setBorderPainted(false);
		bStop.addActionListener(this);
		bRewind.addActionListener(this);
		bSBack.addActionListener(this);
		bSFore.addActionListener(this);

		bStop.setEnabled(false);
		bSBack.setEnabled(false);

		bClose = new JButton(Text.Gui.getString("close"));
		bClose.addActionListener(this);

		bHelp = TOPALiHelp.getHelpButton("movie_dialog");

		slider = new JSlider(1, frames);
		slider.setToolTipText(Text.GuiDiag.getString("MovieDialog.gui06"));
		slider.addChangeListener(this);
		slider.setValue(1);

		// JToolBar toolbar = new JToolBar();
		// toolbar.setFloatable(false);
		// toolbar.setOpaque(true);
		// toolbar.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

		toolbar.add(bRewind);
		// toolbar.add(new JLabel(" "));
		toolbar.add(bSBack);
		// toolbar.add(new JLabel(" "));
		toolbar.add(bStop);
		// toolbar.add(new JLabel(" "));
		toolbar.add(bPlay);
		// toolbar.add(new JLabel(" "));
		toolbar.add(bSFore);

		JPanel p2 = new JPanel(new BorderLayout());

		p2.add(winPanel, BorderLayout.NORTH);
		p2.add(slider, BorderLayout.CENTER);
		p2.add(sliderLabel, BorderLayout.SOUTH);
		p2.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		JPanel p3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p3.add(toolbar);

		JPanel chkPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		chkPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 10, 5));
		chkPanel.add(checkCurrent);
		chkPanel.add(checkCircular);
		JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p4.add(chkPanel);

		navPanel = new JPanel(new BorderLayout());
		navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		navPanel.add(p4, BorderLayout.NORTH);
		navPanel.add(p2, BorderLayout.CENTER);
		navPanel.add(p3, BorderLayout.SOUTH);

		advancedPanel = new AdvancedPanel();
	}

	private void onClose()
	{
		// Stop any currently running animation
		playState = STOPPED;

		Prefs.gui_movie_x = getLocation().x;
		Prefs.gui_movie_y = getLocation().y;
		Prefs.gui_movie_width = getSize().width;
		Prefs.gui_movie_height = getSize().height;

		setVisible(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bPlay)
			onPlay();

		else if (e.getSource() == bStop)
			onStop();

		else if (e.getSource() == bRewind)
			slider.setValue(currentFrame = 1);

		else if (e.getSource() == bSBack)
			slider.setValue(--currentFrame);

		else if (e.getSource() == bSFore)
			slider.setValue(++currentFrame);

		else if (e.getSource() == bClose)
			onClose();

		else if (e.getSource() == checkCurrent)
		{
			Prefs.gui_movie_current = checkCurrent.isSelected();
			createTree(sNuc, eNuc);
		}

		else if (e.getSource() == checkCircular)
		{
			Prefs.gui_movie_circular = checkCircular.isSelected();
			createTree(sNuc, eNuc);
		}
	}

	private void onPlay()
	{
		playState = PLAYING;
		bPlay.setEnabled(false);
		bStop.setEnabled(true);
		advancedPanel.setStates(false);

		new Thread(new TreePlayer()).start();
	}

	private void onStop()
	{
		playState = STOPPED;
		bStop.setEnabled(false);
		bPlay.setEnabled(true);
		advancedPanel.setStates(true);
	}

	public void stateChanged(ChangeEvent e)
	{
		currentFrame = slider.getValue();

		// Button states
		if (currentFrame == frames)
			bSFore.setEnabled(false);
		else if (!bSFore.isEnabled())
			bSFore.setEnabled(true);

		if (currentFrame == 1)
			bSBack.setEnabled(false);
		else if (!bSBack.isEnabled())
			bSBack.setEnabled(true);

		// Create the tree for this window position
		sNuc = ((currentFrame - 1) * Prefs.gui_movie_step) + 1;
		eNuc = sNuc + Prefs.gui_movie_window - 1;
		try
		{
			createTree(sNuc, eNuc);
		} catch (Exception e1)
		{
			currentFrame--;
		}

		Object[] args =
		{ slider.getValue(), frames, sNuc, eNuc };
		sliderLabel.setText(Text.format(Text.GuiDiag
				.getString("MovieDialog.gui07"), args));
	}

	void createTree(int start, int end)
	{
		// Work out which sequences to use
		int[] indices = null;
		if (Prefs.gui_movie_current)
			indices = ss.getSelectedSequences();
		else
			indices = ss.getAllSequences();

		// Can only draw trees with at least 3 sequences
		if (indices.length < 3)
		{
			clearTree();
			return;
		}

		// Create a PAL alignment that can be used to create this tree
		SimpleAlignment alignment = ss.getAlignment(indices, start, end, false);
		TreeCreator tc = new TreeCreator(alignment, ss.isDNA(), true, false);
		
		tree = tc.getTree();
		// tree = tc.createTree(TreeCreator.JC_NJ);

		if (tree != null)
		{
			if (Prefs.gui_movie_circular)
				painter = new TreePainterCircular(tree, "", false);
			else
				painter = new TreePainterNormal(tree, "", false);
			painter.setPenWidth(1);
		} else
			painter = null;

		canvas.repaint();
		winCan.repaint();
	}

	private void setFrames()
	{
		// Check settings for validity
		if (Prefs.gui_movie_window < 2
				|| Prefs.gui_movie_window > ss.getLength())
			Prefs.gui_movie_window = 2;
		if (Prefs.gui_movie_step < 1 || Prefs.gui_movie_step > ss.getLength())
			Prefs.gui_movie_step = 1;

		frames = ((ss.getLength() - Prefs.gui_movie_window) / Prefs.gui_movie_step) + 1;

		if (slider != null)
		{
			slider.setMaximum(frames);
			slider.setValue(1);
		}
	}

	class TreePlayer implements Runnable
	{
		public void run()
		{
			// Run the animation loop
			while (currentFrame <= frames && playState != STOPPED)
			{
				try
				{
					Thread.sleep(Prefs.gui_movie_delay);
				} catch (InterruptedException e)
				{
				}

				setGUI(currentFrame);
				currentFrame++;
			}

			// Once finished...
			stopButton();
		}

		private void stopButton()
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					onStop();
				}
			};

			SwingUtilities.invokeLater(r);
		}

		private void setGUI(final int value)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					slider.setValue(value);
				}
			};

			try
			{
				SwingUtilities.invokeAndWait(r);
			} catch (Exception e)
			{
			}
		}
	}

	void clearTree()
	{
		painter = null;
		canvas.repaint();
	}

	class TreeCanvas extends JPanel
	{
		TreeCanvas()
		{
			setBackground(Color.white);
			setPreferredSize(new Dimension(400, 300));
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			if (painter != null)
			{
				if (Prefs.gui_tree_unique_cols)
				{
					painter.setColouriser(ss
							.getNameColouriser(Prefs.gui_color_seed));
					painter.setUsingColor(false);
				}

				painter.paint(g, getSize().width, getSize().height);
			}
		}
	}

	class WindowCanvas extends JPanel
	{
		int p0 = 0, pm;

		float x0 = 1, xm = ss.getLength();

		WindowCanvas()
		{
			setBackground(Prefs.gui_graph_background.darker().darker());
			setToolTipText(Text.GuiDiag.getString("MovieDialog.gui08"));
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			pm = getSize().width;

			int winWidth = getPosition(Prefs.gui_movie_window);
			int x1 = getPosition(sNuc);

			g.setColor(Prefs.gui_graph_background);
			g.fillRect(x1, 0, winWidth > 1 ? winWidth : 1, getSize().height);
		}

		private int getPosition(int value)
		{
			return ((int) ((pm - p0) * ((value - x0) / (xm - x0)) + p0));
		}
	}

	class AdvancedPanel extends JPanel implements ChangeListener
	{
		private SpinnerNumberModel winModel, stepModel, delayModel;

		private JSpinner winSpin, stepSpin, delaySpin;

		private JLabel label1, label2, label3;

		AdvancedPanel()
		{
			winModel = new SpinnerNumberModel(Prefs.gui_movie_window, 2, ss
					.getLength(), 1);
			stepModel = new SpinnerNumberModel(Prefs.gui_movie_step, 1, ss
					.getLength(), 1);
			delayModel = new SpinnerNumberModel(Prefs.gui_movie_delay, 5, 5000,
					5);
			winSpin = new JSpinner(winModel);
			winSpin.addChangeListener(this);
			stepSpin = new JSpinner(stepModel);
			stepSpin.addChangeListener(this);
			delaySpin = new JSpinner(delayModel);
			delaySpin.addChangeListener(this);

			((JSpinner.NumberEditor) winSpin.getEditor())
					.getTextField()
					.setToolTipText(Text.GuiDiag.getString("MovieDialog.gui12"));
			((JSpinner.NumberEditor) stepSpin.getEditor())
					.getTextField()
					.setToolTipText(Text.GuiDiag.getString("MovieDialog.gui13"));
			((JSpinner.NumberEditor) delaySpin.getEditor())
					.getTextField()
					.setToolTipText(Text.GuiDiag.getString("MovieDialog.gui14"));

			label1 = new JLabel(Text.GuiDiag.getString("MovieDialog.gui09"));
			label1.setDisplayedMnemonic(KeyEvent.VK_S);
			label1.setLabelFor(((JSpinner.NumberEditor) stepSpin.getEditor())
					.getTextField());
			label2 = new JLabel(Text.GuiDiag.getString("MovieDialog.gui10"));
			label2.setDisplayedMnemonic(KeyEvent.VK_W);
			label2.setLabelFor(((JSpinner.NumberEditor) winSpin.getEditor())
					.getTextField());
			label3 = new JLabel(Text.GuiDiag.getString("MovieDialog.gui11"));
			label3.setDisplayedMnemonic(KeyEvent.VK_D);
			label3.setLabelFor(((JSpinner.NumberEditor) delaySpin.getEditor())
					.getTextField());

			DoeLayout layout = new DoeLayout();
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
			add(layout.getPanel(), BorderLayout.NORTH);

			layout.add(bClose, 0, 0, 1, 1, new Insets(5, 5, 5, 5));
			layout.add(bHelp, 0, 1, 1, 1, new Insets(0, 5, 5, 5));
			layout.add(new JPanel(), 0, 2, 1, 1, new Insets(5, 5, 5, 5));
			layout.add(label1, 0, 3, 1, 1, new Insets(5, 5, 0, 5));
			layout.add(stepSpin, 0, 4, 1, 1, new Insets(2, 5, 5, 5));
			layout.add(label2, 0, 5, 1, 1, new Insets(5, 5, 0, 5));
			layout.add(winSpin, 0, 6, 1, 1, new Insets(2, 5, 5, 5));
			layout.add(label3, 0, 7, 1, 1, new Insets(5, 5, 0, 5));
			layout.add(delaySpin, 0, 8, 1, 1, new Insets(2, 5, 5, 5));
		}

		void setStates(boolean state)
		{
			stepSpin.setEnabled(state);
			winSpin.setEnabled(state);
		}

		public void stateChanged(ChangeEvent e)
		{
			if (e.getSource() == winSpin)
				Prefs.gui_movie_window = winModel.getNumber().intValue();
			else if (e.getSource() == stepSpin)
				Prefs.gui_movie_step = stepModel.getNumber().intValue();
			else if (e.getSource() == delaySpin)
				Prefs.gui_movie_delay = delayModel.getNumber().intValue();

			if (e.getSource() != delaySpin)
				setFrames();
		}
	}
}