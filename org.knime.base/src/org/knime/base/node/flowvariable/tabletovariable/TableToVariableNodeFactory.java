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
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tabletovariable;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.port.PortType;


/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class TableToVariableNodeFactory 
    extends GenericNodeFactory<TableToVariableNodeModel> {
    
    private final PortType m_inOutType;
    
    /** Creates factory for {@link BufferedDataTable} type ports. */
    public TableToVariableNodeFactory() {
        this(BufferedDataTable.TYPE);
    }
    
    /** Create factory, that instantiates nodes, whose first input and the
     * only output port are typed to the argument.
     * @param inOutType The type of the (passed through) port.
     */
    public TableToVariableNodeFactory(final PortType inOutType) {
        if (inOutType == null) {
            throw new NullPointerException("PortType must not be null");
        }
        m_inOutType = inOutType;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public TableToVariableNodeModel createNodeModel() {
        return new TableToVariableNodeModel(m_inOutType);
    }

    /** {@inheritDoc} */
    @Override
    public GenericNodeView<TableToVariableNodeModel> createNodeView(
            final int index, final TableToVariableNodeModel model) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return false;
    }

}
