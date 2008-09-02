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
 *   07.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.mdsprojection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.mds.MDSCellFactory;
import org.knime.base.node.mine.mds.MDSNodeDialog;
import org.knime.base.node.mine.sota.logic.SotaUtil;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSProjectionNodeModel extends NodeModel {
    
    /**
     * The default projection setting.
     */
    public static final boolean DEF_PROJECT_ONLY = true;    
    
    private SettingsModelIntegerBounded m_rowsModel = 
        MDSNodeDialog.getRowsModel();
    
    private SettingsModelBoolean m_useRowsModel = 
        MDSNodeDialog.getUseMaxRowsModel();
    
    private SettingsModelDoubleBounded m_learnrateModel = 
        MDSNodeDialog.getLearnrateModel();
    
    private SettingsModelIntegerBounded m_epochsModel = 
        MDSNodeDialog.getEpochModel();
    
    private SettingsModelIntegerBounded m_outputDimModel = 
        MDSNodeDialog.getOutputDimModel();
    
    private SettingsModelString m_distModel = 
        MDSNodeDialog.getDistanceModel();
    
    private SettingsModelFilterString m_colModel = 
        MDSNodeDialog.getColumnModel();
    
    private SettingsModelIntegerBounded m_seedModel = 
        MDSNodeDialog.getSeedModel();
    
    private SettingsModelFilterString m_fixedMdsColModel = 
        MDSProjectionNodeDialog.getFixedColumnModel();
    
    private SettingsModelBoolean m_projectOnly =
        MDSProjectionNodeDialog.getProjectOnlyModel();
    
    
    private MDSProjectionManager m_manager;
    
    private List<String> m_includeList;
    
    private boolean m_fuzzy = false;    
    
    /**
     * The index of the data table containing the fixed data.
     */
    static final int FIXED_DATA_INDEX = 0;
    
    /**
     * The index of the data table containing the input data.
     */
    static final int IN_DATA_INDEX = 1;
    
    
    /**
     * Creates a new instance of <code>MDSNodeModel</code>.
     */
    public MDSProjectionNodeModel() {
        super(2, 1);
        m_useRowsModel.addChangeListener(new CheckBoxChangeListener());
        checkUncheck();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex < 0 || outIndex >= getNrOutPorts()) {
            throw new IndexOutOfBoundsException("index=" + outIndex);
        }
        return getInHiLiteHandler(IN_DATA_INDEX);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 2;
        m_includeList = m_colModel.getIncludeList();
        StringBuffer buffer = new StringBuffer();
        
        //
        /// Check input data
        //
        
        // check number of selected columns
        List<String> allColumns = new ArrayList<String>();
        int numberCells = 0;
        int fuzzyCells = 0;
        for (int i = 0; i < inSpecs[IN_DATA_INDEX].getNumColumns(); i++) {
            allColumns.add(inSpecs[IN_DATA_INDEX].getColumnSpec(i).getName());
            if (m_includeList.contains(
                    inSpecs[IN_DATA_INDEX].getColumnSpec(i).getName())) {
                DataType type = inSpecs[IN_DATA_INDEX]
                                        .getColumnSpec(i).getType();
                if (SotaUtil.isNumberType(type)) {
                    numberCells++;
                } else if (SotaUtil.isFuzzyIntervalType(type)) {
                    fuzzyCells++;
                }
            }
        }
        
        // check if selected columns are still in spec
        for (String s : m_includeList) {
            if (!allColumns.contains(s)) {
                buffer.append("Selected column are not in spec !");
            }
        }
        
        // throw exception if number of selected columns is not valid.
        if (numberCells <= 0 && fuzzyCells <= 0) {
            buffer.append("Number of columns has to be "
                    + "greater than zero !");
        } else if (numberCells > 0 && fuzzyCells > 0) {
            buffer.append("Number cells and fuzzy cells must not be mixed !");
        } else if (fuzzyCells > 0) {
            m_fuzzy = true;
        } else if (numberCells > 0) {
            m_fuzzy = false;
        }

        //
        /// Check fixed data
        //
        
        int fixedNumberCells = 0;
        List<String> fixedColumns = new ArrayList<String>();
        for (int i = 0; i < inSpecs[FIXED_DATA_INDEX].getNumColumns(); i++) {
            fixedColumns.add(
                    inSpecs[FIXED_DATA_INDEX].getColumnSpec(i).getName());
            if (m_includeList.contains(
                    inSpecs[FIXED_DATA_INDEX].getColumnSpec(i).getName())) {
                DataType type = inSpecs[FIXED_DATA_INDEX]
                                        .getColumnSpec(i).getType();
                if (SotaUtil.isNumberType(type)) {
                    fixedNumberCells++;
                }
            }
        }
        
        if (numberCells <= 0) {
            buffer.append("Number of columns containing fixed data has " 
                    + "to be greater than zero !");
        }
        
        // check if selected columns are still in spec
        List<String> fixedColInlucdeList = m_fixedMdsColModel.getIncludeList();
        for (String s : fixedColInlucdeList) {
            if (!fixedColumns.contains(s)) {
                buffer.append("Selected fixed column are not in spec !");
            }
        }
        
        
        // if buffer throw exception
        if (buffer.length() > 0) {
            throw new InvalidSettingsException(buffer.toString());
        }
        
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        assert inData.length == 2;
        
        int rowsToUse = m_rowsModel.getIntValue();
        if (m_useRowsModel.getBooleanValue()) {
            rowsToUse = inData[IN_DATA_INDEX].getRowCount();
        }
        
        // Warn if number of rows is greater than chosen number of rows
        if (inData[IN_DATA_INDEX].getRowCount() > rowsToUse) {
            setWarningMessage("Maximal number of rows to report is less" 
                    + " than number of rows in input data table !");
        }

        // use only specified rows
        DataTable dataContainer = new DefaultDataArray(
                inData[IN_DATA_INDEX], 1, rowsToUse);
        
        // save BufferedDataTable with rows to use and ALL columns to generate
        // the output data table out of it.
        BufferedDataTable rowCutDataTable = 
            exec.createBufferedDataTable(dataContainer, exec);
        
        // use only specified columns
        if (m_includeList != null) {
            dataContainer = new FilterColumnTable(dataContainer, 
                    m_includeList.toArray(new String[m_includeList.size()]));
        }
        
        // create BufferedDataTable
        BufferedDataTable dataTableToUse = 
            exec.createBufferedDataTable(dataContainer, exec);
        
        // get the indices of the fixed mds columns
        List<String> fixedCols = m_fixedMdsColModel.getIncludeList();
        int[] fixedColsIndicies = new int[fixedCols.size()]; 
        int currentColIndex = 0;
        DataTableSpec spec = inData[FIXED_DATA_INDEX].getSpec();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (fixedCols.contains(spec.getColumnSpec(i).getName())) {
                fixedColsIndicies[currentColIndex] = i;
                currentColIndex++;
            }
        }
        
        // create MDS manager, init and train stuff
        m_manager = new MDSProjectionManager(m_outputDimModel.getIntValue(),
                m_distModel.getStringValue(), m_fuzzy, dataTableToUse,
                inData[FIXED_DATA_INDEX], fixedColsIndicies, exec);
        m_manager.setProjectOnly(m_projectOnly.getBooleanValue());
        m_manager.init(m_seedModel.getIntValue());
        m_manager.train(m_epochsModel.getIntValue(),
                m_learnrateModel.getDoubleValue());
        
        // create BufferedDataTable out of mapped data.
        ColumnRearranger rearranger = new ColumnRearranger(
                rowCutDataTable.getDataTableSpec());
        rearranger.append(new MDSCellFactory(m_manager.getDataPoints(), 
                m_manager.getDimension()));
        
        return new BufferedDataTable[] {
                exec.createColumnRearrangeTable(rowCutDataTable, rearranger, 
                exec.createSubProgress(0.1))};
    }

    /**
     * Creates the <code>DataTableSpec</code> of the output data table.
     * 
     * @param inPsec The <code>DataTableSpec</code> of the input data table.
     * @param dimensions The dimensions of the output data.
     * @return The <code>DataTableSpec</code> of the output data table.
     */
    static DataTableSpec createDataTableSpec(final DataTableSpec inPsec,
            final int dimensions) {
        return new DataTableSpec(inPsec, 
                new DataTableSpec(getColumnSpecs(dimensions)));
    }
    
    /**
     * The <code>DataColumnSpec</code>s of the mds data (columns).
     * 
     * @param dimensions The dimension of the mds data. 
     * @return The <code>DataColumnSpec</code>s of the mds data.
     */
    static DataColumnSpec[] getColumnSpecs(final int dimensions) {
        DataColumnSpec[] specs = new DataColumnSpec[dimensions]; 
        for (int i = 0; i < dimensions; i++) {
            DataColumnSpecCreator creator =
                new DataColumnSpecCreator("MDS Col " + (i + 1), 
                        DoubleCell.TYPE);
            specs[i] = creator.createSpec();
            
        }
        return specs;
    }
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colModel.loadSettingsFrom(settings);
        m_distModel.loadSettingsFrom(settings);
        m_epochsModel.loadSettingsFrom(settings);
        m_learnrateModel.loadSettingsFrom(settings);
        m_outputDimModel.loadSettingsFrom(settings);
        m_rowsModel.loadSettingsFrom(settings);
        m_seedModel.loadSettingsFrom(settings);
        m_useRowsModel.loadSettingsFrom(settings);
        m_fixedMdsColModel.loadSettingsFrom(settings);
        m_projectOnly.loadSettingsFrom(settings);
        checkUncheck();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_manager != null) {
            m_manager.reset();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colModel.saveSettingsTo(settings);
        m_distModel.saveSettingsTo(settings);
        m_epochsModel.saveSettingsTo(settings);
        m_learnrateModel.saveSettingsTo(settings);
        m_outputDimModel.saveSettingsTo(settings);
        m_rowsModel.saveSettingsTo(settings);
        m_seedModel.saveSettingsTo(settings);
        m_useRowsModel.saveSettingsTo(settings);
        m_fixedMdsColModel.saveSettingsTo(settings);
        m_projectOnly.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colModel.validateSettings(settings);
        m_distModel.validateSettings(settings);
        m_epochsModel.validateSettings(settings);
        m_learnrateModel.validateSettings(settings);
        m_outputDimModel.validateSettings(settings);
        m_rowsModel.validateSettings(settings);
        m_seedModel.validateSettings(settings);
        m_useRowsModel.validateSettings(settings);
        m_fixedMdsColModel.validateSettings(settings);
        m_projectOnly.validateSettings(settings);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    
    
    /**
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class CheckBoxChangeListener implements ChangeListener {

        /**
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            checkUncheck();
        }
    }
    
    private void checkUncheck() {
        if (m_useRowsModel.getBooleanValue()) {
            m_rowsModel.setEnabled(false);
        } else {
            m_rowsModel.setEnabled(true);
        }
    }    
}
