package edu.cmu.lti.f14.hw3.hw3_archetype.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_archetype.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_archetype.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_archetype.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {
  class ScoreObj {
    int qid;

    double score;

    String docContent;

    int rank;

    public boolean relevant;
  }

  /** query id **/
  public ArrayList<Integer> qIdList;

  /** save relevant documents for each query **/
  public HashMap<Integer, ArrayList<Integer>> relList;

  /** save document frequency for each term **/
  public HashMap<String, Integer> df;

  /** save documents with hashmap type by query ID **/
  public HashMap<Integer, ArrayList<HashMap<String, Integer>>> qIdToDocList;

  /** save each documents content by query ID **/
  public HashMap<Integer, ArrayList<String>> docContenttList;

  /** number of docs **/
  private int totdocNum;

  /** total length of docs **/
  private int totdoclength;

  String SimilarityFunction = "CosineSimilarity";

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new HashMap<Integer, ArrayList<Integer>>();

    docContenttList = new HashMap<Integer, ArrayList<String>>();

    df = new HashMap<String, Integer>();

    qIdToDocList = new HashMap<Integer, ArrayList<HashMap<String, Integer>>>();

    totdocNum = 1;

    totdoclength = 0;
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    totdocNum++;
    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      HashMap<String, Integer> tmap = new HashMap<String, Integer>();
      Iterator<Token> iter = tokenList.iterator();
      while (iter.hasNext()) {
        Token t = iter.next();
        tmap.put(t.getText(), t.getFrequency());

        // count document frequency
        if (!df.containsKey(t.getText()))
          df.put(t.getText(), 1);
        else {
          df.put(t.getText(), df.get(t.getText()) + 1);
        }
      }

      // count total length of documents
      totdoclength += doc.getText().length();

      qIdList.add(doc.getQueryID());

      // if the list for the query had been created
      if (qIdToDocList.containsKey(doc.getQueryID())) {
        ArrayList<HashMap<String, Integer>> alist = qIdToDocList.get(doc.getQueryID());
        ArrayList<String> dlist = docContenttList.get(doc.getQueryID());
        if (doc.getRelevanceValue() == 99) {
          alist.add(0, tmap);
          // put the query document in the first of list
          dlist.add(0, doc.getText());

          // if there is relevant document existing in reList for now query, record the new position
          // of relevant document
          if (relList.containsKey(doc.getQueryID())) {
            ArrayList<Integer> rlist = relList.get(doc.getQueryID());
            ArrayList<Integer> tempilist = new ArrayList<Integer>();
            for (int i = 0; i < rlist.size(); i++)
              tempilist.add(rlist.get(0) + 1);
            relList.put(doc.getQueryID(), tempilist);
          }
        } else {
          alist.add(tmap);
          dlist.add(doc.getText());

        }
      } else {
        ArrayList<HashMap<String, Integer>> alist = new ArrayList<HashMap<String, Integer>>();
        ArrayList<String> dlist = new ArrayList<String>();
        alist.add(tmap);
        dlist.add(doc.getText());
        qIdToDocList.put(doc.getQueryID(), alist);
        docContenttList.put(doc.getQueryID(), dlist);
      }

      // process relevant document
      if (doc.getRelevanceValue() == 1) {
        int nowSize = qIdToDocList.get(doc.getQueryID()).size();
        if (relList.containsKey(doc.getQueryID())) {
          ArrayList<Integer> arr = relList.get(doc.getQueryID());
          arr.add(nowSize - 1);
          relList.put(doc.getQueryID(), arr);
        } else {
          ArrayList<Integer> arr = new ArrayList<Integer>();
          arr.add(nowSize - 1);
          relList.put(doc.getQueryID(), arr);
        }
      }
    }

  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    ArrayList<ScoreObj> finallist = new ArrayList<ScoreObj>();
    double queryScore = 0;
    int queryCount = 0;

    for (Entry<Integer, ArrayList<HashMap<String, Integer>>> docs : qIdToDocList.entrySet()) {
      ArrayList<ScoreObj> slist = new ArrayList<ScoreObj>();
      ArrayList<Integer> arr = relList.get(docs.getKey());

      double cosScore = 0;
      // TODO :: compute the cosine similarity measure
      for (int i = 1; i < docs.getValue().size(); i++) {

        int doclength = docContenttList.get(docs.getKey()).get(i).length();

        if (SimilarityFunction.equals("CosineSimilarity"))
          cosScore = computeCosineSimilarity(docs.getValue().get(0), docs.getValue().get(i));
        else if (SimilarityFunction.equals("TFIDF"))
          cosScore = computeTFIDFCosineSimilarity(docs.getValue().get(0), docs.getValue().get(i));
        else if (SimilarityFunction.equals("BM25"))
          cosScore = computeBM25(docs.getValue().get(0), docs.getValue().get(i), (double) doclength);

        ScoreObj sobj = new ScoreObj();
        sobj.rank = i;
        sobj.score = cosScore;
        if (arr.contains(i))
          sobj.relevant = true;
        else
          sobj.relevant = false;
        slist.add(sobj);

      }

      // Ranked lists by Score
      Collections.sort(slist, new ScoreObjComparator());
      // TODO :: compute the rank of retrieved sentences
      for (int i = 0; i < slist.size(); i++)
        System.out.println("cos=" + slist.get(i).score + " rank=" + (i + 1) + " qid="
                + docs.getKey() + " " + docContenttList.get(docs.getKey()).get(slist.get(i).rank));
      for (int i = 0; i < slist.size(); i++) {
        if (arr.contains(slist.get(i).rank)) {
          ScoreObj sobj = new ScoreObj();
          sobj.qid = docs.getKey();
          sobj.rank = i + 1;
          sobj.score = slist.get(i).score;
          sobj.docContent = docContenttList.get(docs.getKey()).get(slist.get(i).rank);
          queryScore += 1 / (double) sobj.rank;
          queryCount++;
          finallist.add(sobj);
          break;
        }
      }
    }

    // Ranked lists by ID
    Collections.sort(finallist, new ScoreObjQIDComparator());

    // TODO :: compute the metric:: mean reciprocal rank
    BufferedWriter oFile2 = null;
    oFile2 = new BufferedWriter(new FileWriter(new File("src/main/resources/data/report.txt")));
    DecimalFormat dformat = new DecimalFormat("0.0000");
    for (int i = 0; i < finallist.size(); i++) {
      ScoreObj sobj = finallist.get(i);
      oFile2.write("cos=" + dformat.format(sobj.score) + "\trank=" + sobj.rank + "\tqid="
              + sobj.qid + "\trel=1\t" + sobj.docContent + "\n");
      System.out.print((1 / (double) sobj.rank) + " ");
    }

    double metric_mrr = compute_mrr(queryScore, queryCount);
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
    oFile2.write("MRR=" + dformat.format(metric_mrr));
    oFile2.close();
  }

  /**
   * Computed CosineSimilarity score by given two hashmaps
   * 
   * @return score of CosineSimilarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosScore = 0;
    for (Entry<String, Integer> q : queryVector.entrySet()) {
      String key = q.getKey();
      // System.out.println(q.getKey()+ " "+q.getValue());
      if (docVector.containsKey(key)) {
        cosScore += docVector.get(key) * queryVector.get(key);
      }
    }
    double qDis = 0;
    for (Entry<String, Integer> entry : queryVector.entrySet()) {
      qDis += Math.pow((double) entry.getValue(), 2);
    }
    qDis = Math.sqrt(qDis);

    double docDis = 0;
    for (Entry<String, Integer> entry : docVector.entrySet()) {
      docDis += Math.pow((double) entry.getValue(), 2);
    }

    docDis = Math.sqrt(docDis);

    // TODO :: compute cosine similarity between two sentences

    return cosScore / (docDis * qDis);
  }

  /**
   * Computed TfIdf score by given two hashmaps
   * 
   * @return score of TfIdf
   */

  private double computeTFIDFCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosScore = 0;
    for (Entry<String, Integer> q : queryVector.entrySet()) {
      String key = q.getKey();
      if (docVector.containsKey(key)) {
        cosScore += (docVector.get(key) * Math.log(totdocNum / (df.get(key) + 1)))
                * (queryVector.get(key) * Math.log(totdocNum / (df.get(key) + 1)));
      }
    }
    double qDis = 0;
    for (Entry<String, Integer> entry : queryVector.entrySet()) {
      qDis += Math.pow((double) entry.getValue(), 2)
              * Math.log(totdocNum / (df.get(entry.getKey()) + 1));
    }
    qDis = Math.sqrt(qDis);

    double docDis = 0;
    for (Entry<String, Integer> entry : docVector.entrySet()) {
      docDis += Math.pow((double) entry.getValue(), 2)
              * Math.log(totdocNum / (df.get(entry.getKey()) + 1));
    }

    docDis = Math.sqrt(docDis);

    // TODO :: compute cosine similarity between two sentences

    return cosScore / (docDis * qDis);
  }

  /**
   * Computed BM25 score by given two hashmaps
   * 
   * @return score of BM25
   */

  private double computeBM25(Map<String, Integer> queryVector, Map<String, Integer> docVector,
          double doclength) {
    double b = 0.75;
    double k1 = 1.6;

    double averageDoclen = ((double) totdoclength / (double) (totdocNum - 1));
    double bmScore = 0;
    for (Entry<String, Integer> q : queryVector.entrySet()) {
      String key = q.getKey();
      if (docVector.containsKey(key)) {
        double IDF = Math.log((totdocNum - df.get(key) + 0.5) / (df.get(key) + 0.5));
        double TF = docVector.get(key) * (k1 + 1)
                / (docVector.get(key) + k1 * (1 - b + b * (doclength / averageDoclen)));
        bmScore += IDF * TF;

      }
    }

    // TODO :: compute cosine similarity between two sentences

    return bmScore;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(double queryScore, int queryNum) {

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

    return queryScore / (double) queryNum;
  }

}
