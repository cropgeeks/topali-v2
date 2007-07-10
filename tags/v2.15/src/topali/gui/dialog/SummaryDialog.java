// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import pal.alignment.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;

import doe.*;

public class SummaryDialog extends JDialog implements ActionListener, Runnable
{
	private AlignmentData data;
	private SequenceSet ss;
	
	private JButton bClose, bClipboard, bHelp;
	private JTextArea text;
	
	public SummaryDialog(JFrame parent, AlignmentData data)
	{
		super(parent, Text.GuiDiag.getString("SummaryDialog.gui01"), true);
		this.data = data;
		ss = data.getSequenceSet();
				
		// Is it even worth progressing?		
		if (ss.getSize() < 3)
		{
			MsgBox.msg(Text.GuiDiag.getString("SummaryDialog.err01"), MsgBox.ERR);
			return;
		}
		
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				startThread();
			}
		});
		
		add(createControls());
		getRootPane().setDefaultButton(bClose);
		Utils.addCloseHandler(this, bClose);;
		
		setSize(500, 375);
		setResizable(false);
		setLocationRelativeTo(parent);
		setVisible(true);
	}
	
	private void startThread()
	{
		new Thread(this).start();
	}
	
	private JPanel createControls()
	{
		bClose = new JButton(Text.Gui.getString("close"));
		bClose.addActionListener(this);
		
		bClipboard = new JButton(Text.Gui.getString("clipboard_1"));
		bClipboard.addActionListener(this);
		
		bHelp = TOPALiHelp.getHelpButton("summary_info");
		
		text = new JTextArea(Text.GuiDiag.getString("SummaryDialog.gui02"));
		Utils.setTextAreaDefaults(text);
		
		JScrollPane sp = new JScrollPane(text);
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		p1.add(sp);
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
		p2.add(bClose);
		p2.add(bClipboard);		
		p2.add(bHelp);
		
		JPanel p3 = new JPanel(new BorderLayout());
		p3.add(p1, BorderLayout.CENTER);
		add(p2, BorderLayout.SOUTH);
		
		return p3;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClose)
			setVisible(false);
		
		else if (e.getSource() == bClipboard)
			Utils.copyToClipboard(text.getText());
	}
	
	public void run()
	{
		String summaryInfo = getSummaryInformation();
		text.setText(summaryInfo);
	}
	
	// Returns a string formatted with output
	public String getSummaryInformation()
	{
		SimpleAlignment alignment = ss.getAlignment(true);
		
		// Basic information
		double[] results = calculatePhyIS(ss.getSequencesArray(ss.getAllSequences()));
		
		Object[] str = new Object[6];
		str[0] = data.name;
		str[1] = "" + alignment.getLength();
		str[2] = "" + alignment.getIdCount();
		str[3] = alignment.getDataType().getDescription();
		str[4] = "" + (int) results[0];
		str[5] = results[1];
		
		String summaryInfo = Text.format(
			Text.GuiDiag.getString("SummaryDialog.gui03"), str);
		
		
		// Duplicates information
		String duplicates = doDuplicateCheck();
		if (duplicates != null)
			summaryInfo += Text.format(
				Text.GuiDiag.getString("SummaryDialog.gui04"), duplicates);
		else
			summaryInfo += Text.GuiDiag.getString("SummaryDialog.gui05");
		
		
		// Additional DNA-specific information
		if (ss.isDNA())
		{
			// Do we need to estimate parameters?
			if (ss.hasParametersEstimated() == false)
				SequenceSetUtils.estimateParameters(ss);
			
			int[] values = getDNACharCount();
			float total = ss.getSize() * ss.getLength();
			
			str = new Object[9];
			for (int i = 0; i < values.length; i++)
				str[i] = values[i]/total * 100;
				
			str[5] = ss.getParams().getAvgDistance();
			str[6] = ss.getParams().getTRatio();
			str[7] = ss.getParams().getAlpha();
			str[8] = ss.getParams().getKappa();
			
			summaryInfo += Text.format(
				Text.GuiDiag.getString("SummaryDialog.gui06"), str);
		}
		
		return summaryInfo;
	}
	
	private int[] getDNACharCount()
	{
		int seqLength = ss.getLength();
		
		int[] values = new int[5];
		
		// For each sequence
		for (Sequence seq: ss.getSequences())
		{
			// For each character in that sequence
			for (int j = 0; j < seqLength; j++)
			{
				switch (seq.getBuffer().charAt(j))
				{
					case 'A' : values[0]++; break;
					case 'C' : values[1]++; break;
					case 'G' : values[2]++; break;
					case 'T' : values[3]++; break;
					case 'U' : values[3]++; break;
					default  : values[4]++; break;
				}
			}
		}
				
		return values;
	}
	
	private double[] calculatePhyIS(Sequence[] sequences)
	{
		int length = sequences[0].getBuffer().length();		
		int num = 0;
		
		// Scan every column
		for (int pos = 0; pos < length; pos++)
		{
			boolean PhyIS = false;
			Hashtable<Character,Integer> map = new Hashtable<Character,Integer>();
			
			// Scan every value in this column
			for (int seq = 0; seq < sequences.length; seq++)
			{
				char c = sequences[seq].getBuffer().charAt(pos);
				
				Integer count = map.get(c);
				if (count == null)
					map.put(c, 1);
				else
					map.put(c, count + 1);
			}
			
			if (map.size() == 2)
			{
				// What were the two characters?
				Enumeration values = map.elements();
				int char1 = ((Integer) values.nextElement()).intValue();
				int char2 = ((Integer) values.nextElement()).intValue();
				
				if (char1 > 1 && char2 > 1)
					PhyIS = true;
			}			
			else if (map.size() > 2)
				PhyIS = true;
				
			if (PhyIS)
				num++;
		}
				
		double[] results = new double[2];
		results[0] = num;
		results[1] = num/((float)length) * 100;
		
		return results;
	}
		
	// Searches for duplicate groups within the alignment.
	// Each time a sequence is found that matches another sequence, it is added
	// to a unique group for those sequences.
	private String doDuplicateCheck()
	{
		StringBuffer buf = new StringBuffer(1000);
		LinkedList<SequenceList> duplicates = new LinkedList<SequenceList>();
		
		// Do the searching (this is similar to the selectUnique() code in
		// SequencePanel.java
		for (Sequence s1: ss.getSequences())
		{
			for (Sequence s2: ss.getSequences())
			{
				if (s1 == s2)
					continue;
				
				if (s1.isEqualTo(s2))
				{
					addToList(s1, duplicates);
					break;
				}
			}
		}
		
		// Create the output (scan the lists, print their results)
		for (SequenceList list: duplicates)
		{
			list.printOutput(buf);
			buf.append("\n");
		}
		
		return (buf.length() > 0) ? buf.toString() : null;
	}
	
	private void addToList(Sequence seq, LinkedList<SequenceList> duplicates)
	{
		// Search through all the existing groups to see if a match can be found
		for (SequenceList list: duplicates)
		{
			if (list.hasMatch(seq))
			{
				list.addSequence(seq);
				return;
			}
		}
				
		// If the sequence wasn't added to an existing group, create a new
		// one for it and add it to that instead
		SequenceList newList = new SequenceList();
		newList.addSequence(seq);
		duplicates.addLast(newList);
	}
	
	/* Stores a list of sequences that are all identical. */
	static class SequenceList
	{
		LinkedList<Sequence> sequences = new LinkedList<Sequence>();
		
		// Does this list contain a sequence that is equal to s2
		boolean hasMatch(Sequence s2)
			{ return sequences.get(0).isEqualTo(s2); }
		
		void addSequence(Sequence seq)
			{ sequences.add(seq); }
		
		void printOutput(StringBuffer buffer)
		{
			for (Sequence seq: sequences)
				buffer.append("\n    " + seq.name);
		}
	}
}