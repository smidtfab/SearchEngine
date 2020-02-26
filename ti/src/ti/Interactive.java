// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class contains the logic to run the retrieval process of the search engine in interactive mode.
 */
public class Interactive
{
	protected RetrievalModel model;
	protected Index index;
	protected DocumentProcessor docProcessor;

	/**
	 * Creates a new interactive retriever using the given model.
	 *
	 * @param model        the retrieval model to run queries.
	 * @param index        the index.
	 * @param docProcessor the processor to extract query terms.
	 */
	public Interactive(RetrievalModel model, Index index, DocumentProcessor docProcessor)
	{
		this.model = model;
		this.index = index;
		this.docProcessor = docProcessor;
	}

	/**
	 * Runs the interactive retrieval process. It asks the user for a query, and then it prints the results to
	 * {@link System#out} showing the document title and a snippet, highlighting important terms for the query.
	 *
	 * @throws Exception in an error occurs during the process.
	 */
	public void run() throws Exception
	{
		// Run prompt loop
		Scanner scan = new Scanner(System.in);
		String input;
		do {
			System.out.println();
			System.out.print("Query (empty to exit): ");
			scan.reset();
			input = scan.nextLine();

			if (!input.isEmpty()) {
				ArrayList<Tuple<Integer, Double>> results = this.model.runQuery(input, this.index, this.docProcessor);
				this.printResults(input, results, 0, 10);
			}
		} while (!input.isEmpty());
	}

	/**
	 * Print a page of results for a query, showing for each document its title and snippet, with highlighted terms.
	 *
	 * @param query   the input query.
	 * @param results the results for the query. A list of {@link Tuple}s where the first item is the {@code docID} and
	 *                the second item is the similarity score.
	 * @param from    index of the first result to print.
	 * @param count   how many results to print from the {@code from} index.
	 */
	protected void printResults(String query, ArrayList<Tuple<Integer, Double>> results, int from, int count) throws Exception
	{
		for (int i = from; i < results.size() && i < from + count; i++) {
			int docId = results.get(i).item1;
			String docName = this.index.documents.get(docId).item1;
			String title = this.index.getCachedDocument(docId).item1;
			String body = this.index.getCachedDocument(docId).item2;

			if (title.length() > 60)
				title = title.substring(0, 60) + "...";

			// Highlight query terms
			ArrayList<Integer> queryTermsAt = new ArrayList<>();
			String bodyLow = body.toLowerCase();
			String[] queryTerms = query.split("[^a-zA-Z0-9']+");
			for (String queryTerm : queryTerms) {
				String queryTermLow = queryTerm.toLowerCase();
				int s = bodyLow.indexOf(queryTermLow, 0);
				while (s >= 0) {
					queryTermsAt.add(s);
					bodyLow = bodyLow.substring(0, s) + "*" + queryTermLow + "*" + bodyLow.substring(s + queryTermLow.length(), bodyLow.length());
					body = body.substring(0, s) + "*" + body.substring(s, s + queryTerm.length()) + "*" + body.substring(s + queryTerm.length(), body.length());

					s = bodyLow.indexOf(queryTermLow, s + queryTermLow.length());
				}
			}

			// Snippet with most query terms
			int bestFrom = 0, bestCount = 0;
			for (int j = 0; j < queryTermsAt.size(); j++) {
				int qFrom = queryTermsAt.get(j);
				int qTo = qFrom + 300;
				int qCount = (int) queryTermsAt.stream().filter(q -> q >= qFrom && q < qTo).count();

				if (qCount > bestCount) {
					bestFrom = qFrom;
					bestCount = qCount;
				}
			}

			body = body.substring(bestFrom, Math.min(body.length(), bestFrom + 300)).trim();
			body = "..." + body + "...";

			System.out.println();
			System.out.println((i + 1) + " (" + docName + "): " + title);
			System.out.println(body);
		}
	}
}