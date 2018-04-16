/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface RegressionPrediction extends Prediction {

    public double getPrediction();
}
