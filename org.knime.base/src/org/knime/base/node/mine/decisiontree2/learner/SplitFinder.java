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
 *   01.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.NodeLogger;

/**
 * Finds the best split for a given {@link InMemoryTable}. The results can be
 * retrieved via getter methdods.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitFinder {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SplitFinder.class);

    private int m_splitAttributeIndex;

    private Split m_split;

    /**
     * Finds the best split for the given data.
     *
     * @param table the data table for which to find the best split attribute
     *            and for this the best split
     * @param splitQualityMeasure the quality measure (e.g. gini or gain
     *            ratio)
     * @param averageSplitpoint if true, the split point is set as the average
     *            of the partition borders, else the upper value of the lower
     *            partition is used
     * @param minObjectsCount minimum number of examples for a partition
     * @param binaryNominalSplits if true, nominal attributes are split
     *            according to binary subsets, else each nominal value
     *            represents one branch
     * @param maxNumNominalsForCompleteComputation the maximum number of nominal
     *            values for which all subsets are calculated (results in the
     *            optimal binary split); this parameter is only use if
     *            <code>binaryNominalSplits</code> is <code>true</code>; if
     *            the number of nominal values is higher, a heuristic is applied
     */
    public SplitFinder(final InMemoryTable table,
            final SplitQualityMeasure splitQualityMeasure,
            final boolean averageSplitpoint, final double minObjectsCount,
            final boolean binaryNominalSplits,
            final int maxNumNominalsForCompleteComputation) {

        // create the best splits for each attribute
        List<Split> splitCandidates = new ArrayList<Split>();
        long time = 0;
        if (LOGGER.isInfoEnabled()) {
            time = System.currentTimeMillis();
            LOGGER.debug("Find best split for all attributes...");
        }

        for (int i = 0; i < table.getNumAttributes(); i++) {
            // check if the attribute should be considered
            if (!table.considerAttribute(i)) {
                continue;
            }
            if (table.isNominal(i)) {
                if (binaryNominalSplits) {
                    splitCandidates.add(new SplitNominalBinary(table, i,
                            splitQualityMeasure, minObjectsCount,
                            maxNumNominalsForCompleteComputation));
                } else {
                    splitCandidates.add(new SplitNominalNormal(table, i,
                            splitQualityMeasure, minObjectsCount));
                }
            } else {
                splitCandidates.add(new SplitContinuous(table, i,
                        splitQualityMeasure, averageSplitpoint,
                        minObjectsCount));
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Split points for #rows<" + table.getNumberDataRows()
                    + "> found in: " + (System.currentTimeMillis() - time));
        }

        // get the best split
        Split bestSplit = null;
        double bestQualityMeasure = splitQualityMeasure.getWorstValue();
        int bestIndex = -1;
        int index = 0;
        for (Split splitCandidate : splitCandidates) {
            LOGGER.debug("Split candidate: "
                    + splitCandidate.getSplitAttributeName()
                    + " best measure: "
                    + splitCandidate.getBestQualityMeasure());
            if (splitCandidate.isValidSplit()
                    && splitQualityMeasure.isBetterOrEqual(splitCandidate
                            .getBestQualityMeasure(), bestQualityMeasure)) {
                bestQualityMeasure = splitCandidate.getBestQualityMeasure();
                bestSplit = splitCandidate;
                bestIndex = index;
            }
            index++;
        }

        m_splitAttributeIndex = bestIndex;
        m_split = bestSplit;
        if (LOGGER.isDebugEnabled()) {
            if (m_split == null) {
                LOGGER.debug("No split could be evaluated.");
            } else {
                LOGGER
                        .debug("Best split: " + m_split.getSplitAttributeName()
                                + " best measure: "
                                + m_split.getBestQualityMeasure());
            }
        }

    }

    /**
     * Returns the split evaluated as the best for the given data.
     *
     * @return the split evaluated as the best for the given data
     */
    public Split getSplit() {
        return m_split;
    }

    /**
     * Returns the attribute list that determines the split.
     *
     * @return the attribute list that determines the split
     */
    public int getSplitAttributeIndex() {
        return m_splitAttributeIndex;
    }
}
