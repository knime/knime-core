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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
class BinaryNominalSplitsPCA {

    /**
     * This nested class is intended to be only used within this class and in
     * TreeNominalColumnData#calcBestSplitClassificationBinaryPCA.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static class CombinedAttributeValues {
        final RealVector m_classFrequencyVector;

        final RealVector m_classProbabilityVector;

        double m_totalWeight;

        BigInteger m_bitMask;

        final List<NominalValueRepresentation> m_nomVals;

        double m_principalComponentScore;

        public CombinedAttributeValues(final double[] classFrequencyVector, final double[] classProbabilityVector,
            final double totalWeight, final NominalValueRepresentation initialNomVal) {
            m_classFrequencyVector = MatrixUtils.createRealVector(classFrequencyVector);
            m_classProbabilityVector = MatrixUtils.createRealVector(classProbabilityVector);
            m_nomVals = new ArrayList<NominalValueRepresentation>();
            m_nomVals.add(initialNomVal);
            m_totalWeight = totalWeight;
            m_bitMask = BigInteger.ZERO.setBit(initialNomVal.getAssignedInteger());
        }

        public void combineAttributeValues(final CombinedAttributeValues attVal) {
            if (!equals(attVal)) {
                throw new IllegalArgumentException(
                    "Only combine attributes if they have the same class probabilities.");
            }

            m_classFrequencyVector.add(attVal.m_classFrequencyVector);
            m_nomVals.addAll(attVal.m_nomVals);
            m_totalWeight += attVal.m_totalWeight;
            m_bitMask = m_bitMask.or(attVal.m_bitMask);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this.getClass() == obj.getClass()) {
                CombinedAttributeValues that = (CombinedAttributeValues)obj;
                if (m_classFrequencyVector.getDimension() == that.m_classFrequencyVector.getDimension()
                    && m_classProbabilityVector.getDimension() == that.m_classProbabilityVector.getDimension()) {
                    return m_classProbabilityVector.equals(that.m_classProbabilityVector);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(m_classProbabilityVector.toArray());
        }

        @Override
        public String toString() {
            return "CombinedAttributeValues[bitMask=" + m_bitMask.toString(2) + "]";
        }
    }

    /**
     * Calculates the mean class probability vector based on the class probability vectors of the
     * CombinedAttributeValues in attVals.
     *
     * @param attVals
     * @param totalWeight
     * @param numTargetVals
     * @return the mean class probability vector
     */
    static RealVector calculateMeanClassProbabilityVector(final CombinedAttributeValues[] attVals,
        final double totalWeight, final int numTargetVals) {
        RealVector meanClassProbabilityVec = MatrixUtils.createRealVector(new double[numTargetVals]);
        for (CombinedAttributeValues attVal : attVals) {
            meanClassProbabilityVec = meanClassProbabilityVec.add(attVal.m_classFrequencyVector);
        }
        meanClassProbabilityVec = meanClassProbabilityVec.mapDivide(totalWeight);
        return meanClassProbabilityVec;
    }

    /**
     * Calculates the weighted covariance matrix of the class probability vectors of the CombinedAttributeValues in
     * attVals
     *
     * @param attVals
     * @param meanClassProbabilityVec
     * @param totalWeight
     * @param numTargetVals
     * @return The weighted covariance matrix of the class probability vectors of the CombinedAttributeValues
     */
    static RealMatrix calculateWeightedCovarianceMatrix(final CombinedAttributeValues[] attVals,
        final RealVector meanClassProbabilityVec, final double totalWeight, final int numTargetVals) {
        RealMatrix weightedCovarianceMatrix = MatrixUtils.createRealMatrix(numTargetVals, numTargetVals);
        for (CombinedAttributeValues attVal : attVals) {
            RealVector diff = attVal.m_classProbabilityVector.subtract(meanClassProbabilityVec);
            weightedCovarianceMatrix =
                weightedCovarianceMatrix.add(diff.outerProduct(diff).scalarMultiply(attVal.m_totalWeight));
        }
        weightedCovarianceMatrix = weightedCovarianceMatrix.scalarMultiply(1 / (totalWeight - 1));

        return weightedCovarianceMatrix;
    }

    /**
     * Applies the PCA algorithm ("Partitioning Nominal Attributes in Decision Trees", Coppersmith et al. (1999)) to
     * order the attribute values according to their principal component score.
     *
     * @param attVals
     * @param totalWeight
     * @param numTargetVals
     * @return a CombinedAttributeValues array ordered by the principal component score of its elements.
     */
    static CombinedAttributeValues[] calculatePCAOrdering(final CombinedAttributeValues[] attVals,
        final double totalWeight, final int numTargetVals) {
        RealVector meanClassProbabilityVec = calculateMeanClassProbabilityVector(attVals, totalWeight, numTargetVals);
        RealMatrix weightedCovarianceMatrix =
            calculateWeightedCovarianceMatrix(attVals, meanClassProbabilityVec, totalWeight, numTargetVals);

        // eigenvalue decomposition
        EigenDecomposition eig;
        try {
            eig = new EigenDecomposition(weightedCovarianceMatrix);
        } catch (Exception e) {
            return null;
        }

        // get principal component
        RealVector principalComponent = eig.getEigenvector(argMax(eig.getRealEigenvalues()));

        // calculate principal component scores
        for (CombinedAttributeValues attVal : attVals) {
            attVal.m_principalComponentScore = principalComponent.dotProduct(attVal.m_classProbabilityVector);
        }

        // sort attribute values list ascending by principal component score
        Comparator<CombinedAttributeValues> pcScoreComparator = new Comparator<CombinedAttributeValues>() {

            @Override
            public int compare(final CombinedAttributeValues arg0, final CombinedAttributeValues arg1) {
                if (arg0.m_principalComponentScore < arg1.m_principalComponentScore) {
                    return -1;
                } else if (arg0.m_principalComponentScore > arg1.m_principalComponentScore) {
                    return 1;
                } else {
                    return 0;
                }
            }

        };
        CombinedAttributeValues[] result = Arrays.copyOf(attVals, attVals.length);
        Arrays.sort(result, pcScoreComparator);

        return result;
    }

    private static int argMax(final double[] array) {
        int index = 0;
        double max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                index = i;
                max = array[i];
            }
        }
        return index;
    }
}
