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
import java.util.ArrayList;
import java.util.Map;

import org.knime.base.data.statistics.Statistics2Table;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The StatisticsNodeModel creates a new StatisticTable based on the input data
 * table.
 * 
 * @author Thomas Gabriel, University of Konstanz
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
    
    private final SettingsModelFilterString m_nominalFilter =
        Statistics2NodeDialogPane.createNominalFilterModel();
    
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
        DataTableSpec numericSpec =
            Statistics2Table.createOutSpecNumeric(inSpecs[0]);
        ArrayList<String> nominalValues = new ArrayList<String>(
                m_nominalFilter.getIncludeList());
        if (nominalValues.isEmpty() 
                && m_nominalFilter.getExcludeList().isEmpty()) {
            for (DataColumnSpec cspec : inSpecs[0]) {
                nominalValues.add(cspec.getName());
            }
            m_nominalFilter.setIncludeList(nominalValues);
        }
        DataTableSpec nominalSpec = Statistics2Table.createOutSpecNominal(
                inSpecs[0], nominalValues);
        return new DataTableSpec[]{numericSpec, nominalSpec};
                
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
                numOfNominalValuesOutput(), m_nominalFilter.getIncludeList(), 
                exec);
        if (m_statTable.getWarning() != null) {
        	super.setWarningMessage(m_statTable.getWarning());
        }
        BufferedDataTable outTable1 = exec.createBufferedDataTable(
                m_statTable.createStatisticMomentsTable(), 
                exec.createSubProgress(0.5));
        BufferedDataTable outTable2 = exec.createBufferedDataTable(
                m_statTable.createNominalValueTable(
                		m_nominalFilter.getIncludeList()), 
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
        m_nominalFilter.loadSettingsFrom(settings);
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
     * @return columns used to count co-occurrences
     */
    String[] getNominalColumnNames() {
        if (m_statTable == null) {
            return null;
        }
        return m_statTable.extractNominalColumns(
                m_nominalFilter.getIncludeList());
    }

    /**
     * @return all column names
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
        m_nominalFilter.saveSettingsTo(settings);
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
        m_nominalFilter.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
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
