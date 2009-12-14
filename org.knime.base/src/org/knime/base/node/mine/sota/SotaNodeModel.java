/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.base.node.mine.sota.logic.SotaUtil;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeModel extends NodeModel {

    /**
     * The input port used here.
     */
    public static final int INPORT = 0;

    /**
     * The default value for the usage of out data.
     */
    public static final boolean DEFAULT_USE_OUTDATA = true;
    
    /**
     * The configuration key for the internal model of SOTA.
     */
    public static final String INTERNAL_MODEL = "SotaModel";
    
    /**
     * The file to save the internal structire of SOTA.
     */
    private static final String TREE_FILE = "tree.sota";

    /**
     * The file to save the in data for SOTA.
     */
    private static final String IN_DATA_FILE = "indata.sota";

    /**
     * The file to save the original data for SOTA.
     */
    private static final String ORIG_DATA_FILE = "origdata.sota";    
    
    
    private SotaManager m_sota;

    private String m_hierarchieLevelCell;
   
    private boolean m_withOutPort = false;
    
    private SettingsModelString m_classCol =
        new SettingsModelString(SotaConfigKeys.CFGKEY_CLASSCOL, "");
    
    private SettingsModelBoolean m_useOutData =
        new SettingsModelBoolean(SotaConfigKeys.CFGKEY_USE_CLASS_DATA, 
                SotaNodeModel.DEFAULT_USE_OUTDATA);    
    
    
    /**
     * Constructor of SoteNodeModel. Creates new instance of SotaNodeModel, with
     * default settings and one out port by default.
     */
    public SotaNodeModel() {
        this(true);
    }

    /**
     * Constructor of SoteNodeModel. Creates new instance of SotaNodeModel, with
     * default settings. If <code>withOutPort</code> is set true one out port
     * will be created otherwise no out port will be created.
     * 
     * @param withOutPort If set true one out port will be created otherwise
     * not.
     */
    public SotaNodeModel(final boolean withOutPort) {
        super(new PortType[]{BufferedDataTable.TYPE}, outPorts(withOutPort));
        m_sota = new SotaManager();
        m_withOutPort = withOutPort;
    }
    
    private static final PortType[] outPorts(final boolean withOutPort) {
        if (withOutPort) {
            return new PortType[]{new PortType(SotaPortObject.class)};
        }
        return new PortType[]{null};
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        assert inSpecs.length == 1;
        
        DataTableSpec inDataSpec = (DataTableSpec)inSpecs[SotaNodeModel.INPORT];
        
        int numberCells = 0;
        int fuzzyCells = 0;
        int intCells = 0;
        for (int i = 0; i < inDataSpec.getNumColumns(); i++) {
            DataType type = inDataSpec.getColumnSpec(i).getType();

            if (SotaUtil.isIntType(type)) {
                intCells++;
            }
            if (SotaUtil.isNumberType(type)) {
                numberCells++;
            } else if (SotaUtil.isFuzzyIntervalType(type)) {
                fuzzyCells++;
            }
        }

        StringBuffer buffer = new StringBuffer();

        if (numberCells > 0 && fuzzyCells > 0) {
            buffer.append("FuzzyIntervalCells and NumberCells are mixed ! ");
        }

        if (numberCells <= 0 && fuzzyCells <= 0) {
            buffer.append("Number of columns has to be "
                    + "greater than zero ! ");
        }

        if (fuzzyCells <= 0 && m_sota.isUseHierarchicalFuzzyData()) {
            buffer.append("No fuzzy cells selected ! ");
        }

        if (m_sota.isUseHierarchicalFuzzyData()
                && m_hierarchieLevelCell == null) {
            buffer.append("No hierarchy column selected ! ");
        }

        // if buffer throw exception
        if (buffer.length() > 0) {
            throw new InvalidSettingsException(buffer.toString());
        }

        int classColIndex = inDataSpec.findColumnIndex(
                m_classCol.getStringValue());
        return new PortObjectSpec[]{new SotaPortObjectSpec(inDataSpec, 
                classColIndex)};
    }    
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        if (!(inData[SotaNodeModel.INPORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Given indata port object is " 
                    + " no BufferedDataTable!");
        }
        
        BufferedDataTable bdt = (BufferedDataTable)inData[SotaNodeModel.INPORT];
        
        final DataArray origRowContainer = new DefaultDataArray(
                bdt, 1, Integer.MAX_VALUE);
        DataTable dataTableToUse = bdt;
        int indexOfClassCol = -1;
        // get index of column containing class information
        indexOfClassCol = dataTableToUse.getDataTableSpec().findColumnIndex(
                m_classCol.getStringValue());
            
            m_sota.initializeTree(dataTableToUse, origRowContainer, exec, 
                    indexOfClassCol);
            m_sota.doTraining();

        if (m_withOutPort) {
            return new PortObject[]{new SotaPortObject(m_sota, 
                    dataTableToUse.getDataTableSpec(), indexOfClassCol)};
        }
        return new PortObject[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_sota.reset();
    }

    /**
     * @return the m_sota
     */
    public SotaManager getSotaManager() {
        return m_sota;
    }

    
    
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_sota.saveSettingsTo(settings);

        // Save ColumnFilterPanel settings
        settings.addBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA,
                m_sota.isUseHierarchicalFuzzyData());
        settings.addString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL,
                m_hierarchieLevelCell);
        
        m_classCol.saveSettingsTo(settings);
        m_useOutData.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */true);
        
        m_classCol.validateSettings(settings);
        m_useOutData.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */false);
        
        m_classCol.loadSettingsFrom(settings);
        m_useOutData.loadSettingsFrom(settings);
    }
    
    private void readSettings(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {
        m_sota.readSettings(settings, validateOnly);

        String msg = "";

        boolean useHFD = settings
                .getBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA);

        //
        // / Throw exception and warn if errors in settings
        //
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg);
        }

        // now take them over - if we are supposed to.
        if (!validateOnly) {
            m_hierarchieLevelCell = settings
                    .getString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL);

            m_sota.setUseHierarchicalFuzzyData(useHFD);
        }
    }
    
    
    
    
    /**
     * {@inheritDoc}
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
                    SotaPortObject.CFG_KEY_USE_FUZZY_HIERARCHY));
            m_sota.setMaxHierarchicalLevel(modelContent.getInt(
                    SotaPortObject.CFG_KEY_MAX_FUZZY_LEVEL));
            inDataSize = modelContent.getInt(
                    SotaPortObject.CFG_KEY_INDATA_SIZE);
            origDataSize = modelContent.getInt(
                    SotaPortObject.CFG_KEY_ORIGDATA_SIZE);
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
     * {@inheritDoc}
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
        modelContent.addBoolean(SotaPortObject.CFG_KEY_USE_FUZZY_HIERARCHY, 
                m_sota.isUseHierarchicalFuzzyData());
        modelContent.addInt(SotaPortObject.CFG_KEY_MAX_FUZZY_LEVEL, 
                m_sota.getMaxHierarchicalLevel());
        modelContent.addInt(SotaPortObject.CFG_KEY_INDATA_SIZE,
                m_sota.getInDataContainer().size());
        modelContent.addInt(SotaPortObject.CFG_KEY_ORIGDATA_SIZE,
                m_sota.getOriginalData().size());
        
        
        File file = new File(internDir, TREE_FILE);
        FileOutputStream fos = new FileOutputStream(file);
        modelContent.saveToXML(fos);
        fos.close();
    }
}
