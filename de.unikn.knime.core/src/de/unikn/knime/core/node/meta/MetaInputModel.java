/* Created on Jun 23, 2006 12:36:10 PM by thor
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
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.PredictorParams;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class MetaInputModel extends NodeModel {
    /**
     * Creates a new input node for meta workflows.
     * 
     * @param nrDataIns the number of data input ports
     * @param nrDataOuts the number of data output ports
     * @param nrPredParamsIns the number of model input ports
     * @param nrPredParamsOuts the number of model output ports
     */
    public MetaInputModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
    }

    
    /**
     * Creates a new input node for meta workflows.
     * 
     * @param nrDataIns the number of data input ports
     * @param nrDataOuts the number of data output ports
     */
    public MetaInputModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }

    
    /**
     * Returns if this model can really be executed.
     * 
     * @return <code>true</code> if it can be executed, <code>false</code>
     * otherwise
     */
    public abstract boolean canBeExecuted();
    
    
    /**
     * Sets the datatable spec that the node should just pass on.
     *  
     * @param spec any datatable spec
     */
    public abstract void setDataTableSpec(final DataTableSpec spec);
    
    
    /**
     * Sets the datatable that the node should just pass on.
     *  
     * @param table any datatable
     */
    public abstract void setDataTable(final DataTable table);
    
    
    /**
     * Sets the predictor params that this model should just pass on.
     * 
     * @param predParams any predictor parameters
     */
    public abstract void setPredictorParams(final PredictorParams predParams);
}
