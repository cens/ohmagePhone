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
package edu.ucla.cens.andwellness.conditionevaluator;

import java.util.HashMap;
import java.util.Map;

public class DataPoint {
    // List of data fields in a data point, add more if necessary for extended data points
    private String id;
    private Object value;  
    private DataPoint.DisplayType displayType;
    private DataPoint.PromptType promptType;
    
    // Need to know whether the DataPoint was "SKIPPED" or "NOT_DISPLAYED"
    // If so the value is meaningless
    private boolean isSkipped;
    private boolean isNotDisplayed;
    
    // Map to store all possible metadata
    Map<String, Object> metadata = new HashMap<String, Object>();
    
    // Possible display types
    public static enum DisplayType {
        category, measurement, event, count, metadata
    }
    
    // Possible prompt types
    public static enum PromptType {
        timestamp, number, hours_before_now, text, multi_choice, single_choice, single_choice_custom, multi_choice_custom, photo
    }
   
    // Nothing to do here!
    public DataPoint() {}
        
    public DataPoint(String _id) {
        id = _id;
    }
    
    // Large set of putters to create the json map
    public void setId(String _id) {
        id = _id;
    }
    
    public String getId() {
        return id;
    }
    
    public void setValue(Object _value) {
        value = _value;
    }

    public Object getValue() {
        return value;
    }
    
    public void setUnit(String _unit) {
        metadata.put("unit", _unit);
    }
    
    public String getUnit() {
       return (String) metadata.get("unit");
    }
    
    public void setDatetime(String _datetime) {
        metadata.put("datetime", _datetime);
    }
    
    public String getDatetime() {
        return (String) metadata.get("datetime");
    }
    
    public void setTz(String _tz) {
        metadata.put("tz", _tz);
    }
    
    public String getTz() {
        return (String) metadata.get("tz");
    }
    
    public void setLat(String _lat) {
        metadata.put("lat", _lat);
    }
    
    public String getLat() {
        return (String) metadata.get("lat");
    }
    
    public void setLon(String _lon) {
        metadata.put("lon", _lon);
    }
    
    public String getLon() {
        return (String) metadata.get("lon");
    }
    
    public void setDisplayType(DisplayType _displayType) {
        displayType = _displayType;
    }
    
    /**
     * Convenience function to set display type directly from a String
     * 
     * @param _displayType A String representing a type from DisplayType
     */
    public void setDisplayType(String _displayType) {
        if (DisplayType.category.toString().equals(_displayType)) {
            displayType = DisplayType.category;
        }
        else if (DisplayType.event.toString().equals(_displayType)) {
            displayType = DisplayType.event;
        }
        else if (DisplayType.measurement.toString().equals(_displayType)) {
            displayType = DisplayType.measurement;
        }
        else if (DisplayType.count.toString().equals(_displayType)) {
            displayType = DisplayType.count;
        }
        else if (DisplayType.metadata.toString().equals(_displayType)) {
            displayType = DisplayType.metadata;
        }
        else {
            throw new IllegalArgumentException("Display type does not exist: " + _displayType);
        }
    }
    
    public DisplayType getDisplayType() {
        return displayType;
    }
    
    public void setPromptType(PromptType _promptType) {
        promptType = _promptType;
    }
    
    /**
     * Convenience function to set prompt type directly from a String
     * 
     * @param _promptType A String representing a type from DisplayType
     */
    public void setPromptType(String _promptType) {
        // timestamp, number, hours_before_now, text, multi_choice, single_choice, single_choice_custom, multi_choice_custom, photo
        
        if (PromptType.timestamp.toString().equals(_promptType)) {
            promptType = PromptType.timestamp;
        }
        else if (PromptType.number.toString().equals(_promptType)) {
            promptType = PromptType.number;
        }
        else if (PromptType.hours_before_now.toString().equals(_promptType)) {
            promptType = PromptType.hours_before_now;
        }
        else if (PromptType.text.toString().equals(_promptType)) {
            promptType = PromptType.text;
        }
        else if (PromptType.multi_choice.toString().equals(_promptType)) {
            promptType = PromptType.multi_choice;
        }
        else if (PromptType.single_choice.toString().equals(_promptType)) {
            promptType = PromptType.single_choice;
        }
        else if (PromptType.single_choice_custom.toString().equals(_promptType)) {
            promptType = PromptType.single_choice_custom;
        }
        else if (PromptType.multi_choice_custom.toString().equals(_promptType)) {
            promptType = PromptType.multi_choice_custom;
        }
        else if (PromptType.photo.toString().equals(_promptType)) {
            promptType = PromptType.photo;
        }
        else {
            throw new IllegalArgumentException("Prompt type does not exist: " + _promptType);
        }
    }
    
    public PromptType getPromptType() {
        return promptType;
    }
    
    public void setSkipped() {
        isSkipped = true;
    }
    
    public boolean isSkipped() {
        return isSkipped;
    }
    
    public void setNotDisplayed() {
        isNotDisplayed = true;
    }
    
    public boolean isNotDisplayed() {
        return isNotDisplayed;
    }
    
    public boolean isMetadata() {
        if (DisplayType.metadata.equals(displayType)) {
            return true;
        }
            
        return false;
    }

    public String toString() {
        return "type " + promptType.toString() + " id " + id + " value " + value;
    }
    
    /**
     * Pass in a DataPoint to compare.  Comparison is done based on
     * node IDs ONLY.
     * 
     * @param toCompare The DataPoint to compare to.
     * @return true/false
     */
    public boolean equals(Object toCompare) {
        if (toCompare instanceof DataPoint)
            return getId().equals(((DataPoint) toCompare).getId());
        
        return false;
    }
}
