
package edu.cmu.lti.f14.hw3.hw3_archetype.annotators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f14.hw3.hw3_archetype.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_archetype.typesystems.Token;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
  HashSet<String> stopwordMap = new HashSet<String>();

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    // TODO Auto-generated method stub
    super.initialize(aContext);
    InputStream in2 = null;
    try {
      in2 = getContext().getResourceAsStream("stopword");
    } catch (ResourceAccessException e1) {
      e1.printStackTrace();
    }
    InputStreamReader is = new InputStreamReader(in2);
    BufferedReader br = new BufferedReader(is);
    String read;
    try {
      read = br.readLine();
      while (read != null) {
        stopwordMap.add(read.trim());
        read = br.readLine();
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * 
   * @param jcas
   * @param doc
   */
  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();
    boolean ToStem =false;
    for (String s: doc.split("\\s+"))
    {
      
      String str;
      if (ToStem == true)
      {
      Stemmer st = new Stemmer();
      st.add(s.toCharArray(), s.length());
      st.stem();
      s =st.toString();
      }
      res.add(s);
    }
    return res;
  }
  
  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();

    HashMap<String, Integer> hmap = new HashMap<String, Integer>();

    // List<String> terms = tokenize0(docText.toLowerCase().replaceAll(",.'?!-;\"", ""));
   
    
   List<String> terms = tokenize0(docText);

    
    for (String term : terms) {
      // System.out.println(stopwordMap.size());
      //if (stopwordMap.contains(term.toLowerCase().trim()))
      //  continue;
      
      if (hmap.containsKey(term)) {
        int termfreq = hmap.get(term);
        hmap.put(term, termfreq + 1);
      } else
        hmap.put(term, 1);
    }

    ArrayList<Token> tokenlist = new ArrayList<Token>();
    for (Entry<String, Integer> t : hmap.entrySet()) {
      Token token = new Token(jcas);
      token.setFrequency(t.getValue());
      token.setText(t.getKey());
      tokenlist.add(token);
    }

    FSList tokenFSList = FSCollectionFactory.createFSList(jcas, tokenlist);
    doc.setTokenList(tokenFSList);

  }

}
