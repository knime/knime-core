/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   24.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.Collection;
import java.util.Collections;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Serves as interface for the loop node models and manages the communication between column handler and
 * search strategy.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelector {

    private static final boolean ALWAYS_INCLUDE = true;

    private static final DataColumnSpec NUM_FEATURES = new DataColumnSpecCreator("Nr. of features", IntCell.TYPE).createSpec();

    private final FeatureSelectionStrategy m_strategy;

    private final AbstractColumnHandler m_colHandler;

    private double m_lastScore;

//    private boolean m_shouldAddRowToResultTable;

    private final FeatureSelectionModel m_selectionModel;

    private BufferedDataContainer m_resultTableContainer;

    /**
     * @param strategy Search strategy for this search (e.g. forward selection)
     * @param columnHandler Column Handler that is used to arrange the tables according to the strategy.
     */
    public FeatureSelector(final FeatureSelectionStrategy strategy, final AbstractColumnHandler columnHandler) {
        m_strategy = strategy;
        m_colHandler = columnHandler;
        m_selectionModel = new FeatureSelectionModel(columnHandler);
    }

    /**
     * Called by the FeatureSelectionLoopEndNodeModel <br>
     * Adds the <b>score</b> to the list of scores. <br>
     * Checks whether to add another feature level to the FeatureSelectionModel. <br>
     * Checks whether to stop the search.
     *
     * @param score
     */
    public void addScore(final double score) {
        // add score
        m_strategy.addScore(score);

        // add a new feature level if round is complete
        if (m_strategy.shouldAddFeatureLevel()) {
            m_lastScore = m_strategy.getCurrentlyBestScore();
            Collection<Integer> featureLevel = m_strategy.getFeatureLevel();
            m_selectionModel.addFeatureLevel(m_lastScore,
                featureLevel);
            m_strategy.prepareNewRound();
            if (m_resultTableContainer != null) {
                m_resultTableContainer.addRowToTable(getRowForResultTable());
            }
        }
    }

    /**
     * @return the feature selection model.
     */
    public FeatureSelectionModel getFeatureSelectionModel() {
        return m_selectionModel;
    }

    /**
     * Tells LoopEndNodeModel whether the loop should continue
     *
     * @return true or false depending on whether the search should go on
     */
    public boolean continueLoop() {
        return m_strategy.continueLoop();
    }

    /**
     * Sets whether the score should be minimized or maximized.
     * Used by the loop end node.
     *
     * @param isMinimize true if score should be minimized.
     */
    public void setIsMinimize(final boolean isMinimize) {
        m_selectionModel.setIsMinimize(isMinimize);
        m_strategy.setIsMinimize(isMinimize);
    }

    /**
     * Sets the name of the score for later use in the FeatureSelectionFilter node.
     * @param scoreName name of the score variable.
     */
    public void setScoreName(final String scoreName) {
        m_selectionModel.setScoreName(scoreName);
    }

    /**
     * @param inSpec {@link DataTableSpec} of the input table.
     * @return the outspec containing all feature columns of the current iteration.
     */
    public DataTableSpec getOutSpec(final DataTableSpec inSpec) {
        return m_colHandler.getOutSpec(m_strategy.getIncludedFeatures(), inSpec, ALWAYS_INCLUDE);
    }

    /**
     * Sets the result table container which records the search statistics.
     *
     * @param container {@link BufferedDataContainer} which should be filled with the search statistics.
     */
    public void setResultTableContainer(final BufferedDataContainer container) {
        m_resultTableContainer = container;
    }

    /**
     * @return the {@link DataTableSpec} for the search statistics table (second outport of FeatureSelectionLoopEnd node).
     */
    public DataTableSpec getSpecForResultTable() {
        DataColumnSpecCreator cc = new DataColumnSpecCreator(m_strategy.getNameForLastChange(), StringCell.TYPE);
        final DataColumnSpec lastChange = cc.createSpec();
        cc = new DataColumnSpecCreator(m_selectionModel.getScoreName(), DoubleCell.TYPE);
        final DataColumnSpec score = cc.createSpec();
        return new DataTableSpec("Result table", NUM_FEATURES, score, lastChange);
    }

    private DataRow getRowForResultTable() {
        final DataCell[] cells = new DataCell[3];
        final int featureLevelSize = m_strategy.getFeatureLevel().size();
        cells[0] = new IntCell(featureLevelSize);
        cells[1] = new DoubleCell(m_lastScore);
        final int changedFeature = m_strategy.getLastBestFeature();
        if (changedFeature == -1) {
            cells[2] = new StringCell("");
        } else {
            cells[2] = new StringCell(m_colHandler.getColumnNamesFor(Collections.singleton(changedFeature)).iterator().next());
        }
        final String rowId = featureLevelSize == m_colHandler.getAvailableFeatures().size() ? "All" : "" + featureLevelSize;
        return new DefaultRow(rowId, cells);
    }

    /**
     * Returns the tables for the next search round.
     * To be used by the loop start nod.
     *
     * @param exec {@link ExecutionContext} of the loop start node.
     * @param inTables The input tables of the loop start node.
     * @return Table containing the feature columns for the current round as well as the constant columns.
     * @throws CanceledExecutionException Thrown if the execution is canceled.
     */
    public BufferedDataTable[] getNextTables(final ExecutionContext exec, final BufferedDataTable[] inTables)
        throws CanceledExecutionException {
        return m_colHandler.getTables(exec, inTables, m_strategy.getIncludedFeatures(), ALWAYS_INCLUDE);
    }

    /**
     * @return The maximal number of iterations the current search has.
     */
    public int getNumberOfIterations() {
        return m_strategy.getNumberOfIterations();
    }

    /**
     * @return the name of the feature that is currently investigated.
     */
    public String getCurrentFeatureName() {
        final Integer feature = m_strategy.getCurrentFeature();
        // in case of backward elimination the first loop contains all columns
        if (feature.intValue() < 0) {
            return "";
        }
        return m_colHandler.getColumnNameFor(m_strategy.getCurrentFeature());
    }

}
