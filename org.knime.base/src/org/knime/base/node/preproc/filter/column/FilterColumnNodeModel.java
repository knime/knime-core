/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.filter.column;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
 * The model for the column filter which extracts certain columns from the input
 * {@link org.knime.core.data.DataTable} using a list of columns to
 * exclude.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
final class FilterColumnNodeModel extends NodeModel {

    /**
     * The input port used here.
     */
    static final int INPORT = 0;

    /**
     * The output port used here.
     */
    static final int OUTPORT = 0;

    /**
     * the excluded settings.
     */
    static final String KEY = "exclude";

    /** Contains all column names to exclude. */
    private final ArrayList<String> m_list;

    /**
     * Creates a new filter model with one and in- and output.
     */
    FilterColumnNodeModel() {
        super(1, 1);
        m_list = new ArrayList<String>();
    }

    /**
     * Resets the internal list of columns to exclude.
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger c = createColumnRearranger(data[0].getDataTableSpec());
        BufferedDataTable outTable = exec.createColumnRearrangeTable(data[0],
                c, exec);
        return new BufferedDataTable[]{outTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to be done
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to be done
    }

    /**
     * Excludes a number of columns from the input spec and generates a new
     * output spec.
     * 
     * @param inSpecs the input table spec
     * @return outSpecs the output table spec with some excluded columns
     * 
     * @throws InvalidSettingsException if the selected column is not available
     *             in the table spec.
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs != null);
        ColumnRearranger c = createColumnRearranger(inSpecs[INPORT]);
        return new DataTableSpec[]{c.createSpec()};

    }

    /**
     * Creates the output data table spec according to the current settings.
     * Throws an InvalidSettingsException if columns are specified that don't
     * exist in the input table spec.
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_list.isEmpty()) {
            super.setWarningMessage("All columns retained.");
            return new ColumnRearranger(inSpec);
        }
        // check if all specified columns exist in the input spec
        for (String name : m_list) {
            if (!inSpec.containsName(name)) {
                throw new InvalidSettingsException("Column '" + name
                        + "' not found.");
            }
        }
        // counter for included columns
        int j = 0;
        // compose list of included column indices
        // which are the original minus the excluded ones
        final int[] columns = new int[inSpec.getNumColumns() - m_list.size()];
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            // if exclude does not contain current column name
            if (!m_list.contains(inSpec.getColumnSpec(i).getName())) {
                columns[j] = i;
                j++;
            }
        }
        assert (j == columns.length);
        // return the new spec
        ColumnRearranger c = new ColumnRearranger(inSpec);
        c.keepOnly(columns);
        return c;
    }

    /**
     * Writes number of filtered columns, and the names as
     * {@link org.knime.core.data.DataCell} to the given settings.
     * 
     * @param settings the object to save the settings into
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(KEY, m_list.toArray(new String[0]));
    }

    /**
     * Reads the filtered columns.
     * 
     * @param settings to read from
     * @throws InvalidSettingsException if the settings does not contain the
     *             size or a particular column key
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // clear exclude column list
        m_list.clear();
        // get list of excluded columns
        String[] columns = settings.getStringArray(KEY, m_list
                .toArray(new String[0]));
        for (int i = 0; i < columns.length; i++) {
            m_list.add(columns[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // true because the filter model does not care if there are columns to
        // exclude are available
    }
}
