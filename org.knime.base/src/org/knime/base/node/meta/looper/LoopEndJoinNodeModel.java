/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 12, 2010 (wiswedel): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.joiner.Joiner;
import org.knime.base.node.preproc.joiner.Joiner2Settings;
import org.knime.base.node.preproc.joiner.Joiner2Settings.CompositionMode;
import org.knime.base.node.preproc.joiner.Joiner2Settings.DuplicateHandling;
import org.knime.base.node.preproc.joiner.Joiner2Settings.JoinMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Loop End Node that joins the input table with the previous input
 * (colum wise concatenation).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LoopEndJoinNodeModel extends NodeModel implements LoopEndNode {

    /** See bug 6544 - columns 51+ always had " (Iter #1)" appended. True only for deprecated node. */
    private final boolean m_appendIterSuffixForBackwardComp;
    private LoopEndJoinNodeConfiguration m_configuration;
    private BufferedDataTable m_currentAppendTable;
    private int m_iteration = 0;

    LoopEndJoinNodeModel(final boolean appendIterSuffixForBackwardComp6544) {
        super(1, 1);
        m_appendIterSuffixForBackwardComp = appendIterSuffixForBackwardComp6544;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_configuration == null) {
            // auto-guess, dialog added in v2.4
            m_configuration = new LoopEndJoinNodeConfiguration();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        boolean hasSameRowsInEachIteration =
            m_configuration.hasSameRowsInEachIteration();
        LoopStartNode startNode = getLoopStartNode();
        if (!(startNode instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop end is not connected"
                   + " to matching/corresponding loop start node. You"
                   + " are trying to create an infinite loop!");
        }
        boolean continueLoop =
            !((LoopStartNodeTerminator)startNode).terminateLoop();
        if (m_currentAppendTable == null) {
            m_currentAppendTable = copy(inData[0], false, exec);
        } else if (hasSameRowsInEachIteration) {
            boolean isCacheNew = m_iteration % 50 == 0;
            double amount = isCacheNew ? (1.0 / 3.0) : (1.0 / 2.0);
            ExecutionContext copyCtx = exec.createSubExecutionContext(amount);
            ExecutionContext joinCtx = exec.createSubExecutionContext(amount);
            exec.setProgress("Copying input");
            BufferedDataTable t = copy(inData[0], true, copyCtx);
            copyCtx.setProgress(1.0);
            exec.setProgress("Joining with previous input");
            m_currentAppendTable = exec.createJoinedTable(
                    m_currentAppendTable, t, joinCtx);
            joinCtx.setProgress(1.0);
            if (isCacheNew) {
                exec.setProgress("Caching intermediate results (iteration "
                        + m_iteration + ")");
                ExecutionContext ctx = exec.createSubExecutionContext(amount);
                //copy the whole table every 50 columns (avoids wrapping to much individual tables)
                //In this case the whole table is copied and column names DON'T need to be made unique (bugfix 6544)
                m_currentAppendTable = copy(m_currentAppendTable, m_appendIterSuffixForBackwardComp, ctx);
                ctx.setProgress(1.0);
            }
        } else {
            Joiner2Settings settings = new Joiner2Settings();
            settings.setCompositionMode(CompositionMode.MatchAll);
            settings.setDuplicateColumnSuffix(" (Iter #" + m_iteration + ")");
            settings.setDuplicateHandling(DuplicateHandling.AppendSuffix);
            settings.setEnableHiLite(false);
            // joining on RowIDs, this should not generate new row IDs but
            // only fill missing rows in either table
            settings.setJoinMode(JoinMode.FullOuterJoin);
            settings.setLeftIncludeAll(true);
            settings.setRightIncludeAll(true);
            // TODO to be replaced by Joiner2Settings.ROW_KEY_IDENTIFIER
            // once that is public
            settings.setLeftJoinColumns(new String[] {"$RowID$"});
            settings.setRightJoinColumns(new String[] {"$RowID$"});
            BufferedDataTable left = m_currentAppendTable;
            BufferedDataTable right = copy(inData[0], true,
                    exec.createSubExecutionContext(0.1));
            Joiner joiner = new Joiner(left.getDataTableSpec(),
                    right.getDataTableSpec(), settings);
            m_currentAppendTable = joiner.computeJoinTable(left, right,
                    exec.createSubExecutionContext(0.9));
        }
        m_iteration += 1;
        if (continueLoop) {
            super.continueLoop();
            return null;
        } else {
            return new BufferedDataTable [] {m_currentAppendTable};
        }
    }



    /**
     * Copies the given table into a new data container. If desired, the column names are made unique beforehand (see
     * {@link #uniqueColumnNames(DataTableSpec)}.
     */
    private BufferedDataTable copy(final BufferedDataTable table, final boolean uniqueColumnNames,
            final ExecutionContext exec) throws CanceledExecutionException {

        DataTableSpec spec;
        if(uniqueColumnNames) {
            spec = uniqueColumnNames(table.getDataTableSpec());
        } else {
            spec = table.getDataTableSpec();
        }
        BufferedDataContainer container = exec.createDataContainer(spec);
        int i = 0;
        final double rowCount = table.size();
        for (DataRow r : table) {
            container.addRowToTable(r);
            exec.setProgress((i++) / rowCount, "Copied row " + i + "/"
                    + rowCount + " (\"" + r.getKey() + "\")");
            exec.checkCanceled();
        }
        container.close();
        return container.getTable();
    }

    /**
     * Returns a column spec with unique column names with respect to the already collected columns
     * (m_currentAppendTable). An "Iter # [m_iteration]" is appended to the duplicate column names.
     */
    private DataTableSpec uniqueColumnNames(final DataTableSpec spec) {
        DataColumnSpec[] colSpecs = new DataColumnSpec[spec.getNumColumns()];
        int i = 0;
        for (DataColumnSpec cs : spec) {
            if ((m_currentAppendTable != null) && m_currentAppendTable.getDataTableSpec().containsName(cs.getName())) {
                String newName = cs.getName() + " (Iter #" + m_iteration + ")";
                colSpecs[i++] = new DataColumnSpecCreator(newName, cs.getType()).createSpec();
            } else {
                colSpecs[i++] = cs;
            }
        }
        return new DataTableSpec(colSpecs);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_currentAppendTable = null;
        m_iteration = 0;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LoopEndJoinNodeConfiguration c = new LoopEndJoinNodeConfiguration();
        c.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LoopEndJoinNodeConfiguration c = new LoopEndJoinNodeConfiguration();
        c.loadConfigurationInModel(settings);
        m_configuration = c;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

}
