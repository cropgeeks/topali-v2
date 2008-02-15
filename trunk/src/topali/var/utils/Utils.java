// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.utils;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.apache.log4j.Logger;
import topali.data.*;
import topali.gui.*;
import topali.i18n.Text;
import topali.var.SysPrefs;

/**
 * Contains misc methods that don't really fit in anywhere else (but are likely
 * to be used by multiple classes
 */
public class Utils {
    static Logger log = Logger.getLogger(Utils.class);

    public static final DecimalFormat d1 = new DecimalFormat("0.0");
    public static final DecimalFormat d2 = new DecimalFormat("0.00");
    public static final DecimalFormat d3 = new DecimalFormat("0.000");
    public static final DecimalFormat d4 = new DecimalFormat("0.0000");
    public static final DecimalFormat d5 = new DecimalFormat("0.00000");
    public static final DecimalFormat i = new DecimalFormat("###");
    public static final DecimalFormat i2 = new DecimalFormat("00");
    public static final DecimalFormat i3 = new DecimalFormat("000");
    public static final DecimalFormat i4 = new DecimalFormat("0000");

    /* Ensures the scratch directory exists */
    public static void createScratch() {
	SysPrefs.tmpDir.mkdirs();
    }

    public static JPanel getButtonPanel(ActionListener al, JButton bOK,
	    JButton bCancel, String helpString) {
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

    public static JPanel getButtonPanel(JButton bRun, JButton bCancel,
	    JButton bDefault, JButton bHelp, JDialog parent, String help) {
	bRun.setText(Text.get("run"));
	bRun.addActionListener((ActionListener) parent);
	bCancel.setText(Text.get("cancel"));
	bCancel.addActionListener((ActionListener) parent);
	bDefault.setText(Text.get("defaults"));
	bDefault.addActionListener((ActionListener) parent);
	if (help != null)
	    bHelp = TOPALiHelp.getHelpButton(help);

	addCloseHandler(parent, bCancel);

	JPanel p1 = new JPanel(new GridLayout(1, 4, 5, 5));
	// JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	p1.add(bRun);
	p1.add(bDefault);
	p1.add(bCancel);
	p1.add(bHelp);
	JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
	p2.add(p1);
	return p2;
    }

    /**
     * Forces the given Window to hide by mapping a KeyEvent for Escape onto the
     * given JComponent.
     */
    public static void addCloseHandler(final Window wnd, JComponent cmp) {
	AbstractAction closeAction = new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		wnd.setVisible(false);
	    }
	};

	KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	cmp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "close");
	cmp.getActionMap().put("close", closeAction);
    }

    public static void setTextAreaDefaults(JTextArea text) {
	text.setMargin(new Insets(5, 5, 5, 5));
	text.setEditable(false);
	text.setFont(new Font("Monospaced", Font.PLAIN, 11));
	text.setTabSize(4);
    }

    public static Color getColor(int number) {
	Random r = new Random(Prefs.gui_color_seed + 1253 + number);
	return new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
    }

    /**
     * Copies a certain String to the system's clipboard
     * @param str
     */
    public static void copyToClipboard(String str) {
	StringSelection selection = new StringSelection(str);

	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,
		null);
    }

    /**
     * Get the current content of the systems's clipboard (if it's text)
     * @return clipboard content or null if not supported or content is not a String
     */
    public static String getClipboardContent() {
	Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
	Transferable transfer = sysClip.getContents(null);

	try {
	    Object o = transfer.getTransferData(DataFlavor.stringFlavor);
	    if (o instanceof String)
		return (String) o;
	    else
		return null;
	} catch (Exception e) {
	    log.warn("Clipboard content is not a String", e);
	    return null;
	}
    }

    /**
     * Opens the desktop's default browser with a certain URL
     * @param url
     * @return False if this is not supported by the OS
     * @throws URISyntaxException
     * @throws IOException
     */
    public static boolean openBrowser(String url) throws URISyntaxException,
	    IOException {
	URI uri = new URI(url);
	if (Desktop.isDesktopSupported()) {
	    Desktop desktop = Desktop.getDesktop();
	    if (desktop.isSupported(Desktop.Action.BROWSE)) {
		desktop.browse(uri);
		return true;
	    }
	}
	return false;
    }
    
    public static boolean openMailclient(String address, String subject, String message) throws URISyntaxException, IOException {
	if (Desktop.isDesktopSupported()) {
	    Desktop desktop = Desktop.getDesktop();
	    if(desktop.isSupported(Desktop.Action.MAIL)) {
		subject = URLEncoder.encode(subject, "utf8");
		message = URLEncoder.encode(message, "utf8");
		URI uri = new URI("mailto:"+address+"?SUBJECT="+subject+"&BODY="+message);
		desktop.mail(uri);
		return true;
	    }
	}
	return false;
    }

    public static String getLocalPath() {
	if (SysPrefs.isWindows)
	    return System.getProperty("user.dir") + "\\binaries\\";
	else if (SysPrefs.isMacOSX)
	    return System.getProperty("user.dir") + "/binaries/src/";
	else
	    return System.getProperty("user.dir") + "/binaries/src/";
    }

    public static String getSafenameTree(String tree, SequenceSet ss) {
	Hashtable<String, String> lookup = new Hashtable<String, String>();
	for (Sequence seq : ss.getSequences()) {
	    lookup.put(seq.name, seq.safeName);
	}

	for (String name : lookup.keySet()) {
	    tree = tree.replaceAll(name, lookup.get(name));
	}
	return tree;
    }

    public static String getNameTree(String tree, SequenceSet ss) {
	Hashtable<String, String> lookup = new Hashtable<String, String>();
	for (Sequence seq : ss.getSequences()) {
	    lookup.put(seq.safeName, seq.name);
	}

	for (String safename : lookup.keySet()) {
	    tree = tree.replaceAll(safename, lookup.get(safename));
	}
	return tree;
    }

    /**
     * Creates a String with data from a one-dimensional array
     * @param array One-dimensional array
     * @param delim Character to separate the data fields
     * @return
     */
    public static String arrayToString(Object array, char delim) {
	StringBuffer sb = new StringBuffer();
	int length = Array.getLength(array);
	for (int i = 0; i < length - 1; i++) {
	    sb.append(Array.get(array, i));
	    sb.append(delim);
	}
	sb.append(Array.get(array, length - 1));
	return sb.toString();
    }

    /**
     * Creates an array from data held in a String
     * @param c Designated type of the array
     * @param s String which holds the data
     * @param delim Character which is used to separate the data fields
     * @return
     * @throws Exception
     */
    public static Object stringToArray(Class<?> c, String s, char delim)
	    throws Exception {
	String[] tmp = s.split("" + delim);
	Object array = Array.newInstance(c, tmp.length);
	Constructor<?> cst = c.getConstructor(String.class);
	for (int i = 0; i < tmp.length; i++)
	    Array.set(array, i, cst.newInstance(tmp[i]));
	return array;
    }

    /**
     * Copies an array into a new array of a certain type (dstClass) (usefull
     * for "casting" arrays, e.g. a float to double array, etc.) Also works with
     * multidimensional arrays
     * 
     * Notes: 
     * 1)
     * You can't directly cast a String[][]... into a primitive array
     * (e.g. int[][]...). First cast String[][]... to Integer[][]..., then cast
     * Integer[][]... to int[][]...
     * 2)
     * If you want to cast ClassA[][]... to ClassB[][]... ClassB must have a
     * constructor of the form ClassB(ClassA obj)
     * 
     * @param array
     * @param dstClass
     * @return
     */
    public static Object castArray(Object array, Class<?> dstClass)
	    throws Exception {
	// first determine the array's dimensions
	List<Integer> tmp = new ArrayList<Integer>();
	getArrayDimensions(array, tmp);
	int[] dims = new int[tmp.size()];
	for (int i = 0; i < dims.length; i++)
	    dims[i] = tmp.get(i);

	// create a new array of the designated type
	Object result = Array.newInstance(dstClass, dims);

	// if we don't deal with primitives we need a constructor
	Constructor<?> cst = null;
	if (!dstClass.isPrimitive())
	    cst = dstClass.getConstructor(getArrayClass(array));

	for (int i = 0; i < dims[0]; i++) {
	    Object obj = Array.get(array, i);
	    if (obj.getClass().isArray()) {
		Array.set(result, i, castArray(obj, dstClass));
	    } else {
		// if we deal with primitives, we can directly set them (Java
		// does the unwrapping)
		if (cst == null)
		    Array.set(result, i, obj);
		else
		    Array.set(result, i, cst.newInstance(obj));
	    }
	}

	return result;
    }

    private static void getArrayDimensions(Object array, List<Integer> dims) {
	if (array.getClass().isArray()) {
	    int length = Array.getLength(array);
	    dims.add(length);
	    if (length > 0)
		getArrayDimensions(Array.get(array, 0), dims);
	}
    }

    private static Class<?> getArrayClass(Object array) {
	if (array.getClass().isArray()) {
	    if (Array.getLength(array) > 0)
		return getArrayClass(Array.get(array, 0));
	    else
		return null;
	} else
	    return array.getClass();
    }

    /**
     * Get the position of a certain object in a list
     * @param list
     * @param obj
     * @return
     */
    public static int indexof(List<Object> list, Object obj) {
	return indexof(list.toArray(), obj);
    }

    /**
     * Get the position of a certain object in an array
     * @param list
     * @param obj
     * @return
     */
    public static int indexof(Object[] list, Object obj) {
	int index = -1;
	for (int i = 0; i < list.length; i++) {
	    if (list[i].equals(obj)) {
		index = i;
		break;
	    }
	}
	return index;
    }

}
