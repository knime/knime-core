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
 *   30.03.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.predictor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.base.node.mine.sota.SotaNodeModel;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPredictorNodeModel extends NodeModel {
    
    private SettingsModelFilterString m_cols = new SettingsModelFilterString(
                SotaPredictorConfigKeys.CFG_KEY_FILTERED_COLS);
    
    private SotaTreeCell m_sotaRoot;
    
    private String m_distance;
    
    
    /**
     * Creates new instance of <code>SotaPredictorNodeModel</code>.
     */
    public SotaPredictorNodeModel() {
        super(1, 1, 1, 0);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createDataTableSpec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        exec.checkCanceled();
        
        // build data table to use
        List<String> includedCols = m_cols.getIncludeList();
        int[] indicesOfIncludedCols = new int[includedCols.size()];
        
        int arrayIndex = 0;
        for (int i = 0; i < inData[0].getDataTableSpec().getNumColumns(); i++) {
            String colName = 
                inData[0].getDataTableSpec().getColumnSpec(i).getName();
            if (includedCols.contains(colName)) {
                indicesOfIncludedCols[arrayIndex] = i;
                arrayIndex++;
            }
        }
        
        exec.checkCanceled();

        ColumnRearranger cr = new ColumnRearranger(
                inData[0].getDataTableSpec());
        cr.append(new SotaPredictorCellFactory(
                m_sotaRoot, indicesOfIncludedCols, m_distance));
        
        
        return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(inData[0], cr, exec)};
    }

    
    /**
     * Creates the outgoing <code>DataTableSpec</code> by adding a column
     * to the incoming <code>DataTableSpec</code> which contains the 
     * predicted class.
     * 
     * @param incomingSpec The incoming <code>DataTableSpec</code>. 
     * @return the outgoing <code>DataTableSpec</code>.
     */
    public static DataTableSpec createDataTableSpec(
            final DataTableSpec incomingSpec) {
        DataColumnSpecCreator creator =
            new DataColumnSpecCreator("Predicted class", StringCell.TYPE);
        return new DataTableSpec(incomingSpec, 
                new DataTableSpec(creator.createSpec()));
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert predParams == predParams;
        
        if (predParams != null) {
            // Load tree
            m_sotaRoot = new SotaTreeCell(0, false);
            try {
                m_sotaRoot.loadFrom(predParams, 0, null, false);
            } catch (InvalidSettingsException e) {
                InvalidSettingsException ioe = new InvalidSettingsException(
                        "Could not load tree cells, due to invalid settings in "
                        + "model content !");
                ioe.initCause(e);
                throw ioe;
            }
            
            m_distance = predParams.getString(SotaNodeModel.CFG_KEY_DIST);
        }
    }  


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_sotaRoot = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_cols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_cols.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_cols.loadSettingsFrom(settings);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
