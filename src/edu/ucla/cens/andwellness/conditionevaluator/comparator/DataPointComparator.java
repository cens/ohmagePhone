package edu.ucla.cens.andwellness.conditionevaluator.comparator;

import edu.ucla.cens.andwellness.conditionevaluator.DataPoint;

public interface DataPointComparator {
	
    public boolean compare(DataPoint dataPoint, String value, String condition);
    
}
