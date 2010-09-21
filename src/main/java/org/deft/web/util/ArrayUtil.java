package org.deft.web.util;

public class ArrayUtil {

	public static String[] removeTrailingEmptyStrings(String[] array) {
		for (int i = array.length - 1 ;i >= 0; i--) {
			if (!array[i].equals("")) {
				String[] trimmedArray = new String[i+1];
				System.arraycopy(array, 0, trimmedArray, 0, i+1);
				return trimmedArray; 
			}
		}
		return null;
	}
	
}
