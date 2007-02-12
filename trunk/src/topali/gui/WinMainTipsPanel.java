// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import doe.GradientPanel;

public class WinMainTipsPanel extends JPanel implements ActionListener
{
	public static final int TIPS_NONE = 0;

	public static final int TIPS_ALN = 1;

	public static final int TIPS_TRE = 2;

	public static final int TIPS_DSS = 3;

	public static final int TIPS_JOB = 4;

	private static ResourceBundle tips;

	private static TipsText tips_aln, tips_tre, tips_dss, tips_job;

	private static TipsText currentTips;

	private static JButton bBack, bNext;

	private static JTextArea text;

	private static JScrollPane sp;

	public WinMainTipsPanel()
	{
		createTips();

		setPreferredSize(new Dimension(100, 150));

		text = new JTextArea("");
		text.setWrapStyleWord(true);
		text.setLineWrap(true);
		text.setMargin(new Insets(5, 2, 5, 2));
		text.setEditable(false);
		text.setFont(new Font("SansSerif", Font.PLAIN, 10));

		sp = new JScrollPane(text);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		setLayout(new BorderLayout());
		TipsGradientPanel tPanel = new TipsGradientPanel();
		tPanel.setStyle(TipsGradientPanel.OFFICE2003);
		add(tPanel, BorderLayout.NORTH);
		add(sp);
	}

	// Creates a JButton that is setup for use on this panel's toolbar
	private JButton getButton(Icon icon1, Icon icon2, String tooltip)
	{
		JButton button = new JButton(icon1);
		button.setBorderPainted(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setContentAreaFilled(false);
		button.setRolloverIcon(icon2);
		button.addActionListener(this);
		button.setToolTipText(tooltip);
		button.setFocusPainted(false);

		return button;
	}

	private void createTips()
	{
		tips = ResourceBundle.getBundle("res.text.tips", Prefs.locale);

		tips_aln = new TipsText("aln");
		tips_tre = new TipsText("tre");
		tips_dss = new TipsText("dss");
		tips_job = new TipsText("job");
	}

	public static void setDisplayedTips(int tipType)
	{
		switch (tipType)
		{
		case TIPS_NONE:
			text.setText("");
			currentTips = null;
			break;

		case TIPS_ALN:
			setTips(tips_aln, 0);
			break;
		case TIPS_TRE:
			setTips(tips_tre, 0);
			break;
		case TIPS_DSS:
			setTips(tips_dss, 0);
			break;
		case TIPS_JOB:
			setTips(tips_job, 0);
			break;
		}
	}

	private static void setTips(TipsText tipsText, int action)
	{
		currentTips = tipsText;

		if (action == 1)
		{
			tipsText.index--;
			if (tipsText.index < 0)
				tipsText.index = tipsText.keys.size() - 1;
		}

		else if (action == 2)
		{
			tipsText.index++;
			if (tipsText.index >= tipsText.keys.size())
				tipsText.index = 0;
		}

		try
		{
			text.setText(tips.getString(tipsText.keys.get(tipsText.index)));
			text.setCaretPosition(0);
		} catch (Exception e)
		{
			text.setText("");
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bBack && currentTips != null)
			setTips(currentTips, 1);
		else if (e.getSource() == bNext && currentTips != null)
			setTips(currentTips, 2);
	}

	// Simple class that extends the standard Tools GradientPanel, adding two
	// buttons to provide back and next navigation
	class TipsGradientPanel extends GradientPanel
	{
		TipsGradientPanel()
		{
			super(Text.Gui.getString("WinMainTipsPanel.gui01"));

			bBack = getButton(Icons.BACK1, Icons.BACK2, Text.Gui
					.getString("WinMainTipsPanel.gui02"));
			bNext = getButton(Icons.NEXT1, Icons.NEXT2, Text.Gui
					.getString("WinMainTipsPanel.gui03"));

			setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			add(bBack);
			add(bNext);
		}
	}

	// Simple class to represent a set of tips, and the current index within
	// that set that is (or was) being displayed
	static class TipsText
	{
		Vector<String> keys = new Vector<String>();

		int index;

		TipsText(String key)
		{
			for (Enumeration e = tips.getKeys(); e.hasMoreElements();)
			{
				String str = (String) e.nextElement();
				if (str.startsWith(key))
					keys.add(str);
			}

			// Randomize the starting index
			index = (int) (Math.random() * (keys.size() - 1));
		}
	}
}