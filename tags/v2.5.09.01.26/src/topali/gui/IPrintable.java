// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.print.Printable;

public interface IPrintable
{
	public boolean isPrintable();

	public Printable[] getPrintables();
}