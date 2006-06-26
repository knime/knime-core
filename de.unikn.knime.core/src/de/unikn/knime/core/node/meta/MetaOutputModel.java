/* Created on Jun 23, 2006 1:18:24 PM by thor
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
public abstract class MetaOutputModel extends NodeModel {
    /**
     * @param nrDataIns
     * @param nrDataOuts
     * @param nrPredParamsIns
     * @param nrPredParamsOuts
     */
    public MetaOutputModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
    }

    /**
     * @param nrDataIns
     * @param nrDataOuts
     */
    public MetaOutputModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }
    
    public abstract PredictorParams getPredictorParams();
    
    public abstract DataTable getDataTable();
    
    public abstract DataTableSpec getDataTableSpec();
}
