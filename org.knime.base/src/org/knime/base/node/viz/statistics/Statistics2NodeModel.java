/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.knime.base.data.statistics.Statistics2Table;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The StatisticsNodeModel creates a new StatisticTable based on the input data
 * table.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class Statistics2NodeModel extends NodeModel {
    
    /** Statistical values table. */
    private Statistics2Table m_statTable;

    private final HiLiteHandler m_hilite = new HiLiteHandler();
    
    private final SettingsModelBoolean m_computeMedian =
        Statistics2NodeDialogPane.createMedianModel();
    
    private final SettingsModelIntegerBounded m_nominalValues = 
        Statistics2NodeDialogPane.createNominalValuesModel();
    
    private final SettingsModelIntegerBounded m_nominalValuesOutput = 
        Statistics2NodeDialogPane.createNominalValuesModelOutput();
    
    /** One input and one output. */
    Statistics2NodeModel() {
        super(1, 2);
    }

    /**
     * Output table is like the input table. After we are executed we can
     * deliver a better table spec. But not before.
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{
                Statistics2Table.createOutSpecNumeric(inSpecs[0]),
                Statistics2Table.createOutSpecNominal(inSpecs[0])};
    }

    /**
     * Computes the statistics for the DataTable at the in-port. Use the view on
     * this node to see them.
     * 
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_statTable = new Statistics2Table(
                inData[0], m_computeMedian.getBooleanValue(), 
                numOfNominalValuesOutput(), exec);
        BufferedDataTable outTable1 = exec.createBufferedDataTable(
                m_statTable.createStatisticMomentsTable(), 
                exec.createSubProgress(0.5));
        BufferedDataTable outTable2 = exec.createBufferedDataTable(
                m_statTable.createNominalValueTable(), 
                exec.createSubProgress(0.5));
        return new BufferedDataTable[]{outTable1, outTable2};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_computeMedian.loadSettingsFrom(settings);
        m_nominalValues.loadSettingsFrom(settings);
        m_nominalValuesOutput.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_statTable = null;
    }
    
    /**
     * @return statistics table containing all statistic moments
     */
    final DataTable getStatsTable() {
        return m_statTable.createStatisticMomentsTable();
    }

    /**
     * @return Returns the column Names.
     */
    String[] getColumnNames() {
        if (m_statTable == null) {
            return null;
        }
        return m_statTable.getColumnNames();
    }
    
    /**
     * @return number of missing values
     */
    double[] getNumMissingValues() {
        if (m_statTable == null) {
            return null;
        }
        return m_statTable.getNumberMissingValues();
    }
    
    /** @return number of nominal values computed */
    int numOfNominalValues() {
        return m_nominalValues.getIntValue();
    }
    
    /** @return number of nominal values for output table */
    int numOfNominalValuesOutput() {
        return m_nominalValuesOutput.getIntValue();
    }
    
    /** @return nominal value and frequency for each column */
    Map<DataCell, Integer>[] getNominals() {
        return m_statTable.getNominalValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_computeMedian.saveSettingsTo(settings);
        m_nominalValues.saveSettingsTo(settings);
        m_nominalValuesOutput.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_computeMedian.validateSettings(settings);
        m_nominalValues.validateSettings(settings);
        m_nominalValuesOutput.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(int outIndex) {
        return m_hilite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettingsRO sett = NodeSettings.loadFromXML(new FileInputStream(
                new File(internDir, "statistic.xml.gz")));
        try {
            m_statTable = Statistics2Table.load(sett);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        NodeSettings sett = new NodeSettings("statistic.xml.gz");
        m_statTable.save(sett);
        sett.saveToXML(new FileOutputStream(
                new File(internDir, sett.getKey())));
    }
}
