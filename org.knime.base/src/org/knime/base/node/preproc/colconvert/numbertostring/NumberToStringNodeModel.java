/* 
 * --------------------------------------------------------------------
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
 * --------------------------------------------------------------------
 * 
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.numbertostring;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.knime.base.node.preproc.colconvert.ColConvertNodeModel;
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
import org.knime.core.node.NodeLogger;
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
  
    /* Node Logger of this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ColConvertNodeModel.class);

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /*
     * The included columns.
     */
    private SettingsModelFilterString m_inclCols =
            new SettingsModelFilterString(CFG_INCLUDED_COLUMNS);

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
        StringBuilder warnings = new StringBuilder();
        List<String> inclcols = m_inclCols.getIncludeList();
        if (inclcols.size() == 0) {
            warnings.append("No columns selected");
        }
        // find indices to work on.
        Vector<Integer> indicesvec = new Vector<Integer>();
        
        for (int i = 0; i < inclcols.size(); i++) {
            int colIndex = inSpecs[0].findColumnIndex(inclcols.get(i));
            if (colIndex >= 0) {
                DataType type = inSpecs[0].getColumnSpec(colIndex).getType();
                if (type.isCompatible(DoubleValue.class)) {
                    indicesvec.add(colIndex);
                } else {
                    warnings.append("Ignoring column \'"
                            + inSpecs[0].getColumnSpec(colIndex).getName()
                            + "\'\n");
                }
            } else {
                throw new InvalidSettingsException("Column index for "
                        + inclcols.get(i) + " not found.");
            }
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
        int[] indices = new int[indicesvec.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesvec.get(i);
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
        StringBuilder warnings = new StringBuilder();
        // find indices to work on.
        DataTableSpec inspec = inData[0].getDataTableSpec();
        List<String> inclcols = m_inclCols.getIncludeList();
        if (inclcols.size() == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
            return new BufferedDataTable[]{inData[0]};
        }
        Vector<Integer> indicesvec = new Vector<Integer>();
        for (int i = 0; i < inclcols.size(); i++) {
            int colIndex = inspec.findColumnIndex(inclcols.get(i));
            if (colIndex >= 0) {
                DataType type = inspec.getColumnSpec(colIndex).getType();
                if (type.isCompatible(DoubleValue.class)) {
                    indicesvec.add(colIndex);
                } else {
                    warnings
                            .append("Ignoring column \'"
                                    + inspec.getColumnSpec(colIndex).getName()
                                    + "\'\n");
                }
            } else {
                throw new Exception("Column index for " + inclcols.get(i)
                        + " not found.");
            }
        }
        int[] indices = new int[indicesvec.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesvec.get(i);
        }
        ConverterFactory converterFac = new ConverterFactory(indices, inspec);
        ColumnRearranger colre = new ColumnRearranger(inspec);
        colre.replace(converterFac, indices);

        BufferedDataTable resultTable =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        String errorMessage = converterFac.getErrorMessage();

        if (errorMessage.length() > 0) {
            warnings.append("Problems occured, see NodeLogger messages.\n");
        }
        if (warnings.length() > 0) {
            LOGGER.warn(errorMessage);
            setWarningMessage(warnings.toString());
        }
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
        m_inclCols.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inclCols.validateSettings(settings);
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

        /*
         * Error messages.
         */
        private StringBuilder m_error;

        /**
         * 
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        ConverterFactory(final int[] colindices, final DataTableSpec spec) {
            m_colindices = colindices;
            m_spec = spec;
            m_error = new StringBuilder();
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

        /**
         * Error messages that occur during execution , i.e.
         * NumberFormatException.
         * 
         * @return error message
         */
        public String getErrorMessage() {
            return m_error.toString();
        }

    } // end ConverterFactory
}
