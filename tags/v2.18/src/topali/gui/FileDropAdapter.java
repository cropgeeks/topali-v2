// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

public class FileDropAdapter extends DropTargetAdapter
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private WinMain winMain;

	FileDropAdapter(WinMain winMain)
	{
		this.winMain = winMain;
	}

	public void drop(DropTargetDropEvent dtde)
	{
		Transferable t = dtde.getTransferable();

		try
		{
			DataFlavor[] dataFlavors = t.getTransferDataFlavors();

			dtde.acceptDrop(DnDConstants.ACTION_COPY);

			for (int i = 0; i < dataFlavors.length; i++)
			{
				// System.out.println(dataFlavors[i].getRepresentationClass().toString());

				if (dataFlavors[i].getRepresentationClass().equals(
						Class.forName("java.util.List")))
				{
					List list = (List) t.getTransferData(dataFlavors[i]);

					// Check for a .topali project
					if (list.size() == 1)
					{
						String filename = list.get(0).toString();
						if (filename.toLowerCase().endsWith(".topali"))
						{
							winMain.menuFileOpenProject(filename);

							dtde.dropComplete(true);
							return;
						}
					}

					// Otherwise assume alignment file(s) are to be imported
					for (int j = 0; j < list.size(); j++)
					{
						File file = new File(list.get(j).toString());
						winMain.menuFileImportDataSet(file);
					}

					break;
				}
			}

			dtde.dropComplete(true);
		} catch (Exception e)
		{
			log.warn(e);
		}
	}

	/*
	 * public void dropActionChanged(DropTargetDragEvent dtde) { }
	 * 
	 * public void dragEnter(DropTargetDragEvent dtde) { }
	 * 
	 * public void dragExit(DropTargetEvent dte) { }
	 * 
	 * public void dragOver(DropTargetDragEvent dtde) { }
	 */
}
