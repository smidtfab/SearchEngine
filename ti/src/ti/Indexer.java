// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * This class contains the logic to run the indexing process of the search engine.
 */
public class Indexer
{
    protected File pathToIndex;
    protected File pathToCollection;
    protected DocumentProcessor docProcessor;

	/**
     * Creates a new indexer with the given paths and document processor.
     * @param pathToIndex path to the index directory.
     * @param pathToCollection path to the original documents directory.
     * @param docProcessor document processor to extract terms.
     */
    public Indexer(File pathToIndex, File pathToCollection, DocumentProcessor docProcessor)
    {
        this.pathToIndex = pathToIndex;
        this.pathToCollection = pathToCollection;
        this.docProcessor = docProcessor;
    }

	/**
     * Run the indexing process in two passes and save the index to disk.
     * @throws IOException if an error occurs while indexing.
     */
    public void run() throws IOException
    {
        Index ind = new Index(this.pathToIndex.getPath());
        this.firstPass(ind);
        this.secondPass(ind);

        // Save index
        System.err.print("Saving index...");
        ind.save();
        System.err.println("done.");
        System.err.println("Index statistics:");
        ind.printStatistics();
    }
    /**
     * Runs the first pass of the indexer.
     * It builds the inverted index by iterating all original document files and calling {@link #processDocument}.
     * @param ind the index.
     * @throws IOException if an error occurs while processing a document.
     */
    protected void firstPass(Index ind) throws IOException
    {
        DecimalFormat df = new DecimalFormat("#.##");
        long startTime = System.currentTimeMillis();
        int totalDocuments = 0;
        long totalBytesDocuments = 0;

        System.err.println("Running first pass...");
        for (File subDir : this.pathToCollection.listFiles()) {
            if (!subDir.getName().startsWith(".")) {
                for (File docFile : subDir.listFiles()) {
                    if (docFile.getPath().endsWith(".html")) {
                        try {
                            System.err.print("  Indexing file " + docFile.getName() + "...");
                            this.processDocument(docFile, ind);
                            System.err.print("done.");
                        } catch (IOException ex) {
                            System.err.println("exception!");
                            System.err.print(ex.getMessage());
                        } finally {
                            System.err.println();
                        }
                        totalDocuments++;
                        totalBytesDocuments += docFile.length();
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000d;
        double totalMegabytes = totalBytesDocuments / 1024d / 1024d;
        System.err.println("...done:");
        System.err.println("  - Documents: " + totalDocuments + " (" + df.format(totalMegabytes) + " MB).");
        System.err.println("  - Time: " + df.format(totalTime) + " seconds.");
        System.err.println("  - Throughput: " + df.format(totalMegabytes / totalTime) + " MB/s.");
    }
    /**
     * Runs the second pass of the indexer.
     * Here it traverses the inverted index to compute and store IDF, update weights in the postings,
     * build the direct index, and compute document norms.
     * @param ind the index.
     */
    protected void secondPass(Index ind)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        long startTime = System.currentTimeMillis();

        System.err.println("Running second pass...");
        System.err.print("  Updating term weights and direct index...");
        // Traverse all terms to compute IDF, direct postings, and norm summations
        for (Map.Entry<String, Tuple<Integer, Double>> term : ind.vocabulary.entrySet()) {
            int termID = term.getValue().item1;

            // Lookup inverse postings list and compute IDF
            ArrayList<Tuple<Integer, Double>> postingsList = ind.invertedIndex.get(termID);
            double idf = Math.log(1.0 + (double) ind.documents.size() / postingsList.size());
            term.getValue().item2 = idf;

            // Traverse postings
            for (Tuple<Integer, Double> posting : postingsList) {
                int docID = posting.item1;
                double tf = posting.item2;
                // update weight
                posting.item2 = tf * idf;
                // add to doc norm
                ind.documents.get(docID).item2 += Math.pow(tf * idf, 2.0);
                // and add direct posting
                ind.directIndex.get(docID).add(new Tuple<>(termID, tf * idf));
            }
        }
        System.err.println("done.");

        System.err.print("  Updating document norms...");
        // Traverse all documents to root-square norms
        for (int docID = 0; docID < ind.documents.size(); docID++) {
            Tuple<String, Double> docInfo = ind.documents.get(docID);
            docInfo.item2 = Math.sqrt(docInfo.item2);
        }

        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000d;
        System.err.println("done.");
        System.err.println("...done");
        System.err.println("  - Time: " + df.format(totalTime) + " seconds.");
    }
	/**
     * Process the original document in the specified path and add it to the given index.
     * <p>
     * After extracting the document terms, it populates the vocabulary and document structures,
     * and adds the corresponding postings to the inverted index.
     * @param docFile the path to the original document file.
     * @param ind the index to add the document to.
     * @throws IOException if an error occurs while processing this document.
     */
    protected void processDocument(File docFile, Index ind) throws IOException
    {
        String html = new String(Files.readAllBytes(docFile.toPath()));
       
        Tuple<String, String> parsed = this.docProcessor.parse(html);
        System.out.println(parsed.item1);
        ArrayList<String> titleTerms = this.docProcessor.processText(parsed.item1);
        

        ArrayList<String> allTerms = this.docProcessor.processText(parsed.item2);
        
        // Add document entries
        String docName = docFile.getName().replace(".html", "");
        int docID = ind.documents.size();
        ind.documents.add(new Tuple<>(docName, 0d));
        ind.setCachedDocument(docID, new Tuple<>(parsed.item1.replaceAll("\\s+", " "), parsed.item2.replaceAll("\\s+", " ")));

        HashSet<String> uniqTerms = new HashSet<>(allTerms);
        ind.directIndex.add(new ArrayList<Tuple<Integer, Double>>(uniqTerms.size()));
        for (String term : uniqTerms) {
            // Lookup term info
            Tuple<Integer, Double> termInfo = ind.vocabulary.get(term);
            if (termInfo == null) {
                // New term: add entry to vocabulary and inverted index
                termInfo = new Tuple<>(ind.vocabulary.size(), 0d);
                ind.vocabulary.put(term, termInfo);
                ind.invertedIndex.add(new ArrayList<Tuple<Integer, Double>>());
            }
            int termID = termInfo.item1;

            // Compute weight and add posting
            double tf = 1.0 + Math.log(Collections.frequency(allTerms, term));
            ind.invertedIndex.get(termID).add(new Tuple<>(docID, tf));
        }
    }
}
