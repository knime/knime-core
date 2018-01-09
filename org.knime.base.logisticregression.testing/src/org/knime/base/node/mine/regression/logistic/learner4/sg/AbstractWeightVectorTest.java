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
 *   25.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Defines tests that must be passed by all classes extending the AbstractWeightVector class.
 *
 * @author Adrian Nembach, KNIME.com
 */
public abstract class AbstractWeightVectorTest {

    protected static double EPSILON = 1e-8;

    protected abstract WeightMatrix<MockClassificationTrainingRow> createTestVec(final boolean fitIntercept, final int nRows, final int nCols);

    protected abstract void testScale() throws Exception;

    @Test
    public void testUpdate() throws Exception {
        doTestUpdate(false, false);
        doTestUpdate(false, true);
        doTestUpdate(true, true);
        doTestUpdate(true, false);
    }

    private void doTestUpdate(
        final boolean vecFitIntercept, final boolean opFitIntercept) {
        WeightMatrix<MockClassificationTrainingRow> vec = createTestVec(vecFitIntercept, 3, 3);
        vec.update((v, c, i) -> 1, opFitIntercept);
        double[][] beta = vec.getWeightVector();
        for (int i = 0; i < beta.length; i++) {
            for (int j = 0; j < beta[i].length; j++) {
                if (j == 0 && !(vecFitIntercept && opFitIntercept)) {
                    assertEquals(0.0, beta[i][j], EPSILON);
                } else {
                    assertEquals(1.0, beta[i][j], EPSILON);
                }
            }
        }

        // set all entries to zero
        vec.update((v, c, i) -> 0, true);
        beta = vec.getWeightVector();
        double expectedIntercept = opFitIntercept && vecFitIntercept ? 1.0 : 0.0;
        double[][] expected = new double[][] {
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0}};
        assertArrayEquals(expected, beta);

        MockClassificationTrainingRow row = new MockClassificationTrainingRow(new double[]{0, 1}, 0, 0);
        vec.update((v, c, i) -> 0, true);
        vec.update((v, c, i, f) -> 1.0, opFitIntercept, row);
        beta = vec.getWeightVector();
        expected = new double[][] {
            {expectedIntercept, 0.0, 1.0},
            {expectedIntercept, 0.0, 1.0},
            {expectedIntercept, 0.0, 1.0}};
        assertArrayEquals(expected, beta);
    }

    @Test
    public void testPredict() throws Exception {
        WeightMatrix<MockClassificationTrainingRow> vec = createTestVec(true, 3, 4);
        vec.update((v, c, i) -> c + i, true);
        // [0 1 2 3; 1 2 3 4; 2 3 4 5]
        MockClassificationTrainingRow row = new MockClassificationTrainingRow(new double[]{1, 1, 1}, 0, 0);
        double[] expected = new double[]{6, 10, 14};
        predictAndCompare(row, expected, vec);

        row = new MockClassificationTrainingRow(new double[]{1, 0, 1}, 0, 0);
        expected = new double[]{4, 7, 10};
        predictAndCompare(row, expected, vec);

        row = new MockClassificationTrainingRow(new double[]{0, 2, 0}, 0, 0);
        expected = new double[]{4, 7, 10};
        predictAndCompare(row, expected, vec);
    }

    private void predictAndCompare(
        final MockClassificationTrainingRow row, final double[] expected,
        final WeightMatrix<MockClassificationTrainingRow> vec) throws Exception{
        double[] prediction = vec.predict(row);
        assertArrayEquals(expected, prediction, EPSILON);
    }

    @Test
    public void testNormalize() throws Exception {
        WeightMatrix<MockClassificationTrainingRow> vec = createTestVec(true, 3, 4);
        // create [0 1 2 3; 1 2 3 4; 2 3 4 5]
        vec.update((v, c , i) -> c + i, true);
        // scale by 5 (except first position) => [0 5 10 15; 1 10 15 20; 2 15 20 25]
        vec.scale(5.0);
        double[][] beta = vec.getWeightVector();
        double[][] expected = new double[][] {{0, 5, 10, 15}, {1, 10, 15, 20}, {2, 15, 20, 25}};
        assertArrayEquals(expected, beta);
    }

}
