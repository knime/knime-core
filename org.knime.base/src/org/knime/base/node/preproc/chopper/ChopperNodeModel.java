/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Dec 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.chopper;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * This is the model implementation of Chopper node model.
 *
 * @author ohl, KNIME
 */
public class ChopperNodeModel extends SimpleStreamableFunctionNodeModel {

    enum TokenType {

        String(StringCellFactory.TYPE), Double(DoubleCellFactory.TYPE);

        private final DataType m_tokenType;

        TokenType(final DataType type) {
            m_tokenType = type;
        }

        DataType getType() {
            return m_tokenType;
        }
    }

    enum NewType {
            Vector, Collection, Cells
    }

    private final SettingsModelString m_column = createSelColumnSettingsModel();

    private final SettingsModelInteger m_numOfCols = createNumberOfColsSettingsModel();

    private final SettingsModelString m_extractType = createExtractedTypeSettingsModel();

    private final SettingsModelString m_createType = createCreatedColTypeSettingsModel();

    private final SettingsModelString m_colPrefix = createColNameSettingsModel();

    private final SettingsModelString m_delimiter = createDelimiterSettingsModel();

    private final SettingsModelBoolean m_keepRest = createKeepRestSettingsModel();

    private int m_selectedCol = -1; // set by configure

    /*
     * static factory methods for the SettingsModels used here, and in the
     * NodeDialog.
     */
    /**
     * @return model to store the selected column name.
     */
    static SettingsModelString createSelColumnSettingsModel() {
        return new SettingsModelString("column", "");
    }

    static SettingsModelInteger createNumberOfColsSettingsModel() {
        return new SettingsModelInteger("numOfCols", 1);
    }

    static SettingsModelString createExtractedTypeSettingsModel() {
        return new SettingsModelString("extractedType", TokenType.Double.name());
    }

    static SettingsModelString createCreatedColTypeSettingsModel() {
        return new SettingsModelString("createdType", NewType.Vector.name());
    }

    static SettingsModelString createColNameSettingsModel() {
        return new SettingsModelString("newColPrefix", "");
    }

    static SettingsModelString createDelimiterSettingsModel() {
        return new SettingsModelString("delimiter", "");
    }

    static SettingsModelBoolean createKeepRestSettingsModel() {
        return new SettingsModelBoolean("keepRest", true);
    }

    /**
     * Creates the ColumnRearranger for the rearranger table. Also used to compute the output table spec. Call only when
     * valid user settings are available!
     *
     * @param inTableSpec the spec of the table to split the column from
     * @return the col rearranger
     * @since 3.1
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inTableSpec) throws InvalidSettingsException {
        String colName = m_column.getStringValue();
        if (colName == null || colName.isEmpty()) {
            throw new InvalidSettingsException("Please configure node.");
        }
        int selectedCol = inTableSpec.findColumnIndex(m_column.getStringValue());
        if (selectedCol < 0) {
            throw new InvalidSettingsException(
                "Selected column '" + m_column.getStringValue() + "' doesn't exist in input table");
        }
        // and it should be of type string
        if (!inTableSpec.getColumnSpec(selectedCol).getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Selected column '" + colName + "' is not of type String");
        }

        if (m_numOfCols.getIntValue() < 1) {
            throw new InvalidSettingsException("Number of extracted columns must be greater than zero.");
        }

        if (m_delimiter.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException("Delimiter of value string must be defined.");
        }
        try {
            NewType.valueOf(m_createType.getStringValue());
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(
                "Specified type of new column(s) is not supported: " + m_createType.getStringValue());
        }
        try {
            TokenType.valueOf(m_extractType.getStringValue());
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(
                "Specified type of items to chop off is not supported: " + m_extractType.getStringValue());
        }

        CellFactory factory;
        NewType newType = NewType.valueOf(m_createType.getStringValue());
        TokenType tokenType = TokenType.valueOf(m_extractType.getStringValue());

        switch (newType) {
            case Vector:
                factory = new ChopperVectorCellFactory(inTableSpec, m_column.getStringValue(),
                    m_colPrefix.getStringValue(), m_delimiter.getStringValue(), m_numOfCols.getIntValue(),
                    tokenType.getType(), m_keepRest.getBooleanValue());
                break;
            case Collection:
                factory = new ChopperCollectionCellFactory(inTableSpec, m_column.getStringValue(),
                    m_colPrefix.getStringValue(), m_delimiter.getStringValue(), m_numOfCols.getIntValue(),
                    tokenType.getType(), m_keepRest.getBooleanValue());
                break;
            case Cells:
                factory = new ChopperCellFactory(inTableSpec, m_column.getStringValue(),
                    m_colPrefix.getStringValue(), m_delimiter.getStringValue(), m_numOfCols.getIntValue(),
                    tokenType.getType(), m_keepRest.getBooleanValue());
                break;
            default:
                throw new InvalidSettingsException("The target type '" + newType + "' is not supported.");
        }
        ColumnRearranger c = new ColumnRearranger(inTableSpec);

        c.append(factory);
        if (m_keepRest.getBooleanValue()) {
            c.remove(m_column.getStringValue());
        }
        return c;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
        m_numOfCols.saveSettingsTo(settings);
        m_extractType.saveSettingsTo(settings);
        m_createType.saveSettingsTo(settings);
        m_colPrefix.saveSettingsTo(settings);
        m_delimiter.saveSettingsTo(settings);
        m_keepRest.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
        m_numOfCols.loadSettingsFrom(settings);
        m_extractType.loadSettingsFrom(settings);
        m_createType.loadSettingsFrom(settings);
        m_colPrefix.loadSettingsFrom(settings);
        m_delimiter.loadSettingsFrom(settings);
        m_keepRest.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.validateSettings(settings);
        m_numOfCols.validateSettings(settings);
        m_extractType.validateSettings(settings);
        m_createType.validateSettings(settings);
        m_colPrefix.validateSettings(settings);
        m_delimiter.validateSettings(settings);
        m_keepRest.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

}
