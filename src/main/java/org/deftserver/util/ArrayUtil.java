package org.deftserver.util;


public class ArrayUtil {

//	private static final List<String> EMPTY_STRING_LIST = Arrays.asList("");
//	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	public static String[] dropFromEndWhile(String[] array, String regex) {
		for (int i = array.length - 1 ;i >= 0; i--) {
			if (!array[i].trim().equals("")) {
				String[] trimmedArray = new String[i+1];
				System.arraycopy(array, 0, trimmedArray, 0, i+1);
				return trimmedArray; 
			}
		}
		return null;
//		{	// alternative impl
//			List<String> list = new ArrayList<String>(Arrays.asList(array));
//			list.removeAll(EMPTY_STRING_LIST);
//			return list.toArray(EMPTY_STRING_ARRAY);
//	 	}
	}
	
}
