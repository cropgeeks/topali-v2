// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.i18n;

import java.text.MessageFormat;
import java.util.*;
import org.apache.log4j.Logger;

public class Text {

	static Logger log = Logger.getLogger(Text.class);
	
	private static ResourceBundle I18N = null;

	static {
		I18N = ResourceBundle.getBundle("res.text.i18n", Locale.getDefault());
	}

	public static String get(String key) {
		if(I18N.containsKey(key))
			return I18N.getString(key);
		else {
			StackTraceElement[] stes = Thread.currentThread().getStackTrace();
			log.warn("I18N key '"+key+"' not found!\n(Calling method: "+stes[3]+")");
			return "!" + key + "!";
		}
	}

	public static String get(String key, Object... args) {
		if (I18N.containsKey(key)) {
			MessageFormat msg = new MessageFormat(I18N.getString(key));
			return msg.format(args);
		} else {
			StackTraceElement[] stes = Thread.currentThread().getStackTrace();
			log.warn("I18N key '"+key+"' not found!\n(Calling method: "+stes[3]+")");
			return "!" + key + "!";
		}
	}
}
