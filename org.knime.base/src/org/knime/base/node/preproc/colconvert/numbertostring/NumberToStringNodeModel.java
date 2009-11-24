/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * --------------------------------------------------------------------
 *
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.numbertostring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * The NodeModel for the Number to String Node that converts numbers
 * to StringValues.
 *
 * @author cebron, University of Konstanz
 */
public class NumberToStringNodeModel extends NodeModel {

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_COLUMNS = "include";

    /** The included columns. */
    private final SettingsModelFilterString m_columns =
            new SettingsModelFilterString(CFG_COLUMNS);

    /**
     * Constructor with one inport and one outport.
     */
    public NumberToStringNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger colre = findColumnIndices(inSpecs[0]);
        DataTableSpec newspec = colre.createSpec();
        return new DataTableSpec[]{newspec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inspec = inData[0].getDataTableSpec();
        ColumnRearranger colre = findColumnIndices(inspec);
        BufferedDataTable resultTable =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        return new BufferedDataTable[]{resultTable};
    }

    private ColumnRearranger findColumnIndices(final DataTableSpec spec) {
        final List<String> columnList;
        if (m_columns.isEnforceInclusion()) {
            columnList = m_columns.getIncludeList();
        } else {
            columnList = m_columns.getExcludeList();
        }

        // compose list of included column indices
        final ArrayList<Integer> columns = new ArrayList<Integer>(); 
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataType type = spec.getColumnSpec(i).getType();
            if (!type.isCompatible(DoubleValue.class)) {
                continue;
            }
            String colName = spec.getColumnSpec(i).getName();
            if (m_columns.isEnforceInclusion()) {
                // if include column list does contain the column
                if (columnList.contains(colName)) {
                    columns.add(i);
                }
            } else {
                // if exclude column list does not contain the column
                if (!columnList.contains(colName)) {
                    columns.add(i);
                }
            }
        }
        
        if (columns.isEmpty()) {
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
        }           
        
        // generated warning message
        StringBuilder warning = new StringBuilder();
        // check if all specified columns exist in the input spec
        for (String name : columnList) {
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
        
        int[] indices = new int[columns.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = columns.get(i);
        }
        ColumnRearranger colre = new ColumnRearranger(spec);
        colre.replace(new ConverterFactory(indices, spec), indices);
        return colre;
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columns.validateSettings(settings);
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
     * The CellFactory to produce the new converted cells.
     *
     * @author cebron, University of Konstanz
     */
    private class ConverterFactory implements CellFactory {

        /*
         * Column indices to use.
         */
        private int[] m_colindices;

        /*
         * Original DataTableSpec.
         */
        private DataTableSpec m_spec;

        /**
         *
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        ConverterFactory(final int[] colindices, final DataTableSpec spec) {
            m_colindices = colindices;
            m_spec = spec;
        }

        /**
         * {@inheritDoc}
         */
        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                // handle integers separately to avoid decimal places
                if (dc instanceof IntValue) {
                    int iVal = ((IntValue)dc).getIntValue();
                    newcells[i] = new StringCell(Integer.toString(iVal));
                } else if (dc instanceof DoubleValue) {
                    double d = ((DoubleValue)dc).getDoubleValue();
                    newcells[i] = new StringCell(Double.toString(d));
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        /**
         * {@inheritDoc}
         */
        public DataColumnSpec[] getColumnSpecs() {
            DataColumnSpec[] newcolspecs =
                    new DataColumnSpec[m_colindices.length];
            for (int i = 0; i < newcolspecs.length; i++) {
                DataColumnSpec colspec = m_spec.getColumnSpec(m_colindices[i]);
                DataColumnSpecCreator colspeccreator = null;
                // change DataType to StringCell
                colspeccreator =
                        new DataColumnSpecCreator(colspec.getName(),
                                StringCell.TYPE);
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        /**
         * {@inheritDoc}
         */
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Converting");
        }

    } // end ConverterFactory
}
