/* Created on May 29, 2006 3:12:46 PM by thor
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

/**
 * This model is a subclass of
 * {@link de.unikn.knime.core.node.meta.PassThroughNodeModel} that adds no
 * special behaviour but is just used to "tag" the model as an output node
 * model.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class MetaOutputNodeModel extends PassThroughNodeModel {
    /**
     * Creates a new node model that simply passes the input data to the
     * output ports.
     * 
     * @param dataOuts the number of data ports
     * @param modelOuts the number od model ports
     */
    public MetaOutputNodeModel(final int dataOuts, final int modelOuts) {
        super(dataOuts, modelOuts);
    }
}
