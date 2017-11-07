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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.Collection;
import java.util.List;

/**
 * Abstract implementation for simple selection strategies (forward and backward).
 *
 * @author Adrian Nembach, KNIME.com
 */
public abstract class AbstractFeatureSelectionStrategy implements FeatureSelectionStrategy {

    private final int m_subsetSize;

    private final List<Integer> m_featureColumns;

    private boolean m_isMinimize;

    private double m_currentBestScore;

    private Integer m_currentBestFeature;

    private Integer m_lastBestFeature;

    private boolean m_shouldStop = false;

    /**
     * @param subSetSize subset size at which the search should stop.
     * @param features ids of the features.
     *
     */
    public AbstractFeatureSelectionStrategy(final int subSetSize, final List<Integer> features) {
        m_subsetSize = subSetSize;
        m_featureColumns = features;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getIncludedFeatures() {
        List<Integer> included = getIncludedInThisIteration();
        return included;
    }

    /**
     * @return the list of features that should be included in this iteration (without constant columns)
     */
    protected abstract List<Integer> getIncludedInThisIteration();

    /**
     * @return the list of currently fixed included columns
     */
    protected abstract List<Integer> getIncluded();


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
    protected abstract void handleBestFeature(Integer bestFeature);

    @Override
    public double getCurrentlyBestScore() {
        return m_currentBestScore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldAddFeatureLevel() {
        if (!reachedEndOfRound()) {
            nextFeature();
            return false;
        }

        handleBestFeature(m_currentBestFeature);
        List<Integer> included = getIncluded();
        m_shouldStop = included.size() == m_subsetSize || reachedEndOfSearch();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Integer> getFeatureLevel() {
        return getIncluded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareNewRound() {
        m_lastBestFeature = m_currentBestFeature;
        m_currentBestScore = initialBestScore();
        m_currentBestFeature = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScore(final double score) {
        if (newBestScore(score)) {
            m_currentBestFeature = getCurrentFeature();
        }

//        if (reachedEndOfRound()) {
//            handleBestFeature(m_currentBestFeature);
//            List<Integer> included = getIncluded();
//            m_shouldStop = included.size() == m_subsetSize || reachedEndOfSearch();
//        } else {
//            nextFeature();
//        }

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
     * @return list containing the ids of all features.
     */
    public List<Integer> getAllFeatures() {
        return m_featureColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getLastBestFeature() {
        return m_lastBestFeature;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfIterations() {
        final int numFeatures = m_featureColumns.size();
        if (m_subsetSize == -1) {
            return (numFeatures * (numFeatures + 1)) / 2;
        }
        return calcNumIterations(m_subsetSize, numFeatures);
    }

    /**
     * @param subsetSize subset size for which the search stops
     * @param numFeatures total number of features
     * @return the number of iterations the strategy needs to reach <b>subsetSize</b>
     */
    protected abstract int calcNumIterations(int subsetSize, int numFeatures);
}
