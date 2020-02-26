// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


/**
 * A processor to extract terms from HTML documents.
 */
public class HtmlProcessor implements DocumentProcessor
{
	protected File pathToStopWords;

	/**
	 * Creates a new HTML processor.
	 *
	 * @param pathToStopWords the path to the file with stopwords, or {@code null} if stopwords are not filtered.
	 * @throws IOException if an error occurs while reading stopwords.
	 */
	public HtmlProcessor(File pathToStopWords) throws IOException
	{
		// Load stopwords
		this.pathToStopWords = pathToStopWords;
	}

	/**
	 * {@inheritDoc}
	 */
	public Tuple<String, String> parse(String html)
	{
		// Parse document
		//TODO: Implement jsoup split into title and body
		
		Document doc = Jsoup.parse(html);

		String title = doc.title();
		String body = doc.body().text();
		Tuple<String, String> outputDoc = new Tuple<>(title, body);

		return outputDoc; // Return title and body separately
	}

	/**
	 * Process the given text (tokenize, normalize, filter stopwords and stemize) and return the list of terms to index.
	 *
	 * @param text the text to process.
	 * @return the list of index terms.
	 */
	public ArrayList<String> processText(String text)
	{
		ArrayList<String> tokens = this.tokenize(text);
		ArrayList<String> processedTokens = this.tokenize(text);
		
		for (String token : tokens) {
			if(!this.isStopWord(token)) {
				String normToken = this.normalize(token);
				//System.out.println(normToken);
				String stemmedToken = this.stem(normToken);
				System.out.println(normToken + " -- stemmed --> " + stemmedToken);
				processedTokens.add(stemmedToken);
			}
		}
		//System.err.println(tokens.get(0));
		//System.err.println("------------------------------------------------------------");
		
		return processedTokens;
	}

	/**
	 * Tokenize the given text.
	 *
	 * @param text the text to tokenize.
	 * @return the list of tokens.
	 */
	protected ArrayList<String> tokenize(String text)
	{
		List<String> tokens = Arrays.asList(text.replaceAll("[^a-zA-Z0-9']", " ").split("\\s+"));
		ArrayList<String> terms = new ArrayList<>();
		
        for (String token : tokens)
            if (token.length() > 4)
                terms.add(token);

		return terms;
	}

	/**
	 * Normalize the given term.
	 *
	 * @param text the term to normalize.
	 * @return the normalized term.
	 */
	protected String normalize(String text)
	{
		String normalized = text.toLowerCase();

		return normalized;
	}

	/**
	 * Checks whether the given term is a stopword.
	 *
	 * @param term the term to check.
	 * @return {@code true} if the term is a stopword and {@code false} otherwise.
	 */
	protected boolean isStopWord(String term)
	{
		boolean isStopWord = false;

		try {
		    Scanner scanner = new Scanner(this.pathToStopWords);

		    //now read the file line by line...
		    int lineNum = 0;
		    while (scanner.hasNextLine()) {
		        String line = scanner.nextLine();
		        lineNum++;
		        if(line.equals(term)) { 
		            System.out.println("stop word in line #" + lineNum + " --> " + term);
		            scanner.close();
		            return true;
		        }
		    }
		    scanner.close();
		} catch(FileNotFoundException e) { 
		    //throw e;
			System.err.println(e.getStackTrace());
		} finally {
			
		}

		return isStopWord;
	}

	/**
	 * Stem the given term.
	 *
	 * @param term the term to stem.
	 * @return the stem of the term.
	 */
	protected String stem(String term)
	{
		Stemmer stemmer = new Stemmer();
		char[] termChars = term.toCharArray();
		stemmer.add(termChars, termChars.length);
		stemmer.stem();
		String stem = stemmer.toString();
		return stem;
	}
}
