/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.base.node.mine.treeensemble2.node.statistics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.statistics.EnsembleStatistic;
import org.knime.base.node.mine.treeensemble2.statistics.TreeStatistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import com.google.common.collect.Lists;

/**
 * @author Adrian Nembach, KNIME.com
 */
class TreeEnsembleStatisticsNodeModel extends NodeModel {

    private static final String NUM_MODELS_NAME = "Number of models";
    private static final String MIN_LEVEL_NAME = "Minimal depth";
    private static final String MAX_LEVEL_NAME = "Maximal depth";
    private static final String AVG_LEVEL_NAME = "Average depth";
    private static final String MIN_NUM_NODES_NAME = "Minimal number of nodes";
    private static final String MAX_NUM_NODES_NAME = "Maximal number of nodes";
    private static final String AVG_NUM_NODES_NAME = "Average number of nodes";

    private static final String NUM_LEVELS_NAME = "Number of levels";
    private static final String NUM_NODES_NAME = "Number of nodes";


    protected TreeEnsembleStatisticsNodeModel() {
        super(new PortType[]{TreeEnsembleModelPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        TreeEnsembleModel treeEnsemble = ((TreeEnsembleModelPortObject)inObjects[0]).getEnsembleModel();
        EnsembleStatistic ensembleStats = new EnsembleStatistic(treeEnsemble);
        DataContainer containerEnsembleStats = exec.createDataContainer(createEnsembleStatsSpec());
        DataCell[] cells = new DataCell[7];
        cells[0] = new IntCell(treeEnsemble.getNrModels());
        cells[1] = new IntCell(ensembleStats.getMinLevel());
        cells[2] = new IntCell(ensembleStats.getMaxLevel());
        cells[3] = new DoubleCell(ensembleStats.getAvgLevel());
        cells[4] = new IntCell(ensembleStats.getMinNumNodes());
        cells[5] = new IntCell(ensembleStats.getMaxNumNodes());
        cells[6] = new DoubleCell(ensembleStats.getAvgNumNodes());
        containerEnsembleStats.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), cells));
        containerEnsembleStats.close();

        DataContainer containerTreeStats = exec.createDataContainer(createTreeStatsSpec());
        for (int i = 0; i < treeEnsemble.getNrModels(); i++) {
            DataCell[] treeCells = new DataCell[2];
            TreeStatistic treeStat = ensembleStats.getTreeStatistic(i);
            treeCells[0] = new IntCell(treeStat.getNumLevels());
            treeCells[1] = new IntCell(treeStat.getNumNodes());
            containerTreeStats.addRowToTable(new DefaultRow(RowKey.createRowKey((long)i), treeCells));
        }
        containerTreeStats.close();

        return new PortObject[] {(PortObject)containerEnsembleStats.getTable(), (PortObject)containerTreeStats.getTable()};
    }

    private DataTableSpec createEnsembleStatsSpec() {
        ArrayList<DataColumnSpec> ensembleStatsCols = Lists.newArrayList();
        ensembleStatsCols.add(new DataColumnSpecCreator(NUM_MODELS_NAME, IntCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(MIN_LEVEL_NAME, IntCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(MAX_LEVEL_NAME, IntCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(AVG_LEVEL_NAME, DoubleCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(MIN_NUM_NODES_NAME, IntCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(MAX_NUM_NODES_NAME, IntCell.TYPE).createSpec());
        ensembleStatsCols.add(new DataColumnSpecCreator(AVG_NUM_NODES_NAME, DoubleCell.TYPE).createSpec());
        return new DataTableSpec(ensembleStatsCols.toArray(new DataColumnSpec[ensembleStatsCols.size()]));
    }

    private DataTableSpec createTreeStatsSpec() {
        ArrayList<DataColumnSpec> treeStatsCols = Lists.newArrayList();
        treeStatsCols.add(new DataColumnSpecCreator(NUM_LEVELS_NAME, IntCell.TYPE).createSpec());
        treeStatsCols.add(new DataColumnSpecCreator(NUM_NODES_NAME, IntCell.TYPE).createSpec());
        return new DataTableSpec(treeStatsCols.toArray(new DataColumnSpec[treeStatsCols.size()]));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createEnsembleStatsSpec(), createTreeStatsSpec()};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

}
