package topali.gui;

import java.awt.event.*;
import javax.swing.*;

public abstract class PopupMenuAdapter extends MouseAdapter
{
	protected JPopupMenu p;
	private boolean enabled = true;
	
	public PopupMenuAdapter()
	{
		p = new JPopupMenu();
	}
	
	public void mouseReleased(MouseEvent e)
	{
		if (e.isPopupTrigger() && enabled)
		{
			handlePopup(e.getX(), e.getY());			
			p.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	protected void add(Action action, int m, int k, int mask, int i, boolean s)
	{
		add(action, null, m, k, mask, i, s);
	}
	
	protected void add(Action action, ImageIcon icon, int m, int k, int mask, int i, boolean s)
	{
		JMenuItem item = WinMainMenuBar.getItem(action, m, k, mask);
		if (i > 0)
			item.setDisplayedMnemonicIndex(i);
		if (s)
			p.addSeparator();
		if (icon != null)
			item.setIcon(icon);
		
		p.add(item);
	}
	
	public void setEnabled(boolean b) {
		this.enabled = b;
	}
	
	protected void handlePopup(int x, int y)
	{
	}}
