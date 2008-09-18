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

import org.knime.base.node.mine.sota.SotaPortObject;
import org.knime.base.node.mine.sota.SotaPortObjectSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPredictorNodeModel extends GenericNodeModel {
    
    private SettingsModelFilterString m_cols = new SettingsModelFilterString(
                SotaPredictorConfigKeys.CFG_KEY_FILTERED_COLS);
    
    
    /**
     * Creates new instance of <code>SotaPredictorNodeModel</code>.
     */
    public SotaPredictorNodeModel() {
        super(new PortType[]{new PortType(SotaPortObject.class),
                BufferedDataTable.TYPE}, 
                new PortType[]{BufferedDataTable.TYPE});
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        
        if (!(inSpecs[0] instanceof SotaPortObjectSpec)) {
            throw new InvalidSettingsException(
                    "Given spec at index 1 is not a SotaPortObjectSpec!");
        }
        if (!(inSpecs[1] instanceof DataTableSpec)) {
            throw new InvalidSettingsException(
                    "Given spec at index 0 is not a DataTableSpec!");
        }
                
        SotaPortObjectSpec sp = (SotaPortObjectSpec)inSpecs[0];
        
        if (!sp.hasClassColumn()) {
            setWarningMessage("Given model is trained on data without a " 
                    + "class column, which makes prediction pretty odd. "
                    + "Predicted class is set to \"NoClassDefined\".");
        }
        
        if (!sp.validateSpec((DataTableSpec)inSpecs[1])) {
            throw new InvalidSettingsException("Input data is not compatible " 
                    + "with given sota model!");
        }
        
        return new PortObjectSpec[]{createDataTableSpec(
                (DataTableSpec)inSpecs[1])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        
        if (!(inData[1] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Given inport object at " 
                    + "index 0 is not a BufferedDataTable!");
        } else if (!(inData[0] instanceof SotaPortObject)) {
            throw new IllegalArgumentException("Given inport object at " 
                    + "index 1 is not a SotaPortObject");
        }
        
        BufferedDataTable bdt = (BufferedDataTable)inData[1];
        SotaPortObject spo = (SotaPortObject)inData[0];
        
        exec.checkCanceled();
        
        // build data table to use
        List<String> includedCols = m_cols.getIncludeList();
        int[] indicesOfIncludedCols = new int[includedCols.size()];
        
        int arrayIndex = 0;
        for (int i = 0; i < bdt.getDataTableSpec().getNumColumns(); i++) {
            String colName = 
                bdt.getDataTableSpec().getColumnSpec(i).getName();
            if (includedCols.contains(colName)) {
                indicesOfIncludedCols[arrayIndex] = i;
                arrayIndex++;
            }
        }
        
        exec.checkCanceled();

        ColumnRearranger cr = new ColumnRearranger(bdt.getDataTableSpec());
        cr.append(new SotaPredictorCellFactory(spo.getSotaRoot(), 
                indicesOfIncludedCols, spo.getDistance()));
        
        
        return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(bdt, cr, exec)};
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
    protected void reset() {
        
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
