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
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEnd2NodeModel extends NodeModel implements LoopEndNode {

    /* Overall row count port 1 */
    private int m_count1 = 0;

    /* Overall row count port 2 */
    private int m_count2 = 0;

    /* Current iteration */
    private int m_iteration  = 0;

    private ConcatenateTableFactory[] m_tableFactories  = new ConcatenateTableFactory[2];

    private final LoopEnd2NodeSettings m_settings = new LoopEnd2NodeSettings();

    /** Creates a new model. */
    public LoopEnd2NodeModel() {
        super(2, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec0;
        if (m_settings.ignoreEmptyTables1() || m_settings.tolerateColumnTypes1() || m_settings.tolerateChangingTableSpecs1()) {
            spec0 = null;
        } else {
            ConcatenateTableFactory fac = new ConcatenateTableFactory(m_settings.ignoreEmptyTables1(),
                m_settings.tolerateColumnTypes1(), m_settings.addIterationColumn(), false, Optional.empty());
            spec0 = ConcatenateTableFactory.createSpec(inSpecs[0], m_settings.addIterationColumn(), false);
        }
        final DataTableSpec spec1;
        if (m_settings.ignoreEmptyTables2() || m_settings.tolerateColumnTypes2() || m_settings.tolerateChangingTableSpecs2()) {
            spec1 = null;
        } else {
            spec1 = ConcatenateTableFactory.createSpec(inSpecs[1], m_settings.addIterationColumn(), false);
        }
        return new DataTableSpec[]{spec0, spec1};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop end is not connected"
                   + " to matching/corresponding loop start node. You"
                   + "are trying to create an infinite loop!");
        }

        if(m_tableFactories[0] == null) {
            //first iteration -> create table factories
            Optional<Function<RowKey, RowKey>> rowKeyFunc1;
            Optional<Function<RowKey, RowKey>> rowKeyFunc2;
            switch(m_settings.rowKeyPolicy()) {
                case APPEND_SUFFIX:
                    rowKeyFunc1 = Optional.of(k -> {return new RowKey(k.toString() + "#" + (m_iteration));});
                    rowKeyFunc2 = Optional.of(k -> {return new RowKey(k.toString() + "#" + (m_iteration));});
                    break;
                case GENERATE_NEW:
                    rowKeyFunc1 = Optional.of(k -> {return new RowKey("Row" + (m_count1++));});
                    rowKeyFunc2 = Optional.of(k -> {return new RowKey("Row" + (m_count2++));});
                    break;
                case UNMODIFIED:
                default:
                    rowKeyFunc1 = Optional.empty();
                    rowKeyFunc2 = Optional.empty();
            }

            m_tableFactories[0] =
                new ConcatenateTableFactory(m_settings.ignoreEmptyTables1(), m_settings.tolerateColumnTypes1(),
                    m_settings.addIterationColumn(), m_settings.tolerateChangingTableSpecs1(), rowKeyFunc1);
            m_tableFactories[1] =
                new ConcatenateTableFactory(m_settings.ignoreEmptyTables2(), m_settings.tolerateColumnTypes2(),
                    m_settings.addIterationColumn(), m_settings.tolerateChangingTableSpecs2(), rowKeyFunc2);
        }

        //add tables to factories
        m_tableFactories[0].addTable(inData[0], exec);
        m_tableFactories[1].addTable(inData[1], exec);

        final boolean terminateLoop =
            ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        if (terminateLoop) {
            m_iteration = 0;
            m_count1 = 0;
            m_count2 = 0;
            BufferedDataTable[] outTables = new BufferedDataTable[2];
            outTables[0] = m_tableFactories[0].createTable(exec);
            outTables[1] = m_tableFactories[1].createTable(exec);
            return outTables;
        } else {
            continueLoop();
            m_iteration++;
            return new BufferedDataTable[2];
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_count1 = 0;
        m_count2 = 0;
        m_iteration = 0;
        Arrays.fill(m_tableFactories, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        AbstractLoopEndNodeSettings s = new LoopEndNodeSettings();
        s.loadSettings(settings);
    }
}
