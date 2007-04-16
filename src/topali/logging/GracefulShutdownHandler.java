// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.logging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;

import javax.swing.*;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import topali.gui.Application;

public class GracefulShutdownHandler extends AppenderSkeleton implements UncaughtExceptionHandler
{
	final int capacity = 10;
	LinkedList<String> messages = new LinkedList<String>();
	Application application;
	
	public GracefulShutdownHandler() {
		super();
	}
	
	@Override
	protected void append(LoggingEvent arg0)
	{
		String mes = this.layout.format(arg0);
		if(this.layout.ignoresThrowable()) {
			String[] throwStrings = arg0.getThrowableStrRep();
			if(throwStrings!=null)
				for(String s : throwStrings)
					mes += s+"\n";
		}
		messages.add(mes);
		
		if(messages.size()>capacity)
			messages.removeFirst();
		
		if(arg0.getLevel().isGreaterOrEqual(Level.FATAL)) {
			showErrorMessage();
		}
	}

	private void showErrorMessage() {
		JTextArea ta = new JTextArea();
		
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<messages.size(); i++)
			sb.append(messages.get(i));
		
		ta.setText(sb.toString());
		ta.setEditable(false);
		
		JLabel l = new JLabel("<html>A <b>fatal error</b> occurred!<br><br>Log details:</html>");
		JPanel p = new JPanel(new BorderLayout());
		p.add(l, BorderLayout.NORTH);
		p.add(new JScrollPane(ta), BorderLayout.CENTER);
		p.add(new JLabel("<html><br>Shutdown application?</html>"), BorderLayout.SOUTH);
		p.setPreferredSize(new Dimension(400,300));
		
		int x = JOptionPane.showConfirmDialog(null, p, "Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
		
		if(x==JOptionPane.YES_OPTION) {
			if(application!=null)
				application.shutdown();
			else
				System.exit(1);
		}
	}
	
	public void setApplication(Application app) {
		this.application = app;
	}
	
	public void close()
	{

	}

	public boolean requiresLayout()
	{
		return true;
	}

	public void uncaughtException(Thread t, Throwable e)
	{
		Logger.getRootLogger().fatal("An uncaught Exception has been thrown in Thread "+t.getName()+"!", e);
	}

}
