// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
//import sun.print.resources.serviceui;
import topali.data.*;
import topali.data.annotations.*;
import topali.data.models.*;
import topali.fileio.AlignmentLoadException;
import topali.gui.TOPALi;
import topali.i18n.Text;

public class JoinAlignmentsDialog extends JDialog implements ActionListener {

	List<AlignmentData> data;

	JButton bOk, bCancel;
	JCheckBox selAll;
	List<JCheckBox> boxes;

	public JoinAlignmentsDialog(Frame owner, boolean modal, List<AlignmentData> data) {
		super(owner, modal);
		setTitle(Text.get("JoinAlignmentsDialog.0"));
		this.data = data;
		init();
		setPreferredSize(new Dimension(300,300));
	}

	private void init() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel lab = new JLabel(Text.get("JoinAlignmentsDialog.1"));
		lab.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		mainPanel.add(lab, BorderLayout.NORTH);

		JPanel centPanel = new JPanel(new BorderLayout());
		JPanel selPanel = getSelectionPanel();
		centPanel.add(new JScrollPane(selPanel), BorderLayout.CENTER);
		JPanel selAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		selAll = new JCheckBox(Text.get("JoinAlignmentsDialog.2"));
		selAll.setSelected(true);
		selAll.addActionListener(this);
		selAllPanel.add(selAll);
		centPanel.add(selAllPanel, BorderLayout.SOUTH);
		mainPanel.add(centPanel, BorderLayout.CENTER);


		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bOk = new JButton(Text.get("ok"));
		bOk.addActionListener(this);
		buttonPanel.add(bOk);
		bCancel = new JButton(Text.get("cancel"));
		bCancel.addActionListener(this);
		buttonPanel.add(bCancel);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		this.add(mainPanel);
	}

	private JPanel getSelectionPanel() {
		//Actually faking a kind of a JList here like the JobsPanel

		JPanel p = new JPanel();
		p.setBackground(Color.WHITE);
		BoxLayout lay = new BoxLayout(p, BoxLayout.Y_AXIS);
		p.setLayout(lay);

		boxes = new ArrayList<JCheckBox>();

		for(AlignmentData d : data) {
			JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
				//don't stretch this panel
				@Override
				public Dimension getMaximumSize()
				{
					return new Dimension(super.getMaximumSize().width,
							getPreferredSize().height);
				}
				@Override
				public Dimension getMinimumSize()
				{
					return new Dimension(super.getMaximumSize().width,
							getPreferredSize().height);
				}
			};
			p2.setBackground(Color.WHITE);
			final JCheckBox tick = new JCheckBox();
			tick.setBackground(Color.WHITE);
			tick.setSelected(true);
			tick.addActionListener(this);
			boxes.add(tick);
			p2.add(tick);
			JLabel lab = new JLabel(d.getName());
			lab.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					tick.setSelected(!tick.isSelected());
					checkTheBoxes();
				}
			});
			lab.setBackground(Color.WHITE);
			p2.add(lab);
			p.add(p2);
		}
		p.add(Box.createVerticalGlue());

		return p;
	}

	private void checkTheBoxes() {
		for(JCheckBox box : boxes) {
			if(box.isSelected()) {
				bOk.setEnabled(true);
				return;
			}
		}
		bOk.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==bOk) {

			AlignmentData joint = joinAlignments();
			TOPALi.winMain.addNewAlignmentData(joint);

			setVisible(false);
		}
		else if(e.getSource()==bCancel) {
			setVisible(false);
		}
		else if(e.getSource()==selAll) {
			boolean sel = selAll.isSelected();
			for(JCheckBox check : boxes)
				check.setSelected(sel);
			checkTheBoxes();
		}

		else {
			checkTheBoxes();
		}
	}

	private AlignmentData joinAlignments() {
		//Be aware of references!

		List<Sequence> seqs = new ArrayList<Sequence>();
		List<Annotation> annos = new ArrayList<Annotation>();

		//Find the first alignments which is selected
		int index = -1;
		for(int i=0; i<boxes.size(); i++) {
			if(boxes.get(i).isSelected()) {
				index = i;
				break;
			}
		}

		//get sequences and annotation from the first alignment
		AlignmentData data0 = data.get(index);
		for(Sequence seq : data0.getSequenceSet().getSequences()) {
			seqs.add(new Sequence(seq));
		}
		for(Annotation anno : data0.getAnnotations().getAnnotations()) {
			if(anno instanceof PartitionAnnotation) {
				annos.add(new PartitionAnnotation((PartitionAnnotation)anno));
			}
			else if (anno instanceof CodingRegionAnnotation) {
				annos.add(new CodingRegionAnnotation((CodingRegionAnnotation)anno));
			}
			else if (anno instanceof SecStructureAnnotation) {
				annos.add(new SecStructureAnnotation((SecStructureAnnotation)anno));
			}
		}
		PartitionAnnotation an = new PartitionAnnotation(1, seqs.get(0).getLength());
		an.setSeqType(data0.getSequenceSet().getProps().getType());
		an.setComment(data0.getName());
		an.setModel(ModelManager.getInstance().copy(data0.getSequenceSet().getProps().getModel()));
		annos.add(an);

		//now try to append the rest
		List<AlignmentData> failedAlignments = new ArrayList<AlignmentData>();
		int annoOffset = seqs.get(0).getLength();
		for(int i=index+1; i<boxes.size(); i++) {
			if(!boxes.get(i).isSelected())
				continue;

			//first check if the sequence names matches
			AlignmentData datax = data.get(i);
			boolean failed = false;
			for(Sequence seq : seqs) {
				Sequence seq2 = datax.getSequenceSet().getSequence(seq.getName());
				if(seq2==null) {
					failedAlignments.add(datax);
					failed=true;
					break;
				}
			}
			if(failed)
				continue;

			//append the sequences
			int seqLength = 0;
			for(Sequence seq : seqs) {
				Sequence seq2 = datax.getSequenceSet().getSequence(seq.getName());
				seqLength = seq2.getLength();
				seq.getBuffer().append(seq2.getSequence());
			}

			//copy the annotations (and adapt their positions)
			for(Annotation anno : datax.getAnnotations().getAnnotations()) {
				Annotation newAnno = null;

				if(anno instanceof PartitionAnnotation) {
					newAnno = new PartitionAnnotation((PartitionAnnotation)anno);
				}
				else if (anno instanceof CodingRegionAnnotation) {
					newAnno = new CodingRegionAnnotation((CodingRegionAnnotation)anno);
				}
				else if (anno instanceof SecStructureAnnotation) {
					newAnno = new SecStructureAnnotation((SecStructureAnnotation)anno);
				}

				newAnno.setStart(newAnno.getStart()+annoOffset);
				newAnno.setEnd(newAnno.getEnd()+annoOffset);
				annos.add(newAnno);
			}

			//add a new PartitionAnnotation which covers this alignment
			PartitionAnnotation anno = new PartitionAnnotation(annoOffset+1, annoOffset+seqLength);
			anno.setSeqType(datax.getSequenceSet().getProps().getType());
			anno.setComment(datax.getName());
			Model model = datax.getSequenceSet().getProps().getModel();
			if(model!=null)
				anno.setModel(ModelManager.getInstance().copy(model));
			annos.add(anno);

			annoOffset += seqLength;
		}

		//create a new AlignmentData object
		AlignmentData newAlign = new AlignmentData();
		SequenceSet ss = new SequenceSet();
		for(Sequence seq : seqs) {
			ss.addSequence(seq);
		}
		try {
			ss.checkValidity();
		}
		catch (AlignmentLoadException e1) {
			e1.printStackTrace();
		}
		newAlign.setSequenceSet(ss);
		AnnotationList anlist = new AnnotationList();
		for(Annotation anno : annos)
			anlist.add(anno);
		newAlign.setAnnotations(anlist);

		newAlign.setName("Joined Alignment");

		if(failedAlignments.size()>0) {
			String msg = "<html>Not all alignments could be joined together,<br>" +
					"because not all sequence names are matching.<br>" +
					"The following alingments could not be joined:<br><br>";
			for(int i=0; i<failedAlignments.size(); i++) {
				msg += failedAlignments.get(i).getName()+"<br>";
			}
			msg += "<br></html>";
			JOptionPane.showMessageDialog(this, new JLabel(msg), "Warning", JOptionPane.WARNING_MESSAGE);
		}

		return newAlign;
	}

}
