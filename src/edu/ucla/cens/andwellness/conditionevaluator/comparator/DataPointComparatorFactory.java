package edu.ucla.cens.andwellness.conditionevaluator.comparator;

import edu.ucla.cens.andwellness.conditionevaluator.DataPoint;

/**
 * Creates a DataPointComparator based on the prompt type.  The possible prompt types are
 * defined in the DataPoint.PromptType enum. Each prompt type must have a comparator defined
 * by extending the AbstractDataPointComparator class.
 * 
 * @author mmonibi
 *
 */
public final class DataPointComparatorFactory {
    private DataPointComparatorFactory() {};
    
    /**
     * Create one of six comparators based on the prompt type.
     * 
     * @param promptType One of the prompt types defined by the DataPoint.PromptType enum
     * @return The appropriate DataPointComparator
     */
    public static DataPointComparator createDataPointComparator(DataPoint.PromptType promptType) {
    	
    	switch (promptType) {
		case single_choice:
			return new SingleChoiceDataPointComparator();
			
		case single_choice_custom:
			return new SingleChoiceCustomDataPointComparator();
			
		case multi_choice:
			return new MultiChoiceDataPointComparator();
			
		case multi_choice_custom:
			return new MultiChoiceCustomDataPointComparator();
			
		case number:
			return new NumberDataPointComparator();
			
		case hours_before_now:
			return new HoursBeforeNowDataPointComparator();
			
		default:
			throw new IllegalArgumentException("No comparator defined for this prompt type.");
		}
    	
    }
}
