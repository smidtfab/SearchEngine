// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.util.ArrayList;

/**
 * This interface defines methods to parse and process the text from documents of a particular tipe (eg, html or text).
 */
public interface DocumentProcessor
{
	/**
	 * Parse the given document content and extract its title and the main body text.
	 *
	 * @param docText the raw text of the document.
	 * @return a {@link Tuple} with the document title and the main body text. If either of them can not be extracted,
	 * it will return {@code ""} in that tuple item.
	 */
	Tuple<String, String> parse(String docText);

	/**
	 * Process the given text (tokenize, normalize, etc.) and return the list of terms to index.
	 *
	 * @param text the text to process.
	 * @return the list of index terms.
	 */
	ArrayList<String> processText(String text);
}
