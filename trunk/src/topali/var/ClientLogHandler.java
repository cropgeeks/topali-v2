// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.logging.*;

import javax.swing.*;

import topali.gui.Application;

/**
 * A custom log handler, which pops up an error message, when receiving a log message of a certain level.
 * The user then can deceide whether to savely close the application (writing unsaved data to disk) or
 * ignore the error message.
 * Can also be used as UncaughtExceptionHandler, to catch uncaught exeptions.
 * (@see Thread.UncaughtExceptionHandler )
 */
public class ClientLogHandler extends Handler implements UncaughtExceptionHandler
{

	LinkedList<String> msg;
	int cap = 10;
	
	Application app;
	Logger log;
	Level userWarnLevel;
	
	/**
	 * @param app The application this handler is connected to (null allowed)
	 * @param log The logger this handler is connected to
	 * @param userWarnLevel The level when the user will be warned with a message popup
	 */
	public ClientLogHandler(Application app, Logger log, Level userWarnLevel) {
		super();
		this.app = app;
		this.log = log;
		this.userWarnLevel = userWarnLevel;
		msg = new LinkedList<String>();
		setFormatter(new SimpleFormatter());
	}
	
	public void setApplication(Application app) {
		this.app = app;
	}
	
	/**
	 * @param cap Number of log records this logger will store (default: 10)
	 */
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
		
		msg.add(getFormatter().format(record));
		if(msg.size()>cap)
			msg.removeFirst();
		
		if(record.getLevel().intValue()>=userWarnLevel.intValue()) {
			JTextArea ta = new JTextArea();
			
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<msg.size(); i++)
				sb.append(msg.get(i));
			
			ta.setText(sb.toString());
			ta.setEditable(false);
			
			JLabel l = new JLabel("<html>An <b>unexpected error</b> occurred!<br><br>Log details:</html>");
			JPanel p = new JPanel(new BorderLayout());
			p.add(l, BorderLayout.NORTH);
			p.add(new JScrollPane(ta), BorderLayout.CENTER);
			p.add(new JLabel("<html><br>Shutdown application?</html>"), BorderLayout.SOUTH);
			p.setPreferredSize(new Dimension(400,300));
			
			int x = JOptionPane.showConfirmDialog(null, p, "Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
			
			if(x==JOptionPane.YES_OPTION) {
				if(app!=null)
					app.shutdown();
				else
					System.exit(1);
			}
		}
	}

	
	public void uncaughtException(Thread t, Throwable e)
	{
		log.log(Level.SEVERE, "Uncaught exception in thread '"+t.getName()+"'", e);
	}	
}
