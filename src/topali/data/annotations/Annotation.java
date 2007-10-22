// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public abstract class Annotation implements Comparable<Annotation>
{
	static List<Class<Annotation>> availableAnnotationTypes = null;
	
	int pos = -1;
	int length = -1;
	String desc = null;
	String comment = "";
	String link = null;

	public Annotation(String desc) {
		this.desc = desc;
	}
	
	public int getPos()
	{
		return pos;
	}

	public void setPos(int pos)
	{
		this.pos = pos;
	}

	public int getLength()
	{
		return length;
	}

	public void setLength(int length)
	{
		this.length = length;
	}

	public String getLink()
	{
		return link;
	}

	public void setLink(String link)
	{
		this.link = link;
	}
	
	public String getDesc()
	{
		return desc;
	}
	
	@Override
	public int compareTo(Annotation o)
	{
		if(o.pos>this.pos)
			return 1;
		else if(o.pos<this.pos)
			return -1;
		else
			return 0;
	}

	@SuppressWarnings("unchecked")
	public static List<Class<Annotation>> getAvailableAnnotationTypes() {
		if(Annotation.availableAnnotationTypes==null) {
			Annotation.availableAnnotationTypes = new LinkedList<Class<Annotation>>();
			
			Package pack = Annotation.class.getPackage();
			
			String name = new String(pack.getName());
	        if (!name.startsWith("/")) {
	            name = "/" + name;
	        }        
	        name = name.replace('.','/');
	        
	        // Get a File object for the package
	        URL url = Annotation.class.getResource(name);
	        File directory = new File(url.getFile());
	        if (directory.exists()) {
	        	// Get the list of the files contained in the package
	            String [] files = directory.list();
	            for (int i=0;i<files.length;i++) {
	                // we are only interested in .class files
	                if (files[i].endsWith(".class")) {
	                    // removes the .class extension
	                    String classname = files[i].substring(0,files[i].length()-6);
	                    try {
	                    	Class c = Class.forName(pack.getName()+"."+classname);
	                        if (c.getGenericSuperclass().equals(Annotation.class)) {
	                        	Annotation.availableAnnotationTypes.add((Class<Annotation>)c);
	                        }
	                    } catch (Exception e) {
	                    }
	                }
	            }
	        }
		}
		return Annotation.availableAnnotationTypes;
	}
	
	public static String getDescription(Class<? extends Annotation> type) {
		try
		{
			Method m = type.getMethod("getDesc", new Class<?>[0]);
			String desc = (String)m.invoke(type.newInstance(), new Object[0]);
			return desc;
		} catch (Exception e)
		{
			return "";
		} 
	}
	
}
