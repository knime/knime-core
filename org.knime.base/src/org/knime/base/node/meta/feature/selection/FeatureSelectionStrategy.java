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
 *
 * @author Adrian Nembach, KNIME.com
 */
public interface FeatureSelectionStrategy {

    /**
     * @return true if the loop should be continued
     */
    public boolean continueLoop();

    /**
     * @return the features that are included
     */
    public List<Integer> getIncludedFeatures();

    /**
     * Adds a new score for one subset
     *
     * @param score
     */
    public void addScore(final double score);

    /**
     * Sets whether the score should be minimized
     *
     * @param isMinimize
     */
    public void setIsMinimize(final boolean isMinimize);

    /**
     * @return true if a new feature level needs to be added (usually after the current search round is complete)
     */
    public boolean shouldAddFeatureLevel();

    /**
     * @return the best score for the current search round
     */
    public double getCurrentlyBestScore();

    /**
     * setup for the next search round
     */
    public void prepareNewRound();

    /**
     * This method is usually called after one search round is completed.
     *
     * @return the last fixed feature level.
     */
    public Collection<Integer> getFeatureLevel();

    /**
     * @return the name for the column that documents what happened during the last search step
     */
    public String getNameForLastChange();

    /**
     * @return the feature that was identified as best feature in the latest search round
     */
    public Integer getLastBestFeature();

    /**
     * @return the maximal number of iterations this strategy performs for the current configuration.
     */
    public int getNumberOfIterations();

    /**
     * @return index of the feature that is currently investigated.
     */
    public Integer getCurrentFeature();

}
