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
 * --------------------------------------------------------------------
 * 
 * History
 *   15.06.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
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

/**
 * NodeModel for the ColConverter Node.
 * 
 * @author cebron, University of Konstanz
 */
public class ColConvertNodeModel extends NodeModel {

    /* Node Logger of this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ColConvertNodeModel.class);

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /**
     * Key for the selected mode in the NodeSettings.
     */
    public static final String CFG_STRINGTODOUBLE = "stringtodouble";

    /*
     * The included columns.
     */
    private String[] m_inclCols;

    /*
     * The selected mode.
     */
    private boolean m_stringtodouble;

    /**
     * Constructor with one inport and one outport. 
     */
    public ColConvertNodeModel() {
        super(1, 1);
        m_inclCols = new String[]{};
        m_stringtodouble = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        StringBuilder warnings = new StringBuilder();
        if (m_inclCols.length == 0) {
            warnings.append("No columns selected");
        }
        // find indices to work on.
        Vector<Integer> indicesvec = new Vector<Integer>();
        for (int i = 0; i < m_inclCols.length; i++) {
            int colIndex = inSpecs[0].findColumnIndex(m_inclCols[i]);
            if (colIndex >= 0) {
                DataType type = inSpecs[0].getColumnSpec(colIndex).getType();
                if ((m_stringtodouble && type.isCompatible(StringValue.class))
                        || (!m_stringtodouble && type
                                .isCompatible(DoubleValue.class))) {
                    indicesvec.add(colIndex);
                } else {
                    warnings.append("Ignoring column \'"
                            + inSpecs[0].getColumnSpec(colIndex).getName()
                            + "\'\n");
                }
            } else {
                throw new InvalidSettingsException("Column index for "
                        + m_inclCols[i] + " not found.");
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
        if (m_inclCols.length == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
            return new BufferedDataTable[]{inData[0]};
        }
        Vector<Integer> indicesvec = new Vector<Integer>();
        for (int i = 0; i < m_inclCols.length; i++) {
            int colIndex = inspec.findColumnIndex(m_inclCols[i]);
            if (colIndex >= 0) {
                DataType type = inspec.getColumnSpec(colIndex).getType();
                if ((m_stringtodouble && type.isCompatible(StringValue.class))
                        || (!m_stringtodouble && type
                                .isCompatible(DoubleValue.class))) {
                    indicesvec.add(colIndex);
                } else {
                    warnings
                            .append("Ignoring column \'"
                                    + inspec.getColumnSpec(colIndex).getName()
                                    + "\'\n");
                }
            } else {
                throw new Exception("Column index for " + m_inclCols[i]
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
        m_inclCols =
                settings.getStringArray(CFG_INCLUDED_COLUMNS, new String[]{});
        m_stringtodouble = settings.getBoolean(CFG_STRINGTODOUBLE, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_INCLUDED_COLUMNS, m_inclCols);
        settings.addBoolean(CFG_STRINGTODOUBLE, m_stringtodouble);
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

        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                if (m_stringtodouble) {
                    // should be a StringCell, otherwise copy original cell.
                    if (dc.getType().isCompatible(StringValue.class)
                            && !dc.isMissing()) {
                        String s = ((StringValue)dc).getStringValue();
                        double d = Double.NaN;
                        try {
                            d = Double.parseDouble(s);
                            newcells[i] = new DoubleCell(d);
                        } catch (NumberFormatException e) {
                            m_error.append("Could not parse cell with value "
                                    + "\'" + s + "\' (RowKey: "
                                    + row.getKey().toString() + ", Position: "
                                    + m_colindices[i] + ")\n");
                            newcells[i] = DataType.getMissingCell();
                        }
                    } else {
                        newcells[i] = dc;
                    }
                } else {
                    // should be a DoubleCell, otherwise copy original cell.
                    if (dc.getType().isCompatible(DoubleValue.class)
                            && !dc.isMissing()) {
                        double d = ((DoubleValue)dc).getDoubleValue();
                        newcells[i] = new StringCell("" + d);
                    } else {
                        newcells[i] = dc;
                    }
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
                // change DataType according to select mode
                if (m_stringtodouble) {
                    colspeccreator =
                            new DataColumnSpecCreator(colspec.getName(),
                                    DoubleCell.TYPE);
                } else {
                    colspeccreator =
                            new DataColumnSpecCreator(colspec.getName(),
                                    StringCell.TYPE);
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
