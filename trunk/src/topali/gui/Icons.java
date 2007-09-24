// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.Color;
import java.lang.reflect.Field;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

/*
 * Simple helper class that contains statically allocated ImageIcon objects that
 * can be accessed by anyone/anywhere elsewhere in the code.
 */
public class Icons
{
	public static   Logger log = Logger.getLogger(Icons.class);
	
	// Globally used colours
	public static Color grayBackground = new Color(119, 126, 143);

	public static Color blueBorder = new Color(127, 157, 185);

	public static ImageIcon EMPTY;

	public static ImageIcon APP_FRAME, OPEN_PROJECT, PRINT, RUN_PDM, RUN_LRT,
			TREE_FOLDER_OPEN, TREE_FOLDER_CLOSED, TREE_ALIGNMENT, TREE_PDM,
			CREATE_TREE, COMMS, TREE_TREE, SIZED_TO_FIT, BACK1, BACK2,
			RUN_PDM2, NEXT1, NEXT2, TREE_NORMAL, TREE_CIRCULAR, TREE_NEWHAMP,
			EXPORT, FLOAT, TREE_RESULTS_OPEN, LOCAL, TREE_RESULTS_CLOSED,
			TREE_DSS, RUN_HMM, RUN_DSS, PLAYER_PLAY, PLAYER_REW, PLAYER_START,
			PLAYER_STOP, PLAYER_END, SETTINGS, TREE_HMM, AUTO_PARTITION,
			ANALYSIS_INFO, RESELECT, ADD_PARTITION, ADJUST_THRESHOLD,
			STATUS_OFF, STATUS_RED, STATUS_GRE, STATUS_BLU, TREE_LRT,
			TREE_TOOLTIPS, CLUSTER, TREE_TABLE, TABLE_IMPORT, TABLE_REMOVE,
			UNKNOWN, CODEML, VISIBLE;

	public static ImageIcon RECOMBINATION, RECOMBINATION_DSS, RECOMBINATION_LRT, RECOMBINATION_PDM, RECOMBINATION_HMM,
			POSSELECTION, POSSELECTION_SITE, POSSELECTION_BRANCH, TREE, CODINGREGIONS, CODONUSAGE, TREE_F84, TREE_MRBAYES, TREE_PHYML;
	
	public static ImageIcon SAVE16, SAVEAS16, OPEN_PROJECT16, PRINT16, FIND16;

	public static ImageIcon INFO16, REMOVE16, IMPORT16, NEW_PROJECT16, UP16;

	public static ImageIcon DOWN16, HELP16;

	public static ImageIcon WIN_WARN, WIN_INFORM, WIN_ERROR, WIN_QUESTION;
	
	public static ImageIcon VAMSASON, VAMSASOFF;
	
	public static ImageIcon MIDPOINT_ROOT, TREE_ANCESTOR;
	
	private Icons()
	{
	}

	public static void loadIcons()
	{
		Icons icons = new Icons();
		Class c = icons.getClass();

		try
		{
			long s = System.currentTimeMillis();
			Field[] fields = c.getFields();
			for (Field field : fields)
			{
				if (field.getType() == ImageIcon.class)
				{
					String name = field.getName().toLowerCase() + ".png";

					ImageIcon icon = new ImageIcon(c.getResource("/res/icons/"
							+ name));

					field.set(null, icon);
				}
			}
			log.info((System.currentTimeMillis() - s)+ "ms to load icons");
		} catch (Exception e)
		{
			throw new RuntimeException("Cannot load icons", e);
		}
	}
}