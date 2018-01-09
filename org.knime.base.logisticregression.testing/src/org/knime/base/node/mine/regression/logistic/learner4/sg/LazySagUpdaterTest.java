/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   28.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;

/**
 * Contains unit tests for LazySagUpdater
 *
 * @author Adrian Nembach, KNIME.com
 */
public class LazySagUpdaterTest {

    private static double EPSILON = 1e-5;

    @Test
    public void testLazyUpdate() throws Exception {
        WeightMatrix<TrainingRow> beta = new SimpleWeightMatrix<TrainingRow>(3, 2, true);
        MockClassificationTrainingRow[] mockRows = new MockClassificationTrainingRow[]{
            new MockClassificationTrainingRow(new double[]{0, 1}, 0, 0),
            new MockClassificationTrainingRow(new double[]{1, 0}, 1, 1),
            new MockClassificationTrainingRow(new double[]{2, 2}, 2, 0),
        };
        LazySagUpdater<TrainingRow> updater = (new LazySagUpdater.LazySagUpdaterFactory<>(3, 3, 2)).create();
        int[] lastVisited = new int[3];
        double[] gradient = new double[]{1.0,-1.0};
        updater.update(mockRows[0], gradient, beta, 1.0, 0);
        double[][] expectedBeta = new double[][]{
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        updater.lazyUpdate(beta, mockRows[1], lastVisited, 1);
        expectedBeta = new double[][]{
            {-1.0, 0.0, 0.0},
            {1.0, 0.0, 0.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        updater.update(mockRows[1], gradient, beta, 1.0, 1);
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        lastVisited[2] = 1;
        lastVisited[0] = 1;
        updater.lazyUpdate(beta, mockRows[0], lastVisited, 2);
        expectedBeta = new double[][]{
            {-2.0, 0.0, -0.5},
            {2.0, 0.0, 0.5}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        gradient = new double[] {-1.0, 1.0};
        updater.update(mockRows[0], gradient, beta, 2.0, 2);
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        lastVisited[1] = 2;
        lastVisited[0] = 2;
        updater.lazyUpdate(beta, mockRows[2], lastVisited, 3);
        expectedBeta = new double[][]{
            {-2.0, -1.0, 1.0},
            {2.0, 1.0, -1.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());
    }

    /**
     * Tests if the lazy updater produces the same beta as the eager updater.
     *
     * @throws Exception
     */
    @Test
    public void testLazyVsEager() throws Exception {
        lazyVsEager(1000, 40, 3, 100, 0.9);
        lazyVsEager(1000, 40, 2, 100, 0.5);
        lazyVsEager(1000, 40, 5, 100, 0.1);
    }

    private void lazyVsEager(final int nRows, final int nFeatures, final int nCats, final int nEpochs, final double fractionZeros) throws Exception{
        LazySagUpdater<MockClassificationTrainingRow> lazyUpdater = new LazySagUpdater.LazySagUpdaterFactory<MockClassificationTrainingRow>(nRows, nFeatures, nCats - 1).create();
        EagerSagUpdater<MockClassificationTrainingRow> eagerUpdater = new EagerSagUpdater.EagerSagUpdaterFactory<MockClassificationTrainingRow>(nRows, nFeatures, nCats - 1).create();
        SimpleWeightMatrix<MockClassificationTrainingRow> eagerBeta = new SimpleWeightMatrix<>(nFeatures, nCats - 1, true);
        SimpleWeightMatrix<MockClassificationTrainingRow> lazyBeta = new SimpleWeightMatrix<>(nFeatures, nCats - 1, true);
        MockClassificationTrainingRow[] rows = new MockClassificationTrainingRow[nRows];
        boolean[] columns2Check = new boolean[nFeatures];
        for (int i = 0; i < nRows; i++) {
            double[] features = new double[nFeatures - 1];
            for (int j = 0; j < nFeatures - 1; j++) {
                features[j] = Math.random() * 2 - 1;
                // set some features to zero
                if (Math.random() <= fractionZeros) {
                    features[j] = 0;
                }
            }
            int cat = (int) (Math.random() * nCats);
            rows[i] = new MockClassificationTrainingRow(features, i, cat);
        }

        double[] gradient = new double[nCats - 1];
        int[] lastVisited = new int[nFeatures];
        double stepSize = 1;
        for (int e = 0; e < nEpochs; e++) {
            for (int k = 0; k < nRows; k++) {
                MockClassificationTrainingRow row = rows[(int)(Math.random() * nRows)];
                lazyUpdater.lazyUpdate(lazyBeta, row, lastVisited, k);
                for (int j = 0; j < nCats - 1; j++) {
                    gradient[j] = Math.random() * 4 - 2;
                }
                for (FeatureIterator iter = row.getFeatureIterator(); iter.next();) {
                    lastVisited[iter.getFeatureIndex()] = k;
                }
                // if feature is present, the lazy update must update beta correctly
                checkPositional(lazyBeta.getWeightVector(), eagerBeta.getWeightVector(), columns2Check, EPSILON);
//                double stepSize = Math.random();
                lazyUpdater.update(row, gradient, lazyBeta, stepSize, k);
                eagerUpdater.update(row, gradient, eagerBeta, stepSize, k);
            }
            lazyUpdater.resetJITSystem(lazyBeta, lastVisited);
            checkEquality(lazyBeta.getWeightVector(), eagerBeta.getWeightVector(), EPSILON, "Epoch " + e);
        }
    }

    private void updateHistory(final double[][] gradientHistory, final double[] gradient, final int iteration) {
        for (int i = 0; i < gradient.length; i++) {
            gradientHistory[iteration][i] = gradient[i];
        }
    }

    private void updateHistory(final double[][][] betaHistory, final double[][] beta, final int iteration) {
        for (int i = 0; i < beta.length; i++) {
            for (int j = 0; j < beta[i].length; j++) {
                betaHistory[iteration][i][j] = beta[i][j];
            }
        }
    }

    private void checkPositional(final double[][] beta1, final double[][] beta2, final boolean[] columns2Check, final double epsilon) {
        assertEquals(beta1.length, beta2.length);
        for (int i = 0; i < beta1.length; i++) {
            assertEquals(beta1[i].length, beta2[i].length);
            for (int j = 0; j < beta1[i].length; j++) {
                if (columns2Check[j]) {
                    assertEquals("Arrays differed first at element [" + i + "][" + j + "]", beta1[i][j], beta2[i][j], epsilon);
                }
            }
        }
    }


    private void checkEquality(final double[][] matrix1, final double[][] matrix2, final double epsilon, final String errorPrefix) throws Exception {
        assertEquals(matrix1.length, matrix2.length);
        for (int i = 0; i < matrix1.length; i++) {
            assertArrayEquals(errorPrefix + " Row " + i + " doesn't match. ", matrix1[i], matrix2[i], epsilon);
        }
    }

    @Test
    public void testResetJITSystem() throws Exception {
        MockClassificationTrainingRow[] mockRows = new MockClassificationTrainingRow[]{
            new MockClassificationTrainingRow(new double[]{0, 1}, 0, 0),
            new MockClassificationTrainingRow(new double[]{1, 0}, 1, 1)};
        SimpleWeightMatrix<TrainingRow> beta = new SimpleWeightMatrix<>(3, 2, true);
        int[] lastVisited = new int[3];
        LazySagUpdater<TrainingRow> updater = new LazySagUpdater.LazySagUpdaterFactory<>(2, 3, 2).create();
        double stepSize = 1.0;
        double[] gradient = new double[] {1, 2};
        updater.lazyUpdate(beta, mockRows[0], lastVisited, 0);
        updater.update(mockRows[0], gradient, beta, stepSize, 0);

        updater.lazyUpdate(beta, mockRows[1], lastVisited, 1);
        updater.update(mockRows[1], gradient, beta, stepSize, 1);
        lastVisited[0] = 1;
        lastVisited[1] = 1;
        updater.resetJITSystem(beta, lastVisited);

        double[][] expectedBeta = new double[][] {
            {-2.0, -0.5, -1.5},
            {-4.0, -1.0, -3.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());
    }
}
