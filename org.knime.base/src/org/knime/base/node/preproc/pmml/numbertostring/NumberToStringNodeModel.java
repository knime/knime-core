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
 * --------------------------------------------------------------------
 *
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.pmml.numbertostring;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.knime.base.node.preproc.colconvert.ColConvertNodeModel;
import org.knime.base.node.preproc.pmml.PMMLStringConversionTranslator;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

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

    /** The included columns. */
    private final SettingsModelFilterString m_inclCols =
            new SettingsModelFilterString(CFG_INCLUDED_COLUMNS);

    /**
     * Constructor with one data inport, one data outport and an optional
     * PMML inport and outport.
     */
    public NumberToStringNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)},
                new PortType[]{BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec dts = (DataTableSpec)inSpecs[0];
        // find indices to work on
        int[] indices = findColumnIndices(dts);
        ConverterFactory converterFac =
                new ConverterFactory(indices, dts);
        ColumnRearranger colre = new ColumnRearranger(dts);
        colre.replace(converterFac, indices);
        DataTableSpec outDataSpec = colre.createSpec();

        // create the PMML spec based on the optional incoming PMML spec
        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
        PMMLPortObjectSpecCreator pmmlSpecCreator
                = new PMMLPortObjectSpecCreator(pmmlSpec, dts);

        return new PortObjectSpec[]{outDataSpec, pmmlSpecCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        StringBuilder warnings = new StringBuilder();
        BufferedDataTable inData = (BufferedDataTable)inObjects[0];
        DataTableSpec inSpec = inData.getDataTableSpec();
        // find indices to work on.
        List<String> inclcols = m_inclCols.getIncludeList();
        BufferedDataTable resultTable = null;
        if (inclcols.size() == 0) {
            // nothing to convert, let's return the input table.
            resultTable = inData;
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
        } else {
            int[] indices = findColumnIndices(inData.getSpec());
            ConverterFactory converterFac
                    = new ConverterFactory(indices, inSpec);
            ColumnRearranger colre = new ColumnRearranger(inSpec);
            colre.replace(converterFac, indices);

            resultTable = exec.createColumnRearrangeTable(inData, colre, exec);
            String errorMessage = converterFac.getErrorMessage();

            if (errorMessage.length() > 0) {
                warnings.append("Problems occurred, see Console messages.\n");
            }
            if (warnings.length() > 0) {
                LOGGER.warn(errorMessage);
                setWarningMessage(warnings.toString());
            }
        }

        // the optional PMML in port (can be null)
        PMMLPortObject inPMMLPort = (PMMLPortObject)inObjects[1];
        PMMLStringConversionTranslator trans
                = new PMMLStringConversionTranslator(
                        m_inclCols.getIncludeList(), StringCell.TYPE,
                        new DerivedFieldMapper(inPMMLPort));

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                inPMMLPort, inSpec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(
               creator.createSpec(), inPMMLPort, inSpec);
        outPMMLPort.addGlobalTransformations(trans.exportToTransDict());

        return new PortObject[]{resultTable, outPMMLPort};
    }

    private int[] findColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        List<String> inclcols = m_inclCols.getIncludeList();
        StringBuilder warnings = new StringBuilder();
        if (inclcols.size() == 0) {
            warnings.append("No columns selected");
        }
        Vector<Integer> indicesvec = new Vector<Integer>();
        if (m_inclCols.isKeepAllSelected()) {
            for (DataColumnSpec cspec : spec) {
                if (cspec.getType().isCompatible(DoubleValue.class)) {
                    indicesvec.add(spec.findColumnIndex(cspec.getName()));
                }
            }
        } else {
            for (int i = 0; i < inclcols.size(); i++) {
                int colIndex = spec.findColumnIndex(inclcols.get(i));
                if (colIndex >= 0) {
                    DataType type = spec.getColumnSpec(colIndex).getType();
                    if (type.isCompatible(DoubleValue.class)) {
                        indicesvec.add(colIndex);
                    } else {
                        warnings.append("Ignoring column \""
                                        + spec.getColumnSpec(colIndex).getName()
                                        + "\"\n");
                    }
                } else {
                    throw new InvalidSettingsException("Column \""
                            + inclcols.get(i) + "\" not found.");
                }
            }
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
        int[] indices = new int[indicesvec.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesvec.get(i);
        }
        return indices;
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
    private static class ConverterFactory implements CellFactory {

        /*
         * Column indices to use.
         */
        private final int[] m_colindices;

        /*
         * Original DataTableSpec.
         */
        private final DataTableSpec m_spec;

        /*
         * Error messages.
         */
        private final StringBuilder m_error;

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
        @Override
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
        @Override
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
        @Override
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
