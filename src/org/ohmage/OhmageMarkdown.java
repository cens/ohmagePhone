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

package org.ohmage;


import android.text.Html;


/**
 * Parser for ohmage Markdown
 * 
 * @author cketcham
 */
public class OhmageMarkdown {

	/**
	 * Parses string to SpannableString. Converts ** to bold and * to italic.
	 * @param label
	 * @return SpannableString
	 */
	public static CharSequence parse(String label) {
		return Html.fromHtml(parseHtml(label));
	}

	/**
	 * Parses string to HTML. Converts ** to bold and * to italic.
	 * @param label
	 * @return HTML string
	 */
	public static String parseHtml(String label) {
		label = Utilities.getHtmlSafeDisplayString(label);
		label = convertCharacters(label, "**", "<b>", "</b>");
		label = convertCharacters(label, "*", "<i>", "</i>");
		return label;
	}

	/**
	 * Searches String for find. Replaces each occurrence with open then close
	 * @param string
	 * @param find
	 * @param open
	 * @param close
	 * @return
	 */
	private static String convertCharacters(String string, String find, String openTag, String closeTag) {
		StringBuilder builder = new StringBuilder();
		int last = 0;
		int open = 0;
		for(int i = string.indexOf(find); i != -1; i = string.indexOf(find, i+find.length())) {
			boolean escaped = (i-1 >= 0 && (string.charAt(i-1) == '\\'));
			builder.append(string.substring(last, (escaped ? i-1 : i)));
			if(!escaped)
				builder.append(open++ % 2 == 0 ? openTag : closeTag);
			last = i + (escaped ? 0 : find.length());
		}
		builder.append(string.substring(last,string.length()));
		return builder.toString();
	}
}
