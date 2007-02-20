/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeModel extends NodeModel {

    /**
     * The input port used here.
     */
    public static final int INPORT = 0;

    private SotaManager m_sota;

    private ArrayList<String> m_includeList;

    private ArrayList<String> m_excludeList;

    private String m_hierarchieLevelCell;
    
    private static final String CFG_KEY_USE_FUZZY_HIERARCHY = "FuzzyHierarchy";
    
    private static final String CFG_KEY_MAX_FUZZY_LEVEL = "MaxFuzzyLevel";
    
    private static final String CFG_KEY_INDATA_SIZE = "InDataContainerSize";
    
    private static final String CFG_KEY_ORIGDATA_SIZE = "OrigDataContainerSize";
    
    private static final String INTERNAL_MODEL = "SotaModel";
    
    private static final String TREE_FILE = "tree.sota";

    private static final String IN_DATA_FILE = "indata.sota";

    private static final String ORIG_DATA_FILE = "origdata.sota";

    /**
     * Constructor of SoteNodeModel. Creates new instance of SotaNodeModel, with
     * default settings.
     */
    public SotaNodeModel() {
        super(1, 0, 0, 0);
        m_sota = new SotaManager();
        m_includeList = new ArrayList<String>();
        m_excludeList = new ArrayList<String>();
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_sota.saveSettingsTo(settings);

        // Save ColumnFilterPanel settings
        settings.addStringArray(SotaConfigKeys.CFGKEY_INCLUDE, m_includeList
                .toArray(new String[m_includeList.size()]));
        settings.addStringArray(SotaConfigKeys.CFGKEY_EXCLUDE, m_excludeList
                .toArray(new String[m_excludeList.size()]));
        settings.addBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA,
                m_sota.isUseHierarchicalFuzzyData());
        settings.addString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL,
                m_hierarchieLevelCell);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */true);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */false);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        final DataArray origRowContainer = new DefaultDataArray(
                inData[SotaNodeModel.INPORT], 1, Integer.MAX_VALUE);
        DataTable dataTableToUse = inData[SotaNodeModel.INPORT];
        if (m_includeList != null) {
            if (m_sota.isUseHierarchicalFuzzyData()) {
                if (m_hierarchieLevelCell != null) {
                    m_includeList.add(m_hierarchieLevelCell);
                }
            }

            DataTable filteredDataTable = new FilterColumnTable(
                    inData[SotaNodeModel.INPORT], m_includeList
                            .toArray(new String[m_includeList.size()]));
            dataTableToUse = filteredDataTable;
        }

        m_sota.initializeTree(dataTableToUse, origRowContainer, exec);
        m_sota.doTraining();

        return new BufferedDataTable[]{};
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_sota.reset();
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        assert inSpecs.length == 1;

        int numberCells = 0;
        int fuzzyCells = 0;
        int intCells = 0;
        for (int i = 0; i < inSpecs[SotaNodeModel.INPORT].getNumColumns(); 
            i++) {
            if (m_includeList.contains(inSpecs[SotaNodeModel.INPORT]
                    .getColumnSpec(i).getName())) {
                DataType type = inSpecs[SotaNodeModel.INPORT].getColumnSpec(i)
                        .getType();

                if (SotaUtil.isIntType(type)) {
                    intCells++;
                }
                if (SotaUtil.isNumberType(type)) {
                    numberCells++;
                } else if (SotaUtil.isFuzzyIntervalType(type)) {
                    fuzzyCells++;
                }
            }
        }

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < m_includeList.size(); i++) {
            if (!inSpecs[0].containsName(m_includeList.get(i))) {
                buffer.append("Column " + m_includeList.get(i)
                        + " not found in spec.");
            }
        }

        if (numberCells > 0 && fuzzyCells > 0) {
            buffer.append("FuzzyIntervalCells and NumberCells are mixed !");
        }

        if (numberCells <= 0 && fuzzyCells <= 0) {
            buffer.append("Number of columns has to be "
                    + "greater than zero !");
        }

        if (fuzzyCells <= 0 && m_sota.isUseHierarchicalFuzzyData()) {
            buffer.append("No fuzzy cells selected !");
        }

        if (m_sota.isUseHierarchicalFuzzyData()
                && m_hierarchieLevelCell == null) {
            buffer.append("No hierarchy column selected !");
        }

        // if buffer throw exception
        if (buffer.length() > 0) {
            throw new InvalidSettingsException(buffer.toString());
        }

        return new DataTableSpec[]{};
    }

    private void readSettings(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {
        m_sota.readSettings(settings, validateOnly);

        String msg = "";

        //
        // / Validate ColumnFilterPanel settings
        //
        String[] dataCellsEx = settings
                .getStringArray(SotaConfigKeys.CFGKEY_EXCLUDE);
        String[] dataCellsIn = settings
                .getStringArray(SotaConfigKeys.CFGKEY_INCLUDE);

        boolean useHFD = settings
                .getBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA);

        // If number of input columns is less or equal 2 warn user !
        if (dataCellsIn.length <= 0) {
            msg += "Number of columns has to be greater than zero, not "
                    + dataCellsIn.length;
        }

        //
        // / Throw exception and warn if errors in settings
        //
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg);
        }

        // now take them over - if we are supposed to.
        if (!validateOnly) {
            // clear include column list
            m_includeList.clear();
            // get list of included columns
            for (int i = 0; i < dataCellsIn.length; i++) {
                m_includeList.add(dataCellsIn[i]);
            }

            // clear include column list
            m_excludeList.clear();
            // get list of included columns
            for (int i = 0; i < dataCellsEx.length; i++) {
                m_excludeList.add(dataCellsEx[i]);
            }

            m_hierarchieLevelCell = settings
                    .getString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL);

            m_sota.setUseHierarchicalFuzzyData(useHFD);
        }
    }

    /**
     * @return the m_sota
     */
    public SotaManager getSotaManager() {
        return m_sota;
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        
        File file = new File(internDir, TREE_FILE);
        FileInputStream fis = new FileInputStream(file);
        ModelContentRO modelContent = ModelContent.loadFromXML(fis);        

        // Load settings
        int inDataSize = 0;
        int origDataSize = 0;
        try {
            m_sota.setUseHierarchicalFuzzyData(modelContent.getBoolean(
                    CFG_KEY_USE_FUZZY_HIERARCHY));
            m_sota.setMaxHierarchicalLevel(modelContent.getInt(
                    CFG_KEY_MAX_FUZZY_LEVEL));
            inDataSize = modelContent.getInt(CFG_KEY_INDATA_SIZE);
            origDataSize = modelContent.getInt(CFG_KEY_ORIGDATA_SIZE);
        } catch (InvalidSettingsException e1) {
            IOException ioe = new IOException("Could not load settings," 
                    + "due to invalid settings in model content !");
            ioe.initCause(e1);
            fis.close();
            throw ioe;
        }
        
        // Load in data
        DataTable table = DataContainer.readFromZip(new File(internDir,
                IN_DATA_FILE));
        final DataArray inData = new DefaultDataArray(table, 1, inDataSize);
        m_sota.setInData(inData);

        // Load orig data
        table = DataContainer.readFromZip(new File(internDir, ORIG_DATA_FILE));
        final DataArray origData = new DefaultDataArray(table, 1, origDataSize);
        m_sota.setOriginalData(origData);

        
        // Load tree
        SotaTreeCell root = new SotaTreeCell(0, false);
        try {
            root.loadFrom(modelContent, 0, null, false);
        } catch (InvalidSettingsException e) {
            IOException ioe = new IOException("Could not load tree cells,"
                    + "due to invalid settings in model content !");
            ioe.initCause(e);
            fis.close();
            throw ioe;
        }
        m_sota.setRoot(root);
        
        fis.close();
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {

        // Save in data container
        DataContainer.writeToZip(m_sota.getInDataContainer(), new File(
                    internDir, IN_DATA_FILE), exec);

        // Save original Data
        DataContainer.writeToZip(m_sota.getOriginalData(), new File(
                    internDir, ORIG_DATA_FILE), exec);

        // Save tree
        ModelContent modelContent = new ModelContent(INTERNAL_MODEL);
        m_sota.getRoot().saveTo(modelContent, 0);

        // Save settings
        modelContent.addBoolean(CFG_KEY_USE_FUZZY_HIERARCHY, 
                m_sota.isUseHierarchicalFuzzyData());
        modelContent.addInt(CFG_KEY_MAX_FUZZY_LEVEL, 
                m_sota.getMaxHierarchicalLevel());
        modelContent.addInt(CFG_KEY_INDATA_SIZE,
                m_sota.getInDataContainer().size());
        modelContent.addInt(CFG_KEY_ORIGDATA_SIZE,
                m_sota.getOriginalData().size());
        
        
        File file = new File(internDir, TREE_FILE);
        FileOutputStream fos = new FileOutputStream(file);
        modelContent.saveToXML(fos);
        fos.close();
    }
}
