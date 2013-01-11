/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.conditionevaluator.comparator;

import org.ohmage.conditionevaluator.DataPoint;
import org.ohmage.prompt.AbstractPrompt;

/**
 * Base class for prompt-type-specific data point comparators.
 * All prompt types that can be conditioned on must define a
 * comparator extending this class and implementing the abstract
 * methods corresponding to the condition operators.
 * 
 * @author mmonibi
 *
 */
public abstract class AbstractDataPointComparator implements DataPointComparator {

	/**
	 * returns the result of the comparison between the dataPoint value 
	 * and provided value using the condition operator
	 */
	@Override
	public boolean compare(DataPoint dataPoint, String value, String condition) {
		// If the data point is NOT_DISPLAYED, then return false unless we are comparing
	    // DataPoint      condition   value
	    // NOT_DISPLAYED  ==          NOT_DISPLAYED
	    // DISPLAYED      !=          NOT_DISPLAYED
	    if(AbstractPrompt.NOT_DISPLAYED_VALUE.equals(value)) {
            return ("==".equals(condition) && dataPoint.isNotDisplayed())
                    || ("!=".equals(condition) && !dataPoint.isNotDisplayed());
	    }

        // If the data point is SKIPPED, then return false unless we are comparing
        // DataPoint      condition   value
        // SKIPPED        ==          SKIPPED
        // SHOWN          !=          SKIPPED
        if(AbstractPrompt.SKIPPED_VALUE.equals(value)) {
            return ("==".equals(condition) && dataPoint.isSkipped())
                    || ("!=".equals(condition) && !dataPoint.isSkipped());
        }

	    // Do the standard comparisons
		if ("==".equals(condition)) {
			return equals(dataPoint, value);
        }
        else if ("!=".equals(condition)) {
        	return notEquals(dataPoint, value);      
        }
        else if ("<".equals(condition)) {
        	return lessThan(dataPoint, value);
        }
        else if (">".equals(condition)) {
        	return greaterThan(dataPoint, value);
        }
        else if ("<=".equals(condition)) {
        	return lessThanOrEquals(dataPoint, value);
        }
        else if (">=".equals(condition)) {
        	return greaterThanOrEquals(dataPoint, value);
        }
        else {
            throw new IllegalArgumentException("The condition is not valid.");
        }
		
	}

	abstract boolean equals(DataPoint dataPoint, String value);
    abstract boolean notEquals(DataPoint dataPoint, String value);
    abstract boolean greaterThan(DataPoint dataPoint, String value);
    abstract boolean greaterThanOrEquals(DataPoint dataPoint, String value);
    abstract boolean lessThan(DataPoint dataPoint, String value);
    abstract boolean lessThanOrEquals(DataPoint dataPoint, String value);
}
