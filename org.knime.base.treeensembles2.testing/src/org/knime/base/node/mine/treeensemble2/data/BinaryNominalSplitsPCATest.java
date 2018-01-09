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
 *   26.11.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.BinaryNominalSplitsPCA;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.BinaryNominalSplitsPCA.CombinedAttributeValues;

/**
 *
 * @author Adrian Nembach
 */
public class BinaryNominalSplitsPCATest {

    private static CombinedAttributeValues[] createTestAttVals() {
        CombinedAttributeValues[] attVals = new CombinedAttributeValues[5];
        double[][] classFrequencies =
            new double[][]{{40, 10, 10}, {10, 40, 10}, {20, 30, 10}, {20, 15, 25}, {10, 5, 45}};
        double[][] classProbabilities = new double[5][3];
        double totalWeight = 60;
        String[] nomValStrings = new String[]{"A", "B", "C", "D", "E"};
        NominalValueRepresentation[] nomVals = new NominalValueRepresentation[5];
        for (int i = 0; i < 5; i++) {
            nomVals[i] = new NominalValueRepresentation(nomValStrings[i], i, totalWeight);
            for (int j = 0; j < 3; j++) {
                classProbabilities[i][j] = classFrequencies[i][j] / totalWeight;
            }
        }

        for (int i = 0; i < 5; i++) {
            attVals[i] =
                new CombinedAttributeValues(classFrequencies[i], classProbabilities[i], totalWeight, nomVals[i]);
        }

        return attVals;
    }

    @Test
    public void testCalculateMeanClassProbabilityVector() {
        final CombinedAttributeValues[] attVals = createTestAttVals();
        final double totalSumWeight = 300;
        final int numTargetVals = 3;

        final RealVector meanClassProbabilityVector =
            BinaryNominalSplitsPCA.calculateMeanClassProbabilityVector(attVals, totalSumWeight, numTargetVals);

        final double aThird = 1.0 / 3.0;
        final RealVector expectedMeanClassProbabilityVector =
            MatrixUtils.createRealVector(new double[]{aThird, aThird, aThird});

        assertEquals(expectedMeanClassProbabilityVector, meanClassProbabilityVector);
    }

    @Test
    public void testCalculateWeightedCovarianceMatrix() {
        final CombinedAttributeValues[] attVals = createTestAttVals();
        final double totalSumWeight = 300;
        final int numTargetVals = 3;
        final RealVector meanClassProbabilityVector =
            BinaryNominalSplitsPCA.calculateMeanClassProbabilityVector(attVals, totalSumWeight, numTargetVals);

        RealMatrix weightedCovarianceMatrix = BinaryNominalSplitsPCA.calculateWeightedCovarianceMatrix(attVals,
            meanClassProbabilityVector, totalSumWeight, numTargetVals);
        // the reference matrix is altered to be easily readable therefore we have to do the same to the calculated matrix
        weightedCovarianceMatrix = weightedCovarianceMatrix.scalarMultiply(1 / weightedCovarianceMatrix.getEntry(0, 0));
        weightedCovarianceMatrix = weightedCovarianceMatrix.scalarMultiply(10);

        final RealMatrix expectedWeightedCovarianceMatrix = MatrixUtils
            .createRealMatrix(new double[][]{{10.0, -4.167, -5.833}, {-4.167, 14.167, -10.0}, {-5.833, -10.0, 15.833}});

        // RealMatrix does overwrite equals but all entries must be exactly the same for two matrices to be equal
        // Therefore we need to use the asserEquals method that allows to define a delta
        assertEquals(expectedWeightedCovarianceMatrix.getRowDimension(), weightedCovarianceMatrix.getRowDimension());
        assertEquals(expectedWeightedCovarianceMatrix.getColumnDimension(), weightedCovarianceMatrix.getColumnDimension());
        for (int r = 0; r < weightedCovarianceMatrix.getRowDimension(); r++) {
            for (int c = 0; c < weightedCovarianceMatrix.getColumnDimension(); c++) {
                assertEquals(expectedWeightedCovarianceMatrix.getEntry(r, c), weightedCovarianceMatrix.getEntry(r, c),
                    0.001);
            }
        }
    }

    @Test
    public void testCalculatePCAOrdering() {
        final CombinedAttributeValues[] attVals = createTestAttVals();
        final double totalSumWeight = 300;
        final int numTargetVals = 3;

        final CombinedAttributeValues[] result = BinaryNominalSplitsPCA.calculatePCAOrdering(attVals, totalSumWeight, numTargetVals);

        // We have to reverse the order that the example got. This is because they altered the weightedCovarianceMatrix.
        // Another possibility is, that our first principal component is the opposite direction of the principal component in the example
        // the reason for that is the eigen decomposition we used. But those differences should not impact the quality of the results of the algorithm.
        final CombinedAttributeValues[] expected = new CombinedAttributeValues[] {attVals[4], attVals[3], attVals[0], attVals[2], attVals[1]};
        assertEquals(expected.length, result.length);
        // CombinedAttributeValues overwrites equals for a special purpose so we need to explicitly check equality here
        for (int i = 0; i < result.length; i++) {
            assertEquals(expected[i].m_bitMask, result[i].m_bitMask);
            assertEquals(expected[i].m_classFrequencyVector, result[i].m_classFrequencyVector);
            assertEquals(expected[i].m_classProbabilityVector, result[i].m_classProbabilityVector);
            assertEquals(expected[i].m_totalWeight, result[i].m_totalWeight, 0);
            // m_nomVals is not checked because it is only used to compute the bitmask and therefore
            // two instances have equal m_nomVals if their m_bitMask is equal
        }
    }
}
