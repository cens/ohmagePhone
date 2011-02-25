package edu.ucla.cens.andwellness.conditionevaluator.comparator;

import edu.ucla.cens.andwellness.conditionevaluator.DataPoint;

/**
 * Comparator for the single_choice_custom prompt type.
 * 
 * @author Mohamad Monibi
 *
 */
public class SingleChoiceCustomDataPointComparator extends AbstractDataPointComparator {

	@Override
	protected boolean equals(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
        Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) == 0) {
            return true;
        } else {
        	return false;
        }
	}

	@Override
	protected boolean greaterThan(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
		Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) > 0) {
            return true;
        } else {
        	return false;
        }
	}

	@Override
	protected boolean greaterThanOrEquals(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
		Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) >= 0) {
            return true;
        } else {
        	return false;
        }
	}

	@Override
	protected boolean lessThan(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
		Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) < 0) {
            return true;
        } else {
        	return false;
        }
	}

	@Override
	protected boolean lessThanOrEquals(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
		Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) <= 0) {
            return true;
        } else {
        	return false;
        }
	}

	@Override
	protected boolean notEquals(DataPoint dataPoint, String value) {
		Integer dataPointValue = (Integer) dataPoint.getValue();
		Integer valueToCompare = Integer.parseInt(value);
        
        if (dataPointValue.compareTo(valueToCompare) != 0) {
            return true;
        } else {
        	return false;
        }
	}

}
