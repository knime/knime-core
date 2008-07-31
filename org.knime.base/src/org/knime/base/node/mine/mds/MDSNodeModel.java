/*
 * ------------------------------------------------------------------
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
package org.knime.base.node.mine.mds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.mds.distances.DistanceManagerFactory;
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

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSNodeModel extends NodeModel {
    
    /**
     * The default number of rows to use.
     */
    public static final int DEF_NO_ROWS = 1000;
    
    /**
     * The default settings of the usage of the number of max rows.
     */
    public static final boolean DEF_USE_MAX_ROWS = true;
    
    /**
     * The minimum number of rows to use.
     */
    public static final int MIN_NO_ROWS = 1;   
    
    /**
     * The maximum number of rows to use.
     */
    public static final int MAX_NO_ROWS = Integer.MAX_VALUE;
    
    /**
     * The default value of the learning rate. 
     */
    public static final double DEF_LEARNINGRATE = 1.0;

    /**
     * The minimum value of the learning rate. 
     */
    public static final double MIN_LEARNINGRATE = 0.0;
    
    /**
     * The maximum value of the learning rate. 
     */
    public static final double MAX_LEARNINGRATE = 1.0;
    
    /**
     * The default value of the epochs.
     */
    public static final int DEF_EPOCHS = 50;

    /**
     * The minimum value of the epochs.
     */
    public static final int MIN_EPOCHS = 1;
    
    /**
     * The maximum value of the epochs.
     */
    public static final int MAX_EPOCHS = Integer.MAX_VALUE;
    
    /**
     * The default value of the output dimension.
     */
    public static final int DEF_OUTPUTDIMS = 2;

    /**
     * The minimum value of the output dimension.
     */
    public static final int MIN_OUTPUTDIMS = 1;
    
    /**
     * The maximum value of the output dimension.
     */
    public static int maxOutputDims = 3;
    
    /**
     * The default value of the distance to use.
     */
    public static final String DEF_DISTANCE = 
        DistanceManagerFactory.EUCLIDEAN_DIST;
    
    
    
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
    
    private SettingsModelString m_distModel = MDSNodeDialog.getDistanceModel();
    
    private SettingsModelFilterString m_colModel = 
        MDSNodeDialog.getColumnModel();
    
    private SettingsModelIntegerBounded m_seedModel = 
        MDSNodeDialog.getSeedModel();
    
    private MDSManager m_manager;
    
    private List<String> m_includeList;
    
    private boolean m_fuzzy = false;
    
    /**
     * Creates a new instance of <code>MDSNodeModel</code>.
     */
    public MDSNodeModel() {
        super(1, 1);
        m_useRowsModel.addChangeListener(new CheckBoxChangeListener());
        checkUncheck();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 1;
        m_includeList = m_colModel.getIncludeList();
        List<String> allColumns = new ArrayList<String>();
        StringBuffer buffer = new StringBuffer();
        
        // check number of selected columns
        int numberCells = 0;
        int fuzzyCells = 0;
        for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
            allColumns.add(inSpecs[0].getColumnSpec(i).getName());
            if (m_includeList.contains(inSpecs[0].getColumnSpec(i).getName())) {
                DataType type = inSpecs[0].getColumnSpec(i).getType();

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

        // if buffer throw exception
        if (buffer.length() > 0) {
            throw new InvalidSettingsException(buffer.toString());
        }

        maxOutputDims = numberCells - 1;
        if (m_fuzzy) {
            maxOutputDims = fuzzyCells - 1;
        }
        if (maxOutputDims < 1) {
            maxOutputDims = 1;
        }
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        int rowsToUse = m_rowsModel.getIntValue();
        if (m_useRowsModel.getBooleanValue()) {
            rowsToUse = inData[0].getRowCount();
        }
        
        // Warn if number of rows is greater than chosen number of rows
        if (inData[0].getRowCount() > rowsToUse) {
            setWarningMessage("Maximal number of rows to report is less" 
                    + " than number of rows in input data table !");
        }

        // use only specified rows
        DataTable dataContainer = new DefaultDataArray(
                inData[0], 1, rowsToUse);
        
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
        
        // create MDS manager, init and train stuff
        m_manager = new MDSManager(m_outputDimModel.getIntValue(),
                m_distModel.getStringValue(), m_fuzzy, dataTableToUse,
                exec.createSubProgress(0.9));
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
