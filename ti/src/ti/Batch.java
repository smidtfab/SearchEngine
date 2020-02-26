package ti;

// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.



import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

/**
 * This class contains the logic to run the retrieval process of the search engine in batch mode.
 */
public class Batch
{
	protected File pathToQueries;

	protected RetrievalModel model;
	protected Index index;
	protected DocumentProcessor docProcessor;

	/**
	 * Creates a new batch retriever for the queries in the specified path and using the given model.
	 *
	 * @param pathToQueries the path to the file with queries.
	 * @param model         the retrieval model to run queries.
	 * @param index         the index.
	 * @param docProcessor  the processor to extract query terms.
	 */
	public Batch(File pathToQueries, RetrievalModel model, Index index, DocumentProcessor docProcessor)
	{
		this.pathToQueries = pathToQueries;
		this.model = model;
		this.index = index;
		this.docProcessor = docProcessor;
	}

	/**
	 * Reads the queries in the specified file path.
	 *
	 * @param pathToQueries the path to the file.
	 * @return a list of {@code Tuple}s where the first item is the {@code queryID} and the second one is the query text.
	 * @throws Exception in an error occurs while reading the file.
	 */
	protected static ArrayList<Tuple<String, String>> readQueries(File pathToQueries) throws Exception
	{
		ArrayList<Tuple<String, String>> queries = new ArrayList<>();

		NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(pathToQueries).getElementsByTagName("topic");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element node = (Element) nodes.item(i);
			String queryId = node.getAttribute("id");
			String queryString = node.getElementsByTagName("title").item(0).getTextContent();
			queries.add(new Tuple<>(queryId, queryString));
		}
		return queries;
	}

	/**
	 * Runs the batch retrieval process. For each query, it prints the results to {@link System#out} in TREC format.
	 *
	 * @throws Exception in an error occurs during the process.
	 */
	public void run() throws Exception
	{
		// Read queries
		ArrayList<Tuple<String, String>> queries = Batch.readQueries(this.pathToQueries);

		// Run the model with each query
		for (Tuple<String, String> query : queries) {
			String queryId = query.item1;
			String queryText = query.item2;

			ArrayList<Tuple<Integer, Double>> results = this.model.runQuery(queryText, this.index, this.docProcessor);
			this.printResults(results, queryId);
		}
	}

	/**
	 * Prints the results in TREC format to {@link System#out}.
	 *
	 * @param results the retrieval results. A list of {@link Tuple}s where the first item is the {@code docID} and the
	 *                second one is the similarty score.
	 * @param queryId the {@code queryID} to print in the results.
	 */
	protected void printResults(ArrayList<Tuple<Integer, Double>> results, String queryId)
	{
		for (int i = 0; i < results.size() && i < 500; i++) {
			String docName = this.index.documents.get(results.get(i).item1).item1;
			System.out.println(queryId + "\tQ0\t" + docName + "\t" + (i + 1) + "\t" + results.get(i).item2 + "\tsys");
		}
	}
}