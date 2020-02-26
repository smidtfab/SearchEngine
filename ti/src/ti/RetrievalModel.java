// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.util.ArrayList;

/**
 * This interface defines methods implemented by a retrieval model.
 */
public interface RetrievalModel
{
	/**
	 * Runs the given query and returns the list of documents sorted by similarity.
	 *
	 * @param queryText    the text of the query.
	 * @param index        the index to search in.
	 * @param docProcessor the {@link DocumentProcessor} to extract query terms.
	 * @return a list of {@link Tuple}s where the first item is the {@code docID} and the second one the similarity score.
	 */
	ArrayList<Tuple<Integer, Double>> runQuery(String queryText, Index index, DocumentProcessor docProcessor);
}
