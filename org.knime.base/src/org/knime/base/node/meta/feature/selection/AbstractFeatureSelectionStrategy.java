/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public abstract class AbstractFeatureSelectionStrategy implements FeatureSelectionStrategy {

    private final int m_subsetSize;

    private final List<String> m_featureColumns;

    private final List<String> m_constantColumns;

    private FeatureSelectionModel m_selectionModel;

    private boolean m_isMinimize;

    private String m_scoreName;

    private double m_currentBestScore;

    private String m_currentBestFeature;

    private boolean m_shouldStop = false;

    /**
     *
     */
    public AbstractFeatureSelectionStrategy(final int subSetSize, final String[] constantColumns,
        final String[] featureColumns) {
        m_subsetSize = subSetSize;
        m_featureColumns = Arrays.asList(featureColumns);
        m_constantColumns = Arrays.asList(constantColumns);
        m_selectionModel = new FeatureSelectionModel(constantColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getIncludedColumns() {
        List<String> included = getIncludedInThisIteration();
        included.addAll(m_constantColumns);
        return included;
    }

    /**
     * @return the list of features that should be included in this iteration (without constant columns)
     */
    protected abstract List<String> getIncludedInThisIteration();

    /**
     * @return the list of currently fixed included columns
     */
    protected abstract List<String> getIncluded();

    /**
     * @return the currently looked at feature
     */
    protected abstract String getCurrentFeature();

    /**
     * @return true if the end of this search round is reached
     */
    protected abstract boolean reachedEndOfRound();

    /**
     * @return true if the end of the complete search is reached
     */
    protected abstract boolean reachedEndOfSearch();

    /**
     * go to the next feature
     */
    protected abstract void nextFeature();

    /**
     * Handle the best feature (probably adjust some lists)
     *
     * @param bestFeature the feature that won this search round
     */
    protected abstract void handleBestFeature(String bestFeature);

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScore(final double score) {
        if (newBestScore(score)) {
            m_currentBestFeature = getCurrentFeature();
        }

        if (reachedEndOfRound()) {
            handleBestFeature(m_currentBestFeature);
            List<String> included = getIncluded();
            m_selectionModel.addFeatureLevel(m_currentBestScore, included);
            m_shouldStop = included.size() == m_subsetSize || reachedEndOfSearch();
            m_currentBestScore = initialBestScore();
            m_currentBestFeature = null;
        } else {
            nextFeature();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean continueLoop() {
        return !m_shouldStop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureSelectionModel getFeatureSelectionModel() {
        m_selectionModel.setScoreName(m_scoreName);
        m_selectionModel.setIsMinimize(m_isMinimize);
        return m_selectionModel;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setIsMinimize(final boolean isMinimize) {
        m_isMinimize = isMinimize;
        m_currentBestScore = initialBestScore();
    }

    private double initialBestScore() {
        return m_isMinimize ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
    }

    private boolean newBestScore(final double score) {
        if (m_isMinimize ? score < m_currentBestScore : score > m_currentBestScore) {
            m_currentBestScore = score;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScoreName(final String scoreName) {
        m_scoreName = scoreName;
    }

    public List<String> getAllFeatures() {
        return m_featureColumns;
    }

}
