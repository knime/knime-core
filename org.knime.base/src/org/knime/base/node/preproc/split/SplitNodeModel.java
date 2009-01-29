/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
