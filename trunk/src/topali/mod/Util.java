// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.mod;

import java.util.*;

public class Util
{

	public static String mapToString(Map<String, String> map, char keyValueDelim, char delim) {
		StringBuffer sb = new StringBuffer();
		Set<String> keys = map.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()) {
			String k = it.next();
			sb.append(k);
			sb.append(keyValueDelim);
			sb.append(map.get(k));
			if(it.hasNext())
				sb.append(delim);
		}
		return sb.toString();
	}
	
	public static Map<String, String> stringToMap(String map, char keyValueDelim, char delim) {
		Map<String, String> result = new HashMap<String, String>();
		String[] tokens = map.split(Character.toString(delim));
		for(String token : tokens) {
			String[] tmp = token.split(Character.toString(keyValueDelim));
			result.put(tmp[0], tmp[1]);
		}
		return result;
	}
}
