/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.node.tableinput;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.ModelContentRO;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * This class lets you fetch the predictor params that are passed from the only
 * one sucessor node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ModelOutputNodeModel extends NodeModel {
    private ModelContentRO m_predictorParams;
    
    /**
     * Creates a new model output node model.
     */
    public ModelOutputNodeModel() {
        super(0, 0, 1, 0);
    }


    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #execute(BufferedDataTable[],
     *  ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        return inData;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_predictorParams = null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #configure(de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
    }
    

   /**
    * @see de.unikn.knime.core.node.NodeModel
    *  #loadModelContent(int, ModelContentRO)
    */
   @Override
   protected void loadModelContent(final int index,
           final ModelContentRO predParams) throws InvalidSettingsException {
       m_predictorParams = predParams;
   }

   
   /**
    * Returns the predictor params from the sucessor node.
    * 
    * @return the predictor params
    */
   public ModelContentRO getModelContent() {
       return m_predictorParams;
   }
   
   /**
    * @see de.unikn.knime.core.node.
    *      NodeModel#loadInternals(File, ExecutionMonitor)
    */
   @Override
   protected void loadInternals(final File nodeInternDir, 
           final ExecutionMonitor exec) 
           throws IOException, CanceledExecutionException {
       
   }

   /**
    * @see de.unikn.knime.core.node.
    *      NodeModel#saveInternals(File, ExecutionMonitor)
    */
   @Override
   protected void saveInternals(final File nodeInternDir, 
           final ExecutionMonitor exec) 
           throws IOException, CanceledExecutionException {
       
   }
   
}
