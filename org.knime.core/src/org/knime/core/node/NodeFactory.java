/* 
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;


/**
 * Interface for factories summarizing <code>NodeModel</code>,
 * <code>NodeView</code>, and <code>NodeDialogPane</code> for a specific
 * <code>Node</code> implementation.
 * 
 * @author Michael Berthold, University of Konstanz
 * @param <T> The specific NodeModel implementation, which is generated
 */
public abstract class NodeFactory<T extends NodeModel>
        extends GenericNodeFactory<GenericNodeModel> {
    
    /**
     * @see GenericNodeFactory#GenericNodeFactory()
     */
    protected NodeFactory() {
        super();
    }

    @Override
    public final GenericNodeView<GenericNodeModel> createNodeView(int viewIndex,
            GenericNodeModel nodeModel) {
        return createNodeView(viewIndex, (T)nodeModel);
    }
    
    public abstract NodeView<T> createNodeView(final int index, final T model);
    
    @Override
    public abstract T createNodeModel();
    
    @Override
    protected abstract NodeDialogPane createNodeDialogPane();
    
}
