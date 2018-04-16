/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface RandomForestRegressionPrediction extends RegressionPrediction, OutOfBagPrediction {

    /**
     * @return the variance of the predictions
     */
    double getVariance();
}
