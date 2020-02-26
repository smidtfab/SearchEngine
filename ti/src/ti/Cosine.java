// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.util.*;

/**
 * Implements retrieval in a vector space with the cosine similarity function and a TFxIDF weight formulation.
 */
public class Cosine implements RetrievalModel
{
	public Cosine()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArrayList<Tuple<Integer, Double>> runQuery(String queryText, Index index, DocumentProcessor docProcessor)
	{
		ArrayList<String> queryTerms = docProcessor.processText(queryText);
		ArrayList<Tuple<Integer, Double>> queryVector = this.computeVector(queryTerms, index);
		return this.computeScores(queryVector, index);
	}

	/**
	 * Returns the list of documents in the specified index sorted by similarity with the specified query vector.
	 *
	 * @param queryVector the vector with query term weights.
	 * @param index       the index to search in.
	 * @return a list of {@link Tuple}s where the first item is the {@code docID} and the second one the similarity score.
	 */
	protected ArrayList<Tuple<Integer, Double>> computeScores(ArrayList<Tuple<Integer, Double>> queryVector, Index index)
	{
		HashMap<Integer, Double> sims = new HashMap<>(); // [docID] -> [sim]
		double queryNorm = 0;
		for (Tuple<Integer, Double> qTerm : queryVector) { // Foreach query term
			int termId = qTerm.item1;
			double qW = qTerm.item2;

			for (Tuple<Integer, Double> posting : index.invertedIndex.get(termId)) { // Foreach posting
				int docID = posting.item1;
				double dW = posting.item2;
				Double docScore = sims.get(docID);
				if (docScore == null) // New document?
					docScore = 0d;
				sims.put(docID, docScore + dW * qW);
			}
			queryNorm += qW * qW;
		}
		queryNorm = Math.sqrt(queryNorm);

		ArrayList<Tuple<Integer, Double>> results = new ArrayList<>(sims.size());
		for (Map.Entry<Integer, Double> sim : sims.entrySet()) {
			int docID = sim.getKey();
			double simScore = sim.getValue();
			double docNorm = index.documents.get(docID).item2;
			results.add(new Tuple<>(docID, simScore / queryNorm / docNorm));
		}

		Collections.sort(results, new Comparator<Tuple<Integer, Double>>()
		{
			@Override
			public int compare(Tuple<Integer, Double> o1, Tuple<Integer, Double> o2)
			{
				return o2.item2.compareTo(o1.item2);
			}
		});
		return results;
	}

	/**
	 * Compute the vector of weights for the specified list of terms.
	 *
	 * @param terms the list of terms.
	 * @param index the index
	 * @return a list of {@code Tuple}s with the {@code termID} as first item and the weight as second one.
	 */
	protected ArrayList<Tuple<Integer, Double>> computeVector(ArrayList<String> terms, Index index)
	{
		ArrayList<Tuple<Integer, Double>> vector = new ArrayList<>();

		HashSet<String> uniqTerms = new HashSet<>(terms);
		for (String term : uniqTerms) {
			Tuple<Integer, Double> termInfo = index.vocabulary.get(term);
			if (termInfo != null) { // If it is in the index...
				int termId = termInfo.item1;
				double idf = termInfo.item2;
				double tf = 1.0 + Math.log(Collections.frequency(terms, term));

				vector.add(new Tuple<>(termId, tf * idf));
			}
		}

		return vector;
	}
}
