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
 *   11.10.2005 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.scatterplot.ScatterPlotNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
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
 * This is the Node Model for the Fuzzy Rule Plotter Node.
 * 
 * @author Fabian Dill
 */
public class Rule2DNodeModel extends NodeModel implements Rule2DDataProvider {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Rule2DNodeModel.class);

    private static final String RULE_FILE_NAME = "rule2D_rules";

    private static final String DATA_FILE_NAME = "rule2D_data";

    /**
     * A constant for the inport the data is connected to.
     */
    private static final int DATA_INPORT = 0;

    /**
     * A constant for the inport the rules are connected to.
     */
    private static final int RULES_INPORT = 1;
    
    /** Config key for the maximum number of rule rows. */
    public static final String CFG_RULES_MAX_ROWS = "ruleMaxRows";
    
    /** Config key for the start rule row. */
    public static final String CFG_RULES_START_ROW = "ruleStartRow";
    
    private int m_dataStartRow = 1;
    private int m_dataMaxRow = 10000;
    
    private int m_ruleStartRow = 1;
    private int m_ruleMaxRow = 1000;  

    /**
     * The data points.
     */
    private DataArray m_data;

    /**
     * The fuzzy rules.
     */
    private DataArray m_fuzzyRules;
    

    /**
     * Creates an instance of the Node Model. The 2 inports are as follows:
     * input1: data input2: fuzzy rules
     */
    public Rule2DNodeModel() {
        super(2, 0);
    }

    /**
     * Checks if there are the 2 required in-ports.
     * 
     * @param inSpecs - the incoming DataTable Specs.
     * @return - an empty DataTableSpec array since there are no output-ports.
     * @see org.knime.core.node.NodeModel#configure(DataTableSpec[])
     * @throws InvalidSettingsException if the specs are null or their length
     *             not 2.
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs == null || inSpecs.length != 2) {
            throw new InvalidSettingsException("need 2 inports");
        }
        DataTableSpec dataSpec = inSpecs[DATA_INPORT];
        DataTableSpec ruleSpec = inSpecs[RULES_INPORT];
        List<String>validColummNames = new ArrayList<String>();
        for (int i = 0; i < dataSpec.getNumColumns(); i++) {
            String colName = dataSpec.getColumnSpec(i).getName();
            if (ruleSpec.getColumnSpec(colName) != null) {
                if (dataSpec.getColumnSpec(i).getName().equals(
                        ruleSpec.getColumnSpec(colName).getName())
                        && ruleSpec.getColumnSpec(colName).getType()
                            .isCompatible(FuzzyIntervalValue.class)) {
                    validColummNames.add(dataSpec.getColumnSpec(i).getName());
                }
            }
        }
        if (validColummNames.size() == 0) {
            throw new InvalidSettingsException("Data must have the same column" 
                    + " names and the rules must be fuzzy intervals!");
        }
        return new DataTableSpec[]{};
    }

    /**
     * Reads in the data and the rules and simply stores them locally.
     * 
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        exec.setProgress(0.2, "Process input data...");
        assert inData != null;
        assert inData.length == 2;
        m_data = new DefaultDataArray(inData[DATA_INPORT], m_dataStartRow,
                m_dataMaxRow);
        m_fuzzyRules = new DefaultDataArray(inData[RULES_INPORT], 
                m_ruleStartRow, m_ruleMaxRow);
        LOGGER.debug("model rules: " + m_fuzzyRules);
        exec.setProgress(0.8, "Process input data...");
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_dataStartRow = settings.getInt(ScatterPlotNodeModel.CFGKEY_FROMROW);
        m_dataMaxRow = settings.getInt(ScatterPlotNodeModel.CFGKEY_ROWCNT);
        m_ruleStartRow = settings.getInt(CFG_RULES_START_ROW);
        m_ruleMaxRow = settings.getInt(CFG_RULES_MAX_ROWS);
    }

    /**
     * Sets the locally stored data and rules to null.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_data = null;
        m_fuzzyRules = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(ScatterPlotNodeModel.CFGKEY_FROMROW, m_dataStartRow);
        settings.addInt(ScatterPlotNodeModel.CFGKEY_ROWCNT, m_dataMaxRow);
        settings.addInt(CFG_RULES_START_ROW, m_ruleStartRow);
        settings.addInt(CFG_RULES_MAX_ROWS, m_ruleMaxRow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getInt(ScatterPlotNodeModel.CFGKEY_FROMROW);
        settings.getInt(ScatterPlotNodeModel.CFGKEY_ROWCNT);
        settings.getInt(CFG_RULES_START_ROW);
        settings.getInt(CFG_RULES_MAX_ROWS);
    }

    /**
     * Returns the locally stored fuzzy rules.
     * 
     * @return - the fuzzy rules.
     */
    public DataArray getRules() {
        return m_fuzzyRules;
    }

    /**
     * Returns the locally stored data.
     * 
     * @return - the data points.
     */
    public DataArray getDataPoints() {
        return m_data;
    }
    

    /**
     * Load internals.
     * 
     * @param internDir The intern node directory.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @throws CanceledExecutionException -if the user abnorts the operation.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File rules = new File(internDir, RULE_FILE_NAME);
        
        ContainerTable ruleTable = DataContainer.readFromZip(rules);
        int rowCount = ruleTable.getRowCount();
        m_fuzzyRules = new DefaultDataArray(ruleTable, 1, rowCount, exec);
        File data = new File(internDir, DATA_FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(data);
        rowCount = table.getRowCount();
        m_data = new DefaultDataArray(table, 1, rowCount, exec);
    }

    /**
     * Save internals.
     * 
     * @param internDir The intern node directory.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @throws CanceledExecutionException - if user cancels operation.
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // throw new IOException("2D Rule view can't be saved yet.");
        if (m_fuzzyRules != null) {
            File rules = new File(internDir, RULE_FILE_NAME);
            DataContainer.writeToZip(m_fuzzyRules, rules, exec);
        }
        if (m_data != null) {
            File data = new File(internDir, DATA_FILE_NAME);
            DataContainer.writeToZip(m_data, data, exec);
        }
    }

}
