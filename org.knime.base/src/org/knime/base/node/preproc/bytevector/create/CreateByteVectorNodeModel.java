/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * Created on 2014.01.15. by gabor
 */
package org.knime.base.node.preproc.bytevector.create;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.SourceColumnsAsProperties;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of CreateByteVector. Creates a new byte vector column from some columns of ints.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public class CreateByteVectorNodeModel extends NodeModel {
    private static final String CFGKEY_COLUMNS = "columns";

    private static final String CFGKEY_REMOVE_INPUT = "remove input columns";

    private static final boolean DEFAULT_REMOVE_INPUT = true;

    private static final String CFGKEY_OUTPUT_COLUMN = "output";

    private static final String DEFAULT_OUTPUT_COLUMN = "Byte Vector";

    private static final String CFGKEY_FAIL_ON_MISSING = "fail on missing";

    private static final boolean DEFAULT_FAIL_ON_MISSING = true;

    private static final String CFGKEY_FAIL_ON_OUT_OF_INTERVAL = "fail on out of interval";

    private static final boolean DEFAULT_FAIL_ON_OUT_OF_INTERVAL = true;

    private final SettingsModelColumnFilter2 m_inputColumns = createInputColumns();

    private final SettingsModelBoolean m_removeInput = createRemoveInput();

    private final SettingsModelString m_outputColumn = createOutputColumn();

    private final SettingsModelBoolean m_failOnMissing = createFailOnMissing();

    private final SettingsModelBoolean m_failOnOutOfInterval = createFailOnOutOfInterval();

    /**
     * Constructor for the node model.
     */
    protected CreateByteVectorNodeModel() {
        super(1, 1);
    }

    /**
     * @return The selected input columns model.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createInputColumns() {
        return new SettingsModelColumnFilter2(CFGKEY_COLUMNS, IntValue.class);
    }

    static SettingsModelBoolean createRemoveInput() {
        return new SettingsModelBoolean(CFGKEY_REMOVE_INPUT, DEFAULT_REMOVE_INPUT);
    }

    static SettingsModelString createOutputColumn() {
        return new SettingsModelString(CFGKEY_OUTPUT_COLUMN, DEFAULT_OUTPUT_COLUMN);
    }

    static SettingsModelBoolean createFailOnMissing() {
        return new SettingsModelBoolean(CFGKEY_FAIL_ON_MISSING, DEFAULT_FAIL_ON_MISSING);
    }

    static SettingsModelBoolean createFailOnOutOfInterval() {
        return new SettingsModelBoolean(CFGKEY_FAIL_ON_OUT_OF_INTERVAL, DEFAULT_FAIL_ON_OUT_OF_INTERVAL);
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger ret = new ColumnRearranger(inSpec);
        final String[] includes = m_inputColumns.applyTo(inSpec).getIncludes();
        if (m_removeInput.getBooleanValue()) {
            ret.remove(includes);
        }
        final DataColumnSpecCreator newCol =
            new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec, m_outputColumn.getStringValue()),
                DenseByteVectorCell.TYPE);
        newCol.setElementNames(includes);
        final int[] sourceColumnIndices = SourceColumnsAsProperties.indices(m_inputColumns.applyTo(inSpec), inSpec);
        for (int i = sourceColumnIndices.length; i-- > 0;) {
            if (sourceColumnIndices[i] < 0) {
                throw new IllegalStateException("Unknown column: " + includes[i]);
            }
        }
        ret.append(new SingleCellFactory(newCol.createSpec()) {

            @Override
            public DataCell getCell(final DataRow row) {
                final DenseByteVectorCellFactory fac = new DenseByteVectorCellFactory(sourceColumnIndices.length);
                for (int i = sourceColumnIndices.length; i-- > 0;) {
                    DataCell cell = row.getCell(sourceColumnIndices[i]);
                    if (cell.isMissing()) {
                        if (m_failOnMissing.getBooleanValue()) {
                            throw new IllegalArgumentException("Missing value in the row: " + row.getKey()
                                + "\nin the column: " + includes[i]);
                        } else {
                            //return DataType.getMissingCell();
                            fac.setValue(i, 0);
                        }
                    } else if (cell instanceof IntValue) {
                        int intValue = ((IntValue)cell).getIntValue();
                        if (intValue < 0 || intValue > 255) {
                            if (m_failOnOutOfInterval.getBooleanValue()) {
                                throw new IllegalArgumentException("Invalid value: " + intValue + "\nin row: "
                                    + row.getKey() + "\nin the column: " + includes[i]);
                            } else {
                                fac.setValue(i, 0);
                            }
                        } else {
                            fac.setValue(i, intValue);
                        }
                    } else {
                        throw new IllegalStateException("Not an int value: " + cell + " (" + cell.getType() + ")");
                    }
                }
                return fac.createDataCell();
            }
        });
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0],
            createRearranger(inData[0].getSpec()), exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state, nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createRearranger(inSpecs[0]).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inputColumns.saveSettingsTo(settings);
        m_removeInput.saveSettingsTo(settings);
        m_outputColumn.saveSettingsTo(settings);
        m_failOnMissing.saveSettingsTo(settings);
        m_failOnOutOfInterval.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumns.loadSettingsFrom(settings);
        m_removeInput.loadSettingsFrom(settings);
        m_outputColumn.loadSettingsFrom(settings);
        m_failOnMissing.loadSettingsFrom(settings);
        m_failOnOutOfInterval.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumns.validateSettings(settings);
        m_removeInput.validateSettings(settings);
        m_outputColumn.validateSettings(settings);
        m_failOnMissing.validateSettings(settings);
        m_failOnOutOfInterval.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state, nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state, nothing to do
    }

}
