/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   25.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.lowvarfilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
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
 * NodeModel for low variance filter node.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LowVarFilterNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(LowVarFilterNodeModel.class);
    
    /** Config key for variance threshold. */
    static final String CFG_KEY_MAX_VARIANCE = "max_variance";
    /** Config key for included columns. */
    static final String CFG_KEY_COL_FILTER = "col_filter";
    
    private String[] m_includedColumns;
    private double m_varianceThreshold;
    
    /** One input, one output. */
    public LowVarFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        StatisticsTable statTable = new StatisticsTable(inData[0], exec);
        ArrayList<String> includes = new ArrayList<String>();
        DataTableSpec s = inData[0].getDataTableSpec();
        int colCount = s.getNumColumns();
        double threshold = m_varianceThreshold;
        HashSet<String> includesHash = 
            new HashSet<String>(Arrays.asList(m_includedColumns));
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec cs = s.getColumnSpec(i);
            if (!includesHash.contains(cs.getName())
                    || !cs.getType().isCompatible(DoubleValue.class)
                    || statTable.getVariance(i) > threshold) {
                includes.add(cs.getName());
            } 
        }
        int filteredOutCount = s.getNumColumns() - includes.size();
        LOGGER.info("Filtered out " + filteredOutCount + " column(s)");
        if (filteredOutCount == 0) {
            setWarningMessage("No columns were filtered out.");
        }
        ColumnRearranger rearranger = new ColumnRearranger(s);
        rearranger.keepOnly(includes.toArray(new String[includes.size()]));
        BufferedDataTable t = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        return new BufferedDataTable[]{t};
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_includedColumns == null) { // auto-guessing
            List<String> defIncludes = new ArrayList<String>();
            for (DataColumnSpec s : inSpecs[0]) {
                if (s.getType().isCompatible(DoubleValue.class)) {
                    defIncludes.add(s.getName());
                }
            }
            m_includedColumns = 
                defIncludes.toArray(new String[defIncludes.size()]);
            m_varianceThreshold = 0.0;
            setWarningMessage("Auto-configuration: Using all double-compatible "
                    + "columns (int total " + defIncludes.size() + ") and " 
                    + "a threshold value of 0");
        }

        if (m_varianceThreshold < 0.0) {
            throw new InvalidSettingsException("Not configured: Please set " 
                    + "variance threshold to value >= 0");
        }
        HashSet<String> hash = 
            new LinkedHashSet<String>(Arrays.asList(m_includedColumns)); 
        for (DataColumnSpec s : inSpecs[0]) {
            hash.remove(s.getName());
        }
        if (!hash.isEmpty()) {
            StringBuilder missing = new StringBuilder();
            Iterator<String> it = hash.iterator();
            for (int i = 0; i < 3 && it.hasNext(); i++) {
                if (i != 0) {
                    missing.append(", ");
                }
                missing.append('\"');
                missing.append(it.next());
                missing.append('\"');
            }
            if (it.hasNext()) {
                missing.append(", ...");
            }
            throw new InvalidSettingsException("No such column(s): " + missing);
        }
        // unable to say anything about the outspec here.
        return new DataTableSpec[1];
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_includedColumns != null) {
            settings.addStringArray(CFG_KEY_COL_FILTER, m_includedColumns);
            settings.addDouble(CFG_KEY_MAX_VARIANCE, m_varianceThreshold);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns = settings.getStringArray(CFG_KEY_COL_FILTER);
        m_varianceThreshold = settings.getDouble(CFG_KEY_MAX_VARIANCE); 
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String[] includes = settings.getStringArray(CFG_KEY_COL_FILTER);
        if (includes == null || includes.length == 0) {
            throw new InvalidSettingsException(
                    "No columns for low variance filtering included.");
        }
        if (Arrays.asList(includes).contains(null)) {
            throw new InvalidSettingsException(
                    "Null elements not allowed in include list");
        }
        double varThresh = settings.getDouble(CFG_KEY_MAX_VARIANCE); 
        if (varThresh < 0.0) {
            throw new InvalidSettingsException(
                    "Negative variance not allowed: " + varThresh);
        }
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
    
}
