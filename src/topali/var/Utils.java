// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.log4j.Logger;

import pal.tree.*;
import topali.cluster.ClusterUtils;
import topali.data.*;
import topali.gui.*;
import doe.MsgBox;

/**
 * Contains misc methods that don't really fit in anywhere else (but are likely
 * to be used by multiple classes
 */
public class Utils
{
	static  Logger log = Logger.getLogger(Utils.class);

	/* Ensures the scratch directory exists */
	public static void createScratch()
	{
		Prefs.tmpDir.mkdirs();
	}

	/* Deletes all the files in the scratch directory */
	public static void emptyScratch()
	{
		log.info("Clearing scratch files");
		ClusterUtils.emptyDirectory(Prefs.tmpDir, true);
	}

	public static JPanel getButtonPanel(ActionListener al, JButton bOK,
			JButton bCancel, String helpString)
	{
		bOK.addActionListener(al);
		bCancel.addActionListener(al);
		JButton bHelp = TOPALiHelp.getHelpButton(helpString);

		JPanel p1 = new JPanel(new GridLayout(1, 3, 5, 5));
		p1.add(bOK);
		p1.add(bCancel);
		p1.add(bHelp);

		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p2.add(p1);

		return p2;
	}

	public static JPanel getButtonPanel(JButton bRun, JButton bCancel, JButton bDefault, JButton bHelp, JDialog parent, String help) {
		bRun.setText("Run");
		bRun.addActionListener((ActionListener)parent);
		bCancel.setText("Cancel");
		bCancel.addActionListener((ActionListener)parent);
		bDefault.setText("Defaults");
		bDefault.addActionListener((ActionListener)parent);
		bHelp = TOPALiHelp.getHelpButton(help);
		
		addCloseHandler(parent, bCancel);
		
		//JPanel p1 = new JPanel(new GridLayout(1, 4, 5, 5));
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bRun);
		p1.add(bDefault);
		p1.add(bCancel);
		p1.add(bHelp);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		return p1;
	}
	
	/**
	 * Forces the given Window to hide by mapping a KeyEvent for Escape onto the
	 * given JComponent.
	 */
	public static void addCloseHandler(final Window wnd, JComponent cmp)
	{
		AbstractAction closeAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				wnd.setVisible(false);
			}
		};

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		cmp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "close");
		cmp.getActionMap().put("close", closeAction);
	}

	public static void setTextAreaDefaults(JTextArea text)
	{
		text.setMargin(new Insets(5, 5, 5, 5));
		text.setEditable(false);
		text.setFont(new Font("Monospaced", Font.PLAIN, 11));
		text.setTabSize(4);
	}

	public static Color getColor(int number)
	{
		Random r = new Random(Prefs.gui_color_seed + 1253 + number);
		return new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
	}

	public static void copyToClipboard(String str)
	{
		StringSelection selection = new StringSelection(str);

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,
				null);

		MsgBox.msg(Text.Gui.getString("clipboard_2"), MsgBox.INF);
	}

	public static String getClipboardContent()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transfer = sysClip.getContents(null);

		try
		{
			Object o = transfer.getTransferData(DataFlavor.stringFlavor);
			if(o instanceof String)
				return (String)o;
			else
				return null;
		} catch (Exception e)
		{
			log.warn("Clipboard content is not a String", e);
			return null;
		}
	}

	public static boolean openBrowser(String url) throws URISyntaxException, IOException {
		URI uri = new URI(url);
		if(Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
				return true;
			}
		}
		return false;
	}

	public static void saveComponent(Component cmp, File file, int w, int h) throws IOException
	{
		// Store the container's current parent
		Container parent = cmp.getParent();

		// Create an image to draw upon
		BufferedImage bi = new BufferedImage(w, h,
				BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g2d = bi.createGraphics();

		// Redraw the component using the image

		SwingUtilities.paintComponent(g2d, cmp, new Container(), 0, 0, w, h);

		g2d.setColor(Color.black);
		g2d.drawRect(0, 0, w - 1, h - 1);

		// Save it...
		ImageIO.write(bi, "png", file);

		// Finally, reassociate the container with its parent
		if (parent != null)
			parent.add(cmp);
	}

	public static String getLocalPath()
	{
		if (Prefs.isWindows)
			return System.getProperty("user.dir") + "\\binaries\\";
		else if (Prefs.isMacOSX)
			return System.getProperty("user.dir") + "/binaries/src/";
		else
			return System.getProperty("user.dir") + "/binaries/src/";
	}

	/**
	 * Get a String representation of a map
	 * @param map The map
	 * @param keyValueDelim key-value delimiter
	 * @param delim delimiter between each key-value pairs
	 * @return
	 */
	public static String mapToString(Map<String, String> map, char keyValueDelim, char delim) {
		StringBuffer sb = new StringBuffer();
		Set<String> keys = map.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()) {
			String k = it.next();
			sb.append(k);
			sb.append(keyValueDelim);
			sb.append(map.get(k));
			if(it.hasNext())
				sb.append(delim);
		}
		return sb.toString();
	}

	/**
	 * Create a map of a String containing key-value pairs
	 * @param map Separated key-value pairs
	 * @param keyValueDelim key-value delimiter
	 * @param delim delimiter between each key-value pairs
	 * @return
	 */
	public static Map<String, String> stringToMap(String map, char keyValueDelim, char delim) {
		Map<String, String> result = new HashMap<String, String>();
		String[] tokens = map.split(Character.toString(delim));
		for(String token : tokens) {
			String[] tmp = token.split(Character.toString(keyValueDelim));
			result.put(tmp[0], tmp[1]);
		}
		return result;
	}

	/**
	 * Converts a float into a double array (as simple casting is not possible)
	 * @param data
	 * @return
	 */
	public static double[][] float2doubleArray(float[][] data) {
		double[][] ddata  = new double[0][0];
		if(data.length>0) {
			ddata = new double[data.length][data[0].length];
			for(int i=0; i<data.length; i++)
				for(int j=0; j<data[i].length; j++)
					ddata[i][j] = data[i][j];
		}
		return ddata;
	}

	/**
	 * Converts a double into a float array (as simple casting is not possible)
	 * @param data
	 * @return
	 */
	public static float[][] double2floatArray(double[][] data) {
		float[][] ddata  = new float[0][0];
		if(data.length>0) {
			ddata = new float[data.length][data[0].length];
			for(int i=0; i<data.length; i++)
				for(int j=0; j<data[i].length; j++)
					ddata[i][j] = (float)data[i][j];
		}
		return ddata;
	}

	public static byte[] readFile(URI uri) throws IOException {
		File inFile = new File(uri);
		FileInputStream fis = new FileInputStream(inFile);
		FileChannel inChannel = fis.getChannel();
		ByteBuffer buf = ByteBuffer.allocate((int)inChannel.size());
		inChannel.read(buf);
		inChannel.close();
		return buf.array();
	}

	public static String intArrayToString(int[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for(int i : array) {
			sb.append(i);
			sb.append(',');
		}
		sb.replace(sb.length()-1, sb.length()-1, "]");
		return sb.toString();
	}

//	public static void printMemUsage() {
//		long freeMem = Runtime.getRuntime().freeMemory();
//		long maxMem = Runtime.getRuntime().maxMemory();
//		System.out.println("MemUsage: "+((maxMem-freeMem)/1024/1024)+" MB\n");
//	}

	public static String midPointRoot(String tree) {
		try
		{
			PushbackReader pbread = new PushbackReader(new StringReader(tree));
			ReadTree t = new ReadTree(pbread);
			Tree t2 = TreeRooter.getMidpointRooted(t);
			return t2.toString();
		} catch (TreeParseException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getSafenameTree(String tree, SequenceSet ss) {
		Hashtable<String, String> lookup = new Hashtable<String, String>();
		for(Sequence seq : ss.getSequences()) {
			lookup.put(seq.name, seq.safeName);
		}
		
		for(String name : lookup.keySet()) {
			tree = tree.replaceAll(name, lookup.get(name));
		}
		return tree;
	}
	
	public static String getNameTree(String tree, SequenceSet ss) {
		Hashtable<String, String> lookup = new Hashtable<String, String>();
		for(Sequence seq : ss.getSequences()) {
			lookup.put(seq.safeName, seq.name);
		}
		
		for(String safename : lookup.keySet()) {
			tree = tree.replaceAll(safename, lookup.get(safename));
		}
		return tree;
	}
}
