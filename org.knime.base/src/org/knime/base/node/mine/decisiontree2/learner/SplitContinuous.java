/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   22.02.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.Iterator;

import org.knime.core.node.NodeLogger;

/**
 * This class determines the best split for a numeric attribute.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitContinuous extends Split {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SplitContinuous.class);

    private static final int BELOW_INDEX = 0;

    private static final int ABOVE_INDEX = 1;

    private double m_bestSplitValue;

    /**
     * The number of valid (non-missing) examples in the lower and upper
     * partition.
     */
    private double[] m_partitionValidCount;

    /**
     * Constructs the best split for the given numeric attribute list and the
     * class distribution. The results can be retrieved from getter methods.
     *
     * @param table the table with the data for which to create the split
     * @param attributeIndex the index of the attribute for which to create the
     *            split
     * @param splitQualityMeasure the quality measure (e.g. gini or gain
     *            ratio)
     * @param averageSplitpoint if true, the split point is set as the average
     *            of the partition borders, else the upper value of the lower
     *            partition is used
     * @param minObjectsCount the minimum number of objects in at least two
     *            partitions
     */
    public SplitContinuous(final InMemoryTable table, final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure,
            final boolean averageSplitpoint, final double minObjectsCount) {

        super(table, attributeIndex, splitQualityMeasure);
        findBestSplit(table, attributeIndex, splitQualityMeasure,
                averageSplitpoint, minObjectsCount);
    }

    /**
     * Constructs the best split for the given numeric attribute list and the
     * class distribution. The results can be retrieved from getter methods.
     *
     * @param table the table with the data for which to create the split
     * @param attributeIndex the index of the attribute for which to create the
     *            split
     * @param splitQualityMeasure the quality measure (e.g. gini or gain
     *            ratio)
     * @param averageSplitpoint if true, the split point is set as the average
     *            of the partition borders, else the upper value of the lower
     *            partition is used
     * @param minObjectsCount the minimumn number of objects in at least two
     *            partitions *
     * @param above the initial class distribution above this attribute list
     * @param below the inintial class distribution below this attribute list
     */
    private void findBestSplit(final InMemoryTable table,
            final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure,
            final boolean averageSplitpoint, final double minObjectsCount) {

        assert !table.isNominal(attributeIndex);

        if (LOGGER.isDebugEnabled()) {
            LOGGER
                    .debug("Attribute: "
                            + table.getAttributeName(attributeIndex));
        }

        // default value is the worst one
        setBestQualityMeasure(splitQualityMeasure.getWorstValue());

        // get the iterator for the data rows of the table
        // (NOTE: the missing values are at the end of the table)
        Iterator<DataRowWeighted> rowIterator = table.iterator();
        // if there are no rows return
        if (!rowIterator.hasNext()) {
            // set the quality measure to NaN marking as "not a valid split"
            setBestQualityMeasure(Double.NaN);
            return;
        }

        // now sort the table on this splits attribute index and get the
        // frequency array for the missing values
        // the missing value frequencies must be subtracted from the
        // counter
        double[] missingValueClassFrequencies =
                table.sortDataRows(attributeIndex);

        // the split is determined by sweeping linearly through the
        // ordered attribute list
        // two historgrams are maintained: one for the class distribution
        // below the current position (potential split point) and one histogram
        // for the class distribution above the current position
        // for all potential split points, the quality measure is calculates
        // and the best split according to this index is remembered
        double[][] partitionHisto = new double[2][];
        partitionHisto[ABOVE_INDEX] = table.getCopyOfClassFrequencyArray();
        double alloverMissingValueWeight = 0.0;
        for (int i = 0; i < missingValueClassFrequencies.length; i++) {
            partitionHisto[ABOVE_INDEX][i] -= missingValueClassFrequencies[i];
            alloverMissingValueWeight += missingValueClassFrequencies[i];
        }
        // initially the "below" histogram is set all fields to 0
        partitionHisto[BELOW_INDEX] =
                new double[partitionHisto[ABOVE_INDEX].length];

        // also the overall number of values in both partitions is maintained
        double alloverCount =
                table.getSumOfWeights() - alloverMissingValueWeight;
        double[] partitionCount = new double[2];
        partitionCount[ABOVE_INDEX] = alloverCount;
        partitionCount[BELOW_INDEX] = 0;

        // init the split quality measure
        m_splitQualityMeasure.initQualityMeasure(partitionHisto[ABOVE_INDEX],
                alloverCount);

        // Determine the minimum number of data rows at least required in each
        // partition.
        double minCount =
                0.1 * alloverCount
                        / table.getClassValueMapper().getNumMappings();
        if (minCount < minObjectsCount) {
            minCount = minObjectsCount;
        } else if (minCount > 25) {
            minCount = 25;
        }

        // check if there are too much missing cells
        if (alloverCount - alloverMissingValueWeight < 2 * minCount) {
            // set the quality measure to NaN marking as "not a valid split"
            setBestQualityMeasure(Double.NaN);
            return;
        }

        // get the first valid attribute value, the class value and its weight
        DataRowWeighted firstRow = rowIterator.next();
        double previouseAttrValue = firstRow.getValue(attributeIndex);
        int previousClassValue = firstRow.getClassValue();
        double weight = firstRow.getWeight();

        // to remember the best split
        // the best split value is the mean of the two split separating values
        // or the lower value (depends on the parameter "averageSplitPoint")
        double bestSplitValue = Double.NaN;
        double bestQualityMeasure = splitQualityMeasure.getWorstValue();
        m_partitionValidCount = new double[2];
        while (rowIterator.hasNext()) {
            // if the above part has too few rows terminate the loop
            if (partitionCount[ABOVE_INDEX] <= minCount) {
                break;
            }
            // adapt the below and above histogram with the previous value
            partitionHisto[BELOW_INDEX][previousClassValue] += weight;
            partitionHisto[ABOVE_INDEX][previousClassValue] -= weight;
            partitionCount[BELOW_INDEX] += weight;
            partitionCount[ABOVE_INDEX] -= weight;

            // get the next data row
            DataRowWeighted row = rowIterator.next();
            double attrValue = row.getValue(attributeIndex);
            if (Double.isNaN(attrValue)) {
                break;
            }
            int classValue = row.getClassValue();

            // the quality measure is only calculated if the value changes
            if (attrValue != previouseAttrValue
                    && partitionCount[BELOW_INDEX] >= minCount) {
                double qualityMeasure =
                        m_splitQualityMeasure.measureQuality(alloverCount,
                                partitionCount, partitionHisto,
                                alloverMissingValueWeight);
                // post process measure
                qualityMeasure =
                    m_splitQualityMeasure.postProcessMeasure(
                            qualityMeasure, alloverCount,
                            partitionCount, alloverMissingValueWeight);

                if (m_splitQualityMeasure.isBetterOrEqual(qualityMeasure,
                        bestQualityMeasure)) {
                    bestQualityMeasure = qualityMeasure;
                    // middle value as split value
                    if (averageSplitpoint) {
                        bestSplitValue =
                                previouseAttrValue / 2.0 + attrValue / 2.0;
                    } else {
                        bestSplitValue = previouseAttrValue;
                    }
                    bestQualityMeasure = qualityMeasure;
                    // also remember the partition counts
                    m_partitionValidCount[BELOW_INDEX] =
                            partitionCount[BELOW_INDEX];
                    m_partitionValidCount[ABOVE_INDEX] =
                            partitionCount[ABOVE_INDEX];
                }
            }

            // set the current values to the previouse ones
            previouseAttrValue = attrValue;
            previousClassValue = classValue;
            weight = row.getWeight();
        }

        setBestQualityMeasure(bestQualityMeasure);
        m_bestSplitValue = bestSplitValue;
    }

    // private String printCountStructures(final double allCount,
    // final double[] partitionCount, final double[][] histoCount) {
    // StringBuilder sb = new StringBuilder();
    // sb.append("All<" + +allCount + "> LowPart<"
    // + partitionCount[BELOW_INDEX] + "> AbovePart<"
    // + partitionCount[ABOVE_INDEX]);
    //
    // sb.append(" ClassHistoLow<");
    // for (double classCount : histoCount[BELOW_INDEX]) {
    // sb.append(classCount).append(",");
    // }
    // sb.append(">");
    //
    // sb.append(" ClassHistoAbove<");
    // for (double classCount : histoCount[ABOVE_INDEX]) {
    // sb.append(classCount).append(",");
    // }
    // sb.append(">");
    //
    // return sb.toString();
    // }

    /**
     * Returns the split value which was evaluated as the best according to the
     * induced partition purity.
     *
     * @return the best split value for the underlying attribute
     */
    public double getBestSplitValue() {
        return m_bestSplitValue;
    }

    /**
     * The number of partitions of a numeric split is always 2.
     *
     * {@inheritDoc}
     */
    @Override
    public int getNumberPartitions() {
        return 2;
    }

    /**
     * For numeric splits it makes sense to use the corresponding atribute in
     * deeper levels.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canBeFurtherUsed() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPartitionForRow(final DataRowWeighted row) {
        double value = row.getValue(getAttributeIndex());
        if (Double.isNaN(value)) {
            return -1;
        }
        if (value <= m_bestSplitValue) {
            return BELOW_INDEX;
        } else {
            return ABOVE_INDEX;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getPartitionWeights() {
        double[] weights = new double[2];
        double alloverCount =
                m_partitionValidCount[BELOW_INDEX]
                        + m_partitionValidCount[ABOVE_INDEX];
        weights[BELOW_INDEX] =
                m_partitionValidCount[BELOW_INDEX] / alloverCount;
        weights[ABOVE_INDEX] = 1.0 - weights[BELOW_INDEX];

        return weights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" Split at:").append(m_bestSplitValue);
        return sb.toString();
    }
}
