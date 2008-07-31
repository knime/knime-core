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
 * Node view base class which implements the basic and common window properties.
 * The part specific to the special purpose node view must be implemented in the
 * derived class and must take place in a <code>Panel</code>. This panel is
 * registered in this base class (method <code>#setComponent(Component)</code>)
 * and will be displayed in the JFrame provided and handled by this class.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeView<T extends NodeModel> extends GenericNodeView<GenericNodeModel> {
    
    /**
     * 
     */
    public NodeView(final T nodeModel) {
        super(nodeModel);
    }
    
    @Override
    protected T getNodeModel() {
        return (T)super.getNodeModel();
    }
    
}
