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
