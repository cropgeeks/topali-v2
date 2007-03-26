// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.awt.BorderLayout;
import java.util.LinkedList;
import java.util.logging.*;

import javax.swing.*;

public class ClientLogHandler extends Handler
{

	LinkedList<String> msg;
	int cap = 5;
	
	Level userWarnLevel;
	boolean shutdown;
	
	public ClientLogHandler(Level userWarnLevel, boolean shutdown) {
		super();
		this.userWarnLevel = userWarnLevel;
		this.shutdown = shutdown;
		msg = new LinkedList<String>();
		setFormatter(new SimpleFormatter());
	}
	
	public void setCapacity(int cap) {
		this.cap = cap;
	}
	
	@Override
	public void close() throws SecurityException
	{
		
	}

	@Override
	public void flush()
	{

	}

	@Override
	public void publish(LogRecord record)
	{
		if(!isLoggable(record))
			return;
		
		msg.add(this.getFormatter().format(record));
		if(msg.size()>cap)
			msg.removeFirst();
		
		if(record.getLevel().intValue()>=userWarnLevel.intValue()) {
			JTextArea ta = new JTextArea();
			
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<msg.size(); i++)
				sb.append(msg.get(i));
			
			ta.setText(sb.toString());
			JLabel l;
			if(shutdown)
				l = new JLabel("<html>A <b>fatal error</b> occurred!<br>Application must be shutted down.<br><br>Log details:</html>");
			else
				l = new JLabel("<html>An <b>error</b> occurred!<br><br>Log details:</html>");
			JPanel p = new JPanel(new BorderLayout());
			p.add(l, BorderLayout.NORTH);
			p.add(new JScrollPane(ta), BorderLayout.CENTER);
			JOptionPane.showMessageDialog(null, p, "Error", JOptionPane.ERROR_MESSAGE);
			if(shutdown)
				System.exit(1);
		}
	}

}
