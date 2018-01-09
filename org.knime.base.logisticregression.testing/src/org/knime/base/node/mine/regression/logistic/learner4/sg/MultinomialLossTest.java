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
 *   26.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.junit.Test;
import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;

/**
 * Contains unit tests for the {@link MultinomialLoss}.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class MultinomialLossTest {
    private static double EPSILON = 1e-3;

    @Test
    public void testEvaluate() throws Exception {
        final MultinomialLoss lossFunc = MultinomialLoss.INSTANCE;
        // the multinomial loss only requires the row to get to the target value
        MockClassificationTrainingRow row = new MockClassificationTrainingRow(new double[]{1, 1, 1}, 0, 0);
        double[] prediction = new double[]{3};
        double loss = lossFunc.evaluate(row, prediction);
        double expectedLoss = 0.0486;
        assertEquals(expectedLoss, loss, EPSILON);

        prediction = new double[]{-3};
        loss = lossFunc.evaluate(row, prediction);
        expectedLoss = 3.0486;
        assertEquals(expectedLoss, loss, EPSILON);

        prediction = new double[]{1,4};
        loss = lossFunc.evaluate(row, prediction);
        expectedLoss = 3.0659;
        assertEquals(expectedLoss, loss, EPSILON);

        row = new MockClassificationTrainingRow(new double[]{1,1,1}, 0, 2);
        prediction = new double[]{1,4};
        loss = lossFunc.evaluate(row, prediction);
        expectedLoss = 4.0659;
        assertEquals(expectedLoss, loss, EPSILON);

        row = new MockClassificationTrainingRow(new double[]{1,1,1}, 0, 2);
        prediction = new double[]{-1,-5};
        loss = lossFunc.evaluate(row, prediction);
        expectedLoss = 0.3182;
        assertEquals(expectedLoss, loss, EPSILON);
    }

    @Test
    public void testGradient() throws Exception {
        final MultinomialLoss lossFunc = MultinomialLoss.INSTANCE;
        // the multinomial loss only requires the row to get to the target value
        MockClassificationTrainingRow row = new MockClassificationTrainingRow(new double[]{1, 1, 1}, 0, 0);
        double[] prediction = new double[]{3};
        double[] gradient = lossFunc.gradient(row, prediction);
        double[] expectedGradient = new double[]{-0.0474};
        assertArrayEquals(expectedGradient, gradient, EPSILON);

        prediction = new double[]{-5};
        gradient = lossFunc.gradient(row, prediction);
        expectedGradient = new double[]{-0.9933};
        assertArrayEquals(expectedGradient, gradient, EPSILON);

        row = new MockClassificationTrainingRow(new double[]{1, 1, 1}, 0, 1);
        prediction = new double[]{-4};
        gradient = lossFunc.gradient(row, prediction);
        expectedGradient = new double[]{0.018};
        assertArrayEquals(expectedGradient, gradient, EPSILON);

        prediction = new double[]{2, 3};
        gradient = lossFunc.gradient(row, prediction);
        expectedGradient = new double[]{0.2595, -0.2946};
        assertArrayEquals(expectedGradient, gradient, EPSILON);
    }

    @Test
    public void testHessian() throws Exception {
        final MultinomialLoss lossFunc = MultinomialLoss.INSTANCE;
        // the multinomial loss only requires the row to get to the target value
        MockClassificationTrainingRow row = new MockClassificationTrainingRow(new double[]{2, 3}, 0, 0);
        final WeightMatrix<ClassificationTrainingRow> beta = new SimpleWeightMatrix<ClassificationTrainingRow>(3, 2, true);
        // [0 1 2; 1 2 3]
        beta.update((v, c, i) -> c + i, true);
        Iterator<ClassificationTrainingRow> mockIterator = mock(Iterator.class);
        when(mockIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(mockIterator.next()).thenReturn(row);
        TrainingData<ClassificationTrainingRow> mockData = mock(TrainingData.class);
        when(mockData.iterator()).thenReturn(mockIterator);
        when(mockData.getFeatureCount()).thenReturn(3);
        when(mockData.getTargetDimension()).thenReturn(2);

        double[][] hessian = lossFunc.hessian(mockData, beta);
        double p1 = Math.exp(8) / (1 + Math.exp(8) + Math.exp(14));
        double p2 = Math.exp(14) / (1 + Math.exp(8) + Math.exp(14));
        double p1_ = p1 * (1 - p1);
        double p2_ = p2 * (1 - p2);
        double p1p2 = p1 * p2;
        double[][] expected = new double[][]{
            {p1_, 2*p1_, 3*p1_, -p1p2, -2*p1p2, -3*p1p2},
            {2*p1_, 4*p1_, 6*p1_, -2*p1p2, -4*p1p2, -6*p1p2},
            {3*p1_, 6*p1_, 9*p1_, -3*p1p2, -6*p1p2, -9*p1p2},
            {-p1p2, -2*p1p2, -3*p1p2, p2_, 2*p2_, 3*p2_},
            {-2*p1p2, -4*p1p2, -6*p1p2, 2*p2_, 4*p2_, 6*p2_},
            {-3*p1p2, -6*p1p2, -9*p1p2, 3*p2_, 6*p2_, 9*p2_},
        };
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], hessian[i], EPSILON);
        }
    }
}
