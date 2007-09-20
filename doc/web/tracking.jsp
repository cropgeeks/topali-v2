<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.io.*, java.util.*" %>

<%
	Logger tracker = Logger.getLogger("topali.tracker");

	String host = request.getRemoteHost();
	String addr = request.getRemoteAddr();
	String id   = request.getParameter("id");

	LinkedList<String> msgs = new LinkedList<String>();
	int i = 1;
	while (request.getParameter("msg" + i) != null)
		msgs.add(request.getParameter("msg" + (i++)));

	// Log connections from TOPALi
	if (id != null)
	{
		String logStr = host + " - " + addr + " - " + id;
		for (String msg: msgs)
			logStr += " - " + msg;

		tracker.info(logStr);
		return;
	}

	// Log links from apache for downloads
	String link = request.getParameter("link");
	if (link != null)
	{
		tracker.info(host + " - " + addr + " - " + link.substring(link.lastIndexOf("/")));
		response.sendRedirect(link);

		return;
	}
%>
