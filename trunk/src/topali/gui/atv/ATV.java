// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.atv;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.UIManager;

import org.forester.atv.ATVapplicationFrame;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.phylogeny.parsers.nhx.NHXParser;

public class ATV extends Thread
{
	Phylogeny phyl;

	String title;

	WindowListener listener;

	public ATV(String tree, String title, WindowListener listener)
	{
		this.title = title;
		this.listener = listener;
		PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
		try
		{
			phyl = factory.create(tree, new NHXParser())[0];
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		String config_filename = "res/ATVConfig.conf";
		ATVapplicationFrame atvframe = new ATVapplicationFrame(phyl,
				config_filename, title);
		
		//ATV will change the LnF, set it back to office2003
		try
		{
			UIManager.setLookAndFeel("org.fife.plaf.Office2003.Office2003LookAndFeel");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		atvframe.addWindowListener(listener);
	}

	public String getCodeMLHypothesis()
	{
		String clip = getClipboardContent();
		if (clip.matches("\\(.+\\)\\;?"))
		{
			String hypo = clip.replaceAll("_#", " #");
			return hypo;
		}
		return null;
	}

	private String getClipboardContent()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transfer = sysClip.getContents(null);
		try
		{
			String data = (String) transfer
					.getTransferData(DataFlavor.stringFlavor);
			return data;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args)
	{
		String tree = "(U68496:0.0027390,U68497:0.0275900,((U68498:0.0315980,(U68501:0.0709420,((U68502:0.0777510,(U68503:0.0440620,U68508:0.1610830):0.0208750):0.0005290,(((U68504:0.0007310,U68506:0.0168530):0.0501390,U68507:0.0000000):0.0014570,U68505:0.0148880):0.0415900):0.0195660):0.0208720):0.0160180,(U68499:0.0152300,U68500:0.0463450):0.0349010):0.0578500);";
		ATV atv = new ATV(tree, "TOPALi Test", null);
		atv.run();
	}
}
