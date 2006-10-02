/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SotaNodeModel.class);

    /**
     * The input port used here.
     */
    public static final int INPORT = 0;

    private SotaManager m_sota;

    private ArrayList<String> m_includeList;

    private ArrayList<String> m_excludeList;

    private String m_hierarchieLevelCell;

    private static final String TREE_FILE = "tree.sota";

    private static final String SETTINGS_FILE = "settings.sota";

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

        // notify Views that training is done and repaint has to be done
        this.notifyViews(new Object());

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

        // Load Settings
        int origDataSize = 0;
        int inDataSize = 0;
        int numberOfCells = 0;
        FileInputStream in = new FileInputStream(new File(internDir,
                SETTINGS_FILE));
        ObjectInputStream s = new ObjectInputStream(in);
        try {
            Object isHierarchicalFuzzyData = s.readObject();
            Object maxHierarchicalLevel = s.readObject();

            m_sota.setUseHierarchicalFuzzyData(((Boolean)
                    isHierarchicalFuzzyData).booleanValue());
            m_sota.setMaxHierarchicalLevel(((Integer)maxHierarchicalLevel)
                    .intValue());

            inDataSize = ((Integer)s.readObject()).intValue();
            origDataSize = ((Integer)s.readObject()).intValue();
            numberOfCells = ((Integer)s.readObject()).intValue();
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Class not found when loading internal settings !");
            e.printStackTrace();
        }
        in.close();

        // Load in data
        DataTable table = DataContainer.readFromZip(new File(internDir,
                IN_DATA_FILE));
        final DataArray inData = new DefaultDataArray(table, 1, inDataSize);
        m_sota.setInData(inData);

        // Load orig data
        table = DataContainer.readFromZip(new File(internDir, ORIG_DATA_FILE));
        final DataArray origData = new DefaultDataArray(table, 1, origDataSize);
        m_sota.setOriginalData(origData);

        // Load Tree
        in = new FileInputStream(new File(internDir, TREE_FILE));
        s = new ObjectInputStream(in);
        try {
            for (int i = 1; i < numberOfCells; i++) {
                s.readObject();
            }

            Object root = s.readObject();
            m_sota.setRoot((SotaTreeCell)root);

        } catch (ClassNotFoundException e) {
            LOGGER.debug("Could not load cells !");
            e.printStackTrace();
        }
        in.close();
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        FileOutputStream out;
        ObjectOutputStream s;

        // Save in data container
        try {
            DataContainer.writeToZip(m_sota.getInDataContainer(), new File(
                    internDir, IN_DATA_FILE), exec);
        } catch (CanceledExecutionException e) {
            LOGGER.debug("Saving of in data was canceled !");
            e.printStackTrace();
        }

        // Save original Data
        try {
            DataContainer.writeToZip(m_sota.getOriginalData(), new File(
                    internDir, ORIG_DATA_FILE), exec);
        } catch (CanceledExecutionException e) {
            LOGGER.debug("Saving of original data was canceled !");
            e.printStackTrace();
        }

        // Save tree
        out = new FileOutputStream(new File(internDir, TREE_FILE));
        s = new ObjectOutputStream(out);
        int cells = m_sota.getRoot().writeToFile(s, 0);
        out.close();

        // Save settings
        out = new FileOutputStream(new File(internDir, SETTINGS_FILE));
        s = new ObjectOutputStream(out);
        s.writeObject(m_sota.isUseHierarchicalFuzzyData());
        s.writeObject(m_sota.getMaxHierarchicalLevel());
        s.writeObject(m_sota.getInDataContainer().size());
        s.writeObject(m_sota.getOriginalData().size());
        s.writeObject(cells);
        s.flush();
        out.close();
    }
}
