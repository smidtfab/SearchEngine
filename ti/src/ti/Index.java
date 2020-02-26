// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

import java.io.*;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class represents the index of the search engine.
 * <p>
 * The structures it holds are: the term and document information, an inverse index, a direct index, and a cached version of the documents.
 * <p>
 * The index can be loaded from and saved to some specified directory.
 */
public class Index
{
    protected final int DOCS_PER_CACHE_BLOCK = 20;

    protected String path;
    /**
     * The vocabulary of terms in the index.
     * <p>
     * {@code [term] -> (termID, IDF)}
     * <p>
     * It maps a term {@link String} onto a {@link Tuple} containing the {@code termID} and its IDF score.
     */
    public HashMap<String, Tuple<Integer, Double>> vocabulary; // [term] -> (termID, IDF)
    /**
     * The list of documents in the index.
     * <p>
     * {@code [docID] -> (docName, norm)}
     * <p>
     * The {@code i}-th element is a {@link Tuple} corresponding to the document with {@code docID=i}.
     * The {@link Tuple} contains the {@code name} of the document and its vector {@code norm}.
     */
    public ArrayList<Tuple<String, Double>> documents; // [docID] -> (docName, norm)
    /**
     * The inverted index.
     * <p>
     * {@code [termID] -> (docID, weight)+}
     * </p>
     * The {@code i}-th element corresponds to the postings list of the term with {@code termID=i}.
     * Each posting is a {@link Tuple} containing a {@code docID} and the {@code weight} of the term in that document.
     */
    public ArrayList<ArrayList<Tuple<Integer, Double>>> invertedIndex; // [termID] -> (docID, weight)+
    /**
     * The direct index.
     * <p>
     * {@code [docID] -> (termID, weight)+}
     * <p>
     * The {@code i}-th element corresponds to the postings list of the document with {@code docID=i}.
     * Each posting is a {@link Tuple} containing a {@code termID} and the {@code weight} of that term in the document.
     */
    public ArrayList<ArrayList<Tuple<Integer, Double>>> directIndex; // [docID] -> (termID, weight)+

    /**
     * Creates a new index to be loaded from or stored in the specified directory.
     * @param path the directory to store the index files.
     */
    public Index(String path)
    {
        this.path = path;
        this.vocabulary = new HashMap<>();
        this.documents = new ArrayList<>();
        this.invertedIndex = new ArrayList<>();
        this.directIndex = new ArrayList<>();
    }

    /**
     * Returns the cached version of the specified document.
     * @param docID the ID of the document.
     * @return a {@link Tuple} containing the document title and its body.
     * @throws Exception if an error occurs while accessing the cache.
     */
    public Tuple<String,String> getCachedDocument(int docID) throws Exception
    {
        int block = docID % this.DOCS_PER_CACHE_BLOCK;
        File blockPath = Paths.get(this.path, "cache"+block).toFile();
        File filePath = new File(blockPath, docID+"");

        ObjectInput ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filePath)));
        String title = (String)ois.readObject();
        String body = (String)ois.readObject();
        ois.close();

        return new Tuple<>(title, body);
    }
    /**
     * Sets the cached version of the specified document.
     * @param docID the ID of the document.
     * @param docText a {@link Tuple} containing the document title and its body.
     *                @throws IOException  if an error occurs while accessing the cache.
     */
    public void setCachedDocument(int docID, Tuple<String,String> docText) throws IOException
    {
        int block = docID % this.DOCS_PER_CACHE_BLOCK;
        File blockPath = Paths.get(this.path, "cache"+block).toFile();
        if(!blockPath.exists())
            blockPath.mkdirs();

        File filePath = new File(blockPath, docID+"");
        ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filePath)));
        oos.writeObject(docText.item1);
        oos.writeObject(docText.item2);
        oos.close();
    }

    /**
     * Loads the index from the path specified in the {@link Index#Index constructor}.
     * @throws Exception if an error occurs while loading the index.
     */
    public void load() throws Exception
    {
        // Vocabulary
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Paths.get(this.path, "vocabulary").toFile()));
        int count = ois.readInt();
        this.vocabulary = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String term = ois.readUTF();
            int termID = ois.readInt();
            double idf = ois.readDouble();
            this.vocabulary.put(term, new Tuple<>(termID, idf));
        }
        ois.close();
        // Documents
        ois = new ObjectInputStream(new FileInputStream(Paths.get(this.path, "documents").toFile()));
        count = ois.readInt();
        this.documents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String docName = ois.readUTF();
            double norm = ois.readDouble();
            this.documents.add(i, new Tuple<>(docName, norm));
        }
        ois.close();
        // Inverted
        ois = new ObjectInputStream(new FileInputStream(Paths.get(this.path, "inverted").toFile()));
        count = ois.readInt();
        this.invertedIndex = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int count2 = ois.readInt();
            ArrayList<Tuple<Integer, Double>> entry = new ArrayList<>(count2);
            for (int i2 = 0; i2 < count2; i2++) {
                int item1 = ois.readInt();
                double item2 = ois.readDouble();
                entry.add(i2, new Tuple<>(item1, item2));
            }
            this.invertedIndex.add(i, entry);
        }
        ois.close();
        // Direct
        ois = new ObjectInputStream(new FileInputStream(Paths.get(this.path, "direct").toFile()));
        count = ois.readInt();
        this.directIndex = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int count2 = ois.readInt();
            ArrayList<Tuple<Integer, Double>> entry = new ArrayList<>(count2);
            for (int i2 = 0; i2 < count2; i2++) {
                int item1 = ois.readInt();
                double item2 = ois.readDouble();
                entry.add(i2, new Tuple<>(item1, item2));
            }
            this.directIndex.add(i, entry);
        }
        ois.close();
    }
    /**
     * Saves the index to the path specified in the {@link Index#Index constructor}.
     * @throws IOException if an error occurs while saving the index.
     */
    public void save() throws IOException
    {
        File di = new File(this.path);
        if (!di.exists())
            di.mkdir();

        // Vocabulary
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Paths.get(this.path, "vocabulary").toFile()));
        oos.writeInt(this.vocabulary.size());
        for (Map.Entry<String, Tuple<Integer, Double>> entry : this.vocabulary.entrySet()) {
            oos.writeUTF(entry.getKey());
            oos.writeInt(entry.getValue().item1);
            oos.writeDouble(entry.getValue().item2);
        }
        oos.close();
        // Documents
        oos = new ObjectOutputStream(new FileOutputStream(Paths.get(this.path, "documents").toFile()));
        oos.writeInt(this.documents.size());
        for (Tuple<String, Double> entry : this.documents) {
            oos.writeUTF(entry.item1);
            oos.writeDouble(entry.item2);
        }
        oos.close();
        // Inverted
        oos = new ObjectOutputStream(new FileOutputStream(Paths.get(this.path, "inverted").toFile()));
        oos.writeInt(this.invertedIndex.size());
        for (ArrayList<Tuple<Integer, Double>> entry : this.invertedIndex) {
            oos.writeInt(entry.size());
            for (Tuple<Integer, Double> entry2 : entry) {
                oos.writeInt(entry2.item1);
                oos.writeDouble(entry2.item2);
            }
        }
        oos.close();
        // Direct
        oos = new ObjectOutputStream(new FileOutputStream(Paths.get(this.path, "direct").toFile()));
        oos.writeInt(this.directIndex.size());
        for (ArrayList<Tuple<Integer, Double>> entry : this.directIndex) {
            oos.writeInt(entry.size());
            for (Tuple<Integer, Double> entry2 : entry) {
                oos.writeInt(entry2.item1);
                oos.writeDouble(entry2.item2);
            }
        }
        oos.close();
    }
    /**
     * Prints statistics about the index to {@link System#err}.
     * The statistics include the number of terms in the vocabulary and its size, the number of documents and the size
     * of the cache, and the size of the direct and inverted indexes.
     */
    public void printStatistics()
    {
        DecimalFormat df = new DecimalFormat("#.##");

        System.err.print("  - Vocabulary: " + this.vocabulary.size() + " terms");
        File file = Paths.get(this.path, "vocabulary").toFile();
        if (file.exists())
            System.err.print(" (" + df.format(file.length() / 1024d / 1024d) + " MB)");
        System.err.println(".");

        System.err.print("  - Documents: " + this.documents.size() + " documents");
        file = Paths.get(this.path, "documents").toFile();
        if (file.exists())
            System.err.print(" (" + df.format(file.length() / 1024d) + " KB)");
        System.err.println(".");

        file = Paths.get(this.path, "inverted").toFile();
        if (file.exists())
            System.err.println("  - Inverted: " + df.format(file.length() / 1024d / 1024d) + " MB.");

        file = Paths.get(this.path, "direct").toFile();
        if (file.exists())
            System.err.println("  - Direct: " + df.format(file.length() / 1024d / 1024d) + " MB.");

        long cacheSize = 0;
        for(int block = 0; block < this.DOCS_PER_CACHE_BLOCK; block++){
            File blockPath = Paths.get(this.path, "cache"+block).toFile();
            if(blockPath.exists())
                for(File docFile : blockPath.listFiles())
                    cacheSize += docFile.length();
        }
        System.err.println("  - Cache: " + df.format(cacheSize / 1024d / 1024d) + " MB.");
    }
}