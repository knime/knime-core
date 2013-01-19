/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 * 
 * History
 *   06.09.2005 (bernd): created
 */
package org.knime.base.node.preproc.split;

import java.io.File;
import java.io.IOException;

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


/**
 * NodeModel with one input, two outputs. It splits the in table (column-based)
 * into a top and a bottom table.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
@Deprecated
public class SplitNodeModel extends NodeModel {
    /** DataCell Array of column names that build the top table. */
    public static final String CFG_TOP = "top";

    /** DataCell Array of column names that build the bottom table. */
    public static final String CFG_BOTTOM = "bottom";

    private String[] m_top;

    private String[] m_bottom;

    /**
     * Split node model with one data in-port and two data out-ports.
     */
    public SplitNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_top != null) {
            settings.addStringArray(CFG_TOP, m_top);
        }
        if (m_bottom != null) {
            settings.addStringArray(CFG_BOTTOM, m_bottom);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(CFG_TOP);
        settings.getStringArray(CFG_BOTTOM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_top = settings.getStringArray(CFG_TOP);
        m_bottom = settings.getStringArray(CFG_BOTTOM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec inSpec = in.getDataTableSpec();
        ColumnRearranger[] a = createColumnRearrangers(inSpec);
        BufferedDataTable[] outs = new BufferedDataTable[2];
        // exec won't be used here! It's just a wrapper.
        outs[0] = exec.createColumnRearrangeTable(in, a[0], exec);
        outs[1] = exec.createColumnRearrangeTable(in, a[1], exec);
        return outs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];
        if (m_top == null || m_bottom == null) {
            m_top = new String[in.getNumColumns()];
            for (int i = 0; i < in.getNumColumns(); i++) {
                m_top[i] = in.getColumnSpec(i).getName();
            }
            m_bottom = new String[0];
            setWarningMessage("No settings available, "
                    + "passing all columns to top output port.");
        }
        for (int i = 0; i < m_top.length; i++) {
            if (!in.containsName(m_top[i])) {
                throw new InvalidSettingsException("No such column: "
                        + m_top[i]);
            }
        }
        for (int i = 0; i < m_bottom.length; i++) {
            if (!in.containsName(m_bottom[i])) {
                throw new InvalidSettingsException("No such column: "
                        + m_bottom[i]);
            }
        }
        ColumnRearranger[] a = createColumnRearrangers(in);
        return new DataTableSpec[]{a[0].createSpec(), a[1].createSpec()};
    }

    private ColumnRearranger[] createColumnRearrangers(final DataTableSpec s) {
        ColumnRearranger topArrange = new ColumnRearranger(s);
        topArrange.keepOnly(m_top);
        ColumnRearranger bottomArrange = new ColumnRearranger(s);
        bottomArrange.keepOnly(m_bottom);
        return new ColumnRearranger[]{topArrange, bottomArrange};
    }
}
