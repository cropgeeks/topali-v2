// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.JList;

public class TooltippedList extends JList {

	List<String> toolTips;
	
	public TooltippedList() {
		super();
		
	}

	public TooltippedList(Object[] listData) {
		super(listData);
	}

	public TooltippedList(Vector<?> listData) {
		super(listData);
	}
	
	public void setToolTips(List<String> tt) {
		this.toolTips = tt;
	}
	
	@Override
	public String getToolTipText(MouseEvent evt) {
         int index = locationToIndex(evt.getPoint());
         if(toolTips!=null && index<toolTips.size()) {
        	return toolTips.get(index); 
         }
         return null;
     }
}
