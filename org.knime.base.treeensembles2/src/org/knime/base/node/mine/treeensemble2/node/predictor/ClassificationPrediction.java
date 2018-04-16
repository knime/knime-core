/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface ClassificationPrediction extends Prediction {

    String getClassPrediction();

    int getWinningClassIdx();

    double getProbability(int classIdx);
}
