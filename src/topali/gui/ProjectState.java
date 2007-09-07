// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

public class ProjectState
{
	private static boolean dataChanged, vamsasSession, vamsasCommitted, fileSaved;

	public static void reset() {
		dataChanged = false;
		vamsasSession = false;
		vamsasCommitted = false;
		fileSaved = false;
		
		WinMainMenuBar.aFileSave.setEnabled(false);
		WinMainMenuBar.aVamCommit.setEnabled(false);
		WinMainMenuBar.aVamsasButton.setEnabled(true);
	}
	
	public static void setDataChanged()
	{
		ProjectState.dataChanged = true;
		WinMainMenuBar.aFileSave.setEnabled(true);
		if(vamsasSession) {
			WinMainMenuBar.aVamCommit.setEnabled(true);
			WinMainMenuBar.aVamsasButton.setEnabled(true);
		}
	}

	public static void setVamsasSession(boolean b)
	{
		ProjectState.vamsasSession = b;
		WinMainMenuBar.aVamsasButton.setEnabled(!b);
	}

	public static void setVamsasCommitted()
	{
		ProjectState.vamsasCommitted = true;
		if(fileSaved)
			dataChanged=false;
		WinMainMenuBar.aVamCommit.setEnabled(false);
		WinMainMenuBar.aVamsasButton.setEnabled(false);
	}

	public static void setFileSaved()
	{
		ProjectState.fileSaved = true;
		if(!vamsasSession)
			dataChanged = false;
		else if(vamsasCommitted)
			dataChanged = false;
		
		WinMainMenuBar.aFileSave.setEnabled(false);
	}

	public static boolean isVamsasSession()
	{
		return vamsasSession;
	}

	public static boolean isVamsasCommitted()
	{
		return vamsasCommitted;
	}

	public static boolean isFileSaved()
	{
		return fileSaved;
	}
	
}
