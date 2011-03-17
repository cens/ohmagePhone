package edu.ucla.cens.andwellness;


public class Utilities {

	public static class KVPair {
		
		public String key;
		public String value;
		
		public KVPair(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
	
	public static class KVLTriplet {
		
		public String key;
		public String value;
		public String label;
		
		public KVLTriplet(String key, String value, String label) {
			this.key = key;
			this.value = value;
			this.label = label;
		}
	}
	
	public static String stringArrayToString(String [] array, String separator) {
		StringBuilder result = new StringBuilder();
	    if (array.length > 0) {
	        result.append(array[0]);
	        for (int i=1; i<array.length; i++) {
	            result.append(separator);
	            result.append(array[i]);
	        }
	    }
	    return result.toString();
	}
}
