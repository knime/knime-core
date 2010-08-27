/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.filter.column;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.base.node.preproc.filter.column.FilterColumnPanel.SelectionOption;
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
    
    /** Config for the force include/exclude flag. */
    static final String CFG_KEY_SELECTIONOPTION = "selectionOption";
    
    /** Enforce inclusion/exclusion flag. */
    private SelectionOption m_selectionOption = 
        SelectionOption.EnforceExclusion;



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
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        boolean allRetained = true;
        // compose list of included column names
        final ArrayList<String> columns = new ArrayList<String>(); 
        for (int i = 0; i < spec.getNumColumns(); i++) {
            String colName = spec.getColumnSpec(i).getName();
            switch (m_selectionOption) {
            case EnforceInclusion:
                // if (include) column list contains current column name
                if (m_list.contains(colName)) {
                    columns.add(colName);
                } else {
                    allRetained = false;
                }
                break;
            case EnforceExclusion:
                // if (exclude) column list does not contain current column name
                if (!m_list.contains(colName)) {
                    columns.add(colName);
                } else {
                    allRetained = false;
                }
                break;
            default:
                throw new InvalidSettingsException("Unknown selection option: "
                        + m_selectionOption);
            }
        }
        
        // generated warning message
        StringBuilder warning = new StringBuilder();
        // check if all specified columns exist in the input spec
        for (String name : m_list) {
            if (!spec.containsName(name)) {
                if (warning.length() > 0) {
                    warning.append(',');
                } 
                warning.append(name);
            }
        }

        if (warning.length() > 0) {
            setWarningMessage("Some columns are not available: " 
                    + warning.toString());
        }
        
        if (columns.isEmpty()) {
            if (spec.getNumColumns() > 0) {
                setWarningMessage("All columns removed.");
            }
        } else {
            if (allRetained) {
                setWarningMessage("All columns retained.");
                return new ColumnRearranger(spec);
            }
        }        
        
        // create column rearranger
        ColumnRearranger c = new ColumnRearranger(spec);
        c.keepOnly(columns.toArray(new String[0]));
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
        settings.addString(
                CFG_KEY_SELECTIONOPTION, m_selectionOption.toString());
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
        m_selectionOption = SelectionOption.parse(
                settings.getString(CFG_KEY_SELECTIONOPTION, 
                SelectionOption.EnforceExclusion.toString()));
        // clear exclude column list
        m_list.clear();
        // get list of excluded columns
        String[] columns = settings.getStringArray(
                KEY, m_list.toArray(new String[0]));
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
        SelectionOption.parse(settings.getString(CFG_KEY_SELECTIONOPTION, 
                SelectionOption.EnforceExclusion.toString()));
        // true because the filter model does not care if there are columns to
        // exclude are available
    }
}
