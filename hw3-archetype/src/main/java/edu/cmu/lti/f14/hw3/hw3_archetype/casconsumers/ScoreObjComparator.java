package edu.cmu.lti.f14.hw3.hw3_archetype.casconsumers;

import java.util.Comparator;

import edu.cmu.lti.f14.hw3.hw3_archetype.casconsumers.RetrievalEvaluator.ScoreObj;

public class ScoreObjComparator implements Comparator<ScoreObj> {

  @Override
  public int compare(ScoreObj arg0, ScoreObj arg1) {
    // TODO Auto-generated method stub
    if (arg0.score < arg1.score)
        return 1;
    else if (arg0.score == arg1.score)
    {
      if (arg1.relevant==true)
        return 1;
      else
        return 0;
    }
    else
        return 0;
  }

}
