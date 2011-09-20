/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   16.12.2009 (meinl): created
 */
package org.knime.base.node.meta.looper.columnlist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This is the model for the column list loop start node that does the real
 * work.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnListLoopStartNodeModel extends NodeModel implements
        LoopStartNodeTerminator {
    private final ColumnListLoopStartSettings m_settings =
            new ColumnListLoopStartSettings();

    private int m_currentColIndex = 0;

    private boolean m_lastIteration;

    /**
     * Creates a new model with one data out- and inport, respectively.
     */
    public ColumnListLoopStartNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if ((m_settings.iterateOverColumns().size() < 1)
                && !m_settings.iterateAllColumns()) {
            throw new InvalidSettingsException(
                    "No columns to iterate over selected");
        }

        if (!m_settings.iterateAllColumns()) {
            for (String col : m_settings.iterateOverColumns()) {
                if (!inSpecs[0].containsName(col)) {
                    throw new IllegalArgumentException("Column '" + col
                            + "' does not exist in input table");
                }
            }
        }

        ColumnRearranger crea = createRearranger(inSpecs[0]);

        return new DataTableSpec[]{crea.createSpec()};
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec) {
        String[] includedCols;
        if (m_settings.iterateAllColumns()) {
            includedCols =
                    new String[]{inSpec.getColumnSpec(m_currentColIndex)
                            .getName()};
            pushFlowVariableString("currentColumnName",
                    inSpec.getColumnSpec(m_currentColIndex).getName());
        } else {
            List<String> temp = new ArrayList<String>();
            for (String s : m_settings.alwaysIncludeColumns()) {
                if (inSpec.containsName(s)) {
                    temp.add(s);
                }
            }

            includedCols = new String[temp.size() + 1];
            int i = 0;
            for (String s : temp) {
                includedCols[i++] = s;
            }

            includedCols[i] =
                    m_settings.iterateOverColumns().get(m_currentColIndex);
            pushFlowVariableString("currentColumnName", m_settings
                    .iterateOverColumns().get(m_currentColIndex));
        }

        ColumnRearranger crea = new ColumnRearranger(inSpec);



        crea.keepOnly(includedCols);
        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec());

        m_currentColIndex++;
        if (m_settings.iterateAllColumns()) {
            m_lastIteration =
                    m_currentColIndex >= inData[0].getDataTableSpec()
                            .getNumColumns();
        } else {
            m_lastIteration =
                    m_currentColIndex >= m_settings.iterateOverColumns().size();
        }

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], crea, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currentColIndex = 0;
        m_lastIteration = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_lastIteration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnListLoopStartSettings s = new ColumnListLoopStartSettings();
        s.loadSettings(settings);

        if (s.iterateOverColumns().size() < 1) {
            throw new InvalidSettingsException(
                    "No columns to iterate over selected");
        }

        List<String> l = new ArrayList<String>();
        l.addAll(s.iterateOverColumns());
        if (l.retainAll(l)) {
            throw new InvalidSettingsException(
                    "Columns to iterate over must not be included always");
        }
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }
}
