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
 *   18.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature Forward Selection. <br>
 * Starts from an empty feature set and iteratively adds the feature that optimizes the score.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FFSStrategy extends AbstractFeatureSelectionStrategy {

    private static final String COL_NAME = "Added feature";

    private final List<Integer> m_availableColumns;
    private final List<Integer> m_includedColumns;
    private int m_featurePointer = 0;

    /**
     * @param subsetSize
     * @param featureColumns
     *
     */
    public FFSStrategy(final int subsetSize, final List<Integer> featureColumns) {
        super(subsetSize, featureColumns);
        m_availableColumns = new ArrayList<>(getAllFeatures());
        m_includedColumns = new ArrayList<>(m_availableColumns.size());
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Integer> getIncludedInThisIteration() {
        final List<Integer> tempIncl = new ArrayList<>(m_includedColumns);
        tempIncl.add(getCurrentFeature());
        return tempIncl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Integer> getIncluded() {
        return m_includedColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getCurrentFeature() {
        return m_availableColumns.get(m_featurePointer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean reachedEndOfRound() {
        if (m_featurePointer == m_availableColumns.size() - 1) {
            m_featurePointer = 0;
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleBestFeature(final Integer bestFeature) {
        m_includedColumns.add(bestFeature);
        m_availableColumns.remove(bestFeature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextFeature() {
        m_featurePointer++;
        assert m_featurePointer < m_availableColumns.size();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean reachedEndOfSearch() {
        return m_availableColumns.isEmpty();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameForLastChange() {
        return COL_NAME;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected int calcNumIterations(final int subsetSize, final int numFeatures) {
        int numIterations = 0;
        for (int i = 0; i < subsetSize; i++) {
            numIterations += numFeatures - i;
        }
        return numIterations;
    }

}
