/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 *
 * History
 *   26.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;

/**
 * This class is the model for the elimination loop's tail node. It compares the
 * results of a preceding prediction node with the target column values and
 * decides which column should be dropped for the next iteration.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimTailNodeModel extends GenericNodeModel 
        implements LoopEndNode {
    
    private final BWElimTailSettings m_settings = new BWElimTailSettings();

    private final List<String> m_includedColumns = new ArrayList<String>();

    private final List<String> m_excludedColumns = new ArrayList<String>();

    private int m_excludedFeatureIndex = -1;

    private static final DataColumnSpec NR_FEATURES =
            new DataColumnSpecCreator("Nr. of features", IntCell.TYPE)
                    .createSpec();

    private static final DataColumnSpec ERROR_RATE;

    private static final DataColumnSpec SQUARED_ERROR;

    private static final DataColumnSpec REMOVED_FEATURE =
            new DataColumnSpecCreator("Removed feature", StringCell.TYPE)
                    .createSpec();

    static {
        DataColumnSpecCreator cc =
                new DataColumnSpecCreator("Error Rate", DoubleCell.TYPE);
        cc.setDomain(new DataColumnDomainCreator(new DoubleCell(0),
                new DoubleCell(1)).createDomain());
        ERROR_RATE = cc.createSpec();

        cc = new DataColumnSpecCreator("Squarred Error", DoubleCell.TYPE);
        cc.setDomain(new DataColumnDomainCreator(new DoubleCell(0),
                new DoubleCell(Double.MAX_VALUE)).createDomain());
        SQUARED_ERROR = cc.createSpec();
    }

    private BufferedDataContainer m_resultTable;

    private BWElimModel m_model;

    private String m_superflousFeature = "";

    private double m_smallestError = Double.POSITIVE_INFINITY;

    /**
     * Creates a new model having one table input port, one table output port
     * and a model output port.
     */
    public BWElimTailNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, BWElimModel.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        int pIndex = inSpec.findColumnIndex(m_settings.predictionColumn());
        if (pIndex == -1) {
            throw new InvalidSettingsException("Prediction column '"
                    + m_settings.predictionColumn() + "' does not exist");
        }

        int tIndex = inSpec.findColumnIndex(m_settings.targetColumn());
        if (tIndex == -1) {
            throw new InvalidSettingsException("Target column '"
                    + m_settings.targetColumn() + "' does not exist");
        }

        DataType commonType =
                DataType.getCommonSuperType(inSpec.getColumnSpec(pIndex)
                        .getType(), inSpec.getColumnSpec(tIndex).getType());
        if ((commonType.getValueClasses().size() == 1)
                && (commonType.getValueClasses().get(0) == DataValue.class)) {
            throw new InvalidSettingsException("Target and prediction column "
                    + "are not of compatible type");
        }

        if (inSpec.getColumnSpec(pIndex).getType().isCompatible(
                DoubleValue.class)) {
            return new PortObjectSpec[]{
                    new DataTableSpec(NR_FEATURES, SQUARED_ERROR,
                            REMOVED_FEATURE), null};
        } else {
            return new PortObjectSpec[]{
                    new DataTableSpec(NR_FEATURES, ERROR_RATE, REMOVED_FEATURE),
                    null};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = (BufferedDataTable)inData[0];
        int pIndex =
                table.getDataTableSpec().findColumnIndex(
                        m_settings.predictionColumn());
        int tIndex =
                table.getDataTableSpec().findColumnIndex(
                        m_settings.targetColumn());

        if (m_resultTable == null) {
            if (table.getDataTableSpec().getColumnSpec(pIndex).getType()
                    .isCompatible(DoubleValue.class)) {
                m_resultTable =
                        exec.createDataContainer(new DataTableSpec(NR_FEATURES,
                                SQUARED_ERROR, REMOVED_FEATURE), false);
            } else {
                m_resultTable =
                        exec.createDataContainer(new DataTableSpec(NR_FEATURES,
                                ERROR_RATE, REMOVED_FEATURE), false);
            }
            m_model = new BWElimModel(m_settings.targetColumn());
        }

        DataColumnSpec tSpec =
                table.getDataTableSpec().getColumnSpec(
                        m_settings.targetColumn());
        double error;
        if (tSpec.getType().isCompatible(DoubleValue.class)) {
            error = computeSquaredError(table, tIndex, pIndex);
        } else {
            error = computeErrorRate(table, tIndex, pIndex);
        }

        if (m_excludedFeatureIndex == -1) {
            m_includedColumns.addAll(((BWElimHeadNodeModel)getLoopStartNode())
                    .inputColumns());
            m_includedColumns.remove(m_settings.targetColumn());
            // first iteration with all columns for reference
            m_resultTable.addRowToTable(new DefaultRow(new RowKey("All"),
                    new IntCell(m_includedColumns.size()),
                    new DoubleCell(error), new StringCell("")));
            m_excludedFeatureIndex = 0;

            m_model.addFeatureLevel(error, m_includedColumns);
        } else {
            if (m_smallestError > error) {
                m_smallestError = error;
                m_superflousFeature =
                        m_includedColumns.get(m_excludedFeatureIndex);
            }

            if (m_excludedFeatureIndex == m_includedColumns.size() - 1) {
                // all remaining features excluded once
                m_excludedColumns.add(m_superflousFeature);
                m_includedColumns.remove(m_superflousFeature);

                m_resultTable.addRowToTable(new DefaultRow(new RowKey(""
                        + m_includedColumns.size()), new IntCell(
                        m_includedColumns.size()), new DoubleCell(
                        m_smallestError), new StringCell(m_superflousFeature)));
                m_excludedFeatureIndex = 0;

                m_model.addFeatureLevel(m_smallestError, m_includedColumns);

                m_smallestError = Double.POSITIVE_INFINITY;
                m_superflousFeature = "";
            } else {
                m_excludedFeatureIndex++;
            }
        }

        if (m_includedColumns.size() > 1) {
            continueLoop();
            return new PortObject[]{null, null};
        } else {
            m_resultTable.close();
            return new PortObject[]{m_resultTable.getTable(), m_model};
        }
    }

    private static double computeSquaredError(final BufferedDataTable table,
            final int targetIndex, final int predictionIndex) {
        double sum = 0;
        for (DataRow row : table) {
            if (row.getCell(targetIndex).isMissing()) {
                throw new RuntimeException(
                        "This node cannot handle missing values");
            }
            if (row.getCell(predictionIndex).isMissing()) {
                throw new RuntimeException(
                        "This node cannot handle missing values");
            }

            double a =
                    ((DoubleValue)row.getCell(predictionIndex))
                            .getDoubleValue();
            double b = ((DoubleValue)row.getCell(targetIndex)).getDoubleValue();
            sum += (a - b) * (a - b);
        }

        return Math.sqrt(sum);
    }

    private static double computeErrorRate(final BufferedDataTable table,
            final int targetIndex, final int predictionIndex) {
        int wrong = 0;
        for (DataRow row : table) {
            if (row.getCell(targetIndex).isMissing()) {
                throw new RuntimeException(
                        "This node cannot handle missing values");
            }
            if (row.getCell(predictionIndex).isMissing()) {
                throw new RuntimeException(
                        "This node cannot handle missing values");
            }
            if (!row.getCell(predictionIndex).equals(
                    row.getCell(targetIndex))) {
                wrong++;
            }

        }

        return wrong / (double)table.getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_includedColumns.clear();
        m_excludedColumns.clear();
        m_resultTable = null;
        m_model = null;
        m_excludedFeatureIndex = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        BWElimTailSettings s = new BWElimTailSettings();
        s.loadSettings(settings);
    }

    /**
     * Returns a list with all currently included columns.
     *
     * @return a list with column names
     */
    List<String> includedColumns() {
        return m_includedColumns;
    }

    /**
     * Returns a list with all currently excluded columns.
     *
     * @return a list with column names
     */
    List<String> excludedColumns() {
        return m_excludedColumns;
    }

    /**
     * Returns the currently excluded column's (feature's) index.
     *
     * @return the index
     */
    int excludedFeatureIndex() {
        return m_excludedFeatureIndex;
    }
}
