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

package org.ohmage.prompt.media;

import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.PromptBuilder;

import java.util.ArrayList;

public class PhotoPromptBuilder implements PromptBuilder {

    @Override
    public void build(Prompt prompt, String id, String displayLabel,
            String promptText, String explanationText, String defaultValue,
            String condition, String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {

        PhotoPrompt photoPrompt = (PhotoPrompt) prompt;
        photoPrompt.setId(id);
        photoPrompt.setDisplayLabel(displayLabel);
        photoPrompt.setPromptText(promptText);
        photoPrompt.setExplanationText(explanationText);
        photoPrompt.setDefaultValue(defaultValue);
        photoPrompt.setCondition(condition);
        photoPrompt.setSkippable(skippable);
        photoPrompt.setSkipLabel(skipLabel);
        photoPrompt.setProperties(properties);

        if (properties != null) {
            for (KVLTriplet property : properties) {
                if (property.key.equals("maxDimension")) {
                    photoPrompt.setResolution(property.label);
                }
            }
        }

        photoPrompt.clearTypeSpecificResponseData();

    }

}
