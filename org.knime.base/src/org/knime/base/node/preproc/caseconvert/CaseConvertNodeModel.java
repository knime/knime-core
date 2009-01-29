/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   15.06.2007 (cebron): created
 */
package org.knime.base.node.preproc.caseconvert;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
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

/**
 * NodeModel for the CaseConverter Node.
 * 
 * @author cebron, University of Konstanz
 */
public class CaseConvertNodeModel extends NodeModel {

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /**
     * Key for the uppercase mode in the NodeSettings.
     */
    public static final String CFG_UPPERCASE = "uppercase";

    /*
     * The included columns.
     */
    private String[] m_inclCols;

    /*
     * The selected mode.
     */
    private boolean m_uppercase;

    
    /**
     * Constructor with one inport and one outport.
     */
    public CaseConvertNodeModel() {
        super(1, 1);
        m_uppercase = true;
        m_inclCols = new String[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // find indices to work on.
        int[] indices = new int[m_inclCols.length];
        if (indices.length == 0) {
            setWarningMessage("No columns selected");
        }
        for (int i = 0; i < indices.length; i++) {
            int colIndex = inSpecs[0].findColumnIndex(m_inclCols[i]);
            if (colIndex >= 0) {
                indices[i] = colIndex;
            } else {
                throw new InvalidSettingsException("Column index for "
                        + m_inclCols[i] + " not found.");
            }
        }
        ConverterFactory converterFac =
                new ConverterFactory(indices, inSpecs[0]);
        ColumnRearranger colre = new ColumnRearranger(inSpecs[0]);
        colre.replace(converterFac, indices);
        DataTableSpec newspec = colre.createSpec();
        return new DataTableSpec[]{newspec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // find indices to work on.
        DataTableSpec inspec = inData[0].getDataTableSpec();
        if (m_inclCols.length == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
            return new BufferedDataTable[]{inData[0]};
        }
        int[] indices = new int[m_inclCols.length];
        for (int i = 0; i < indices.length; i++) {
            int colIndex = inspec.findColumnIndex(m_inclCols[i]);
            if (colIndex >= 0) {
                indices[i] = colIndex;
            } else {
                throw new Exception("Column index for " + m_inclCols[i]
                        + " not found.");
            }
        }
        ConverterFactory converterFac = new ConverterFactory(indices, inspec);
        ColumnRearranger colre = new ColumnRearranger(inspec);
        colre.replace(converterFac, indices);

        BufferedDataTable resultTable =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        return new BufferedDataTable[]{resultTable};
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
        m_inclCols =
                settings.getStringArray(CFG_INCLUDED_COLUMNS,
                        new String[]{});
        m_uppercase =
                settings.getBoolean(CFG_UPPERCASE, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
       settings.addStringArray(CFG_INCLUDED_COLUMNS, m_inclCols);
       settings.addBoolean(CFG_UPPERCASE, m_uppercase);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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

        private Locale m_locale;

        /**
         * 
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        ConverterFactory(final int[] colindices, final DataTableSpec spec) {
            m_colindices = colindices;
            m_spec = spec;
            m_locale = Locale.getDefault();
        }

        /** {@inheritDoc} */
        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                if (!dc.isMissing()) {
                    String newstring = null;
                    if (m_uppercase) {
                        newstring =
                                ((StringValue)dc).getStringValue().toUpperCase(
                                        m_locale);
                    } else {
                        newstring =
                                ((StringValue)dc).getStringValue().toLowerCase(
                                        m_locale);
                    }
                    newcells[i] = new StringCell(newstring);
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
                DataColumnDomain domain = colspec.getDomain();
                Set<DataCell> newdomainvalues = new HashSet<DataCell>();
                DataColumnSpecCreator colspeccreator =
                        new DataColumnSpecCreator(colspec);
                if (domain.hasValues()) {
                    for (DataCell dc : domain.getValues()) {
                        String newstring = null;
                        if (m_uppercase) {
                            newstring =
                                    ((StringValue)dc).getStringValue()
                                            .toUpperCase(m_locale);
                        } else {
                            newstring =
                                    ((StringValue)dc).getStringValue()
                                            .toLowerCase(m_locale);
                        }
                        newdomainvalues.add(new StringCell(newstring));
                    }
                    DataColumnDomainCreator domaincreator =
                            new DataColumnDomainCreator();
                    domaincreator.setValues(newdomainvalues);
                    colspeccreator.setDomain(domaincreator.createDomain());
                }
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
