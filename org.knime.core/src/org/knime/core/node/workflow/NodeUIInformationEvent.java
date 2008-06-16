/*
 * ------------------------------------------------------------------ *
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
 * -------------------------------------------------------------------
 *
 * History
 *   20.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.workflow;

import java.util.EventObject;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class NodeUIInformationEvent extends EventObject {

    private final UIInformation m_uiInformation;

    private final String m_customName;

    private final String m_description;

    /**
     * @param src the node id of the source node
     * @param uiInformation the new UI information
     */
    public NodeUIInformationEvent(final NodeID src,
            final UIInformation uiInformation, final String customName,
            final String description) {
        super(src);
        m_uiInformation = uiInformation;
        m_customName = customName;
        m_description = description;
    }

    /**
     *
     * @return the new UI information
     */
    public UIInformation getUIInformation() {
        return m_uiInformation;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public NodeID getSource() {
        return (NodeID)super.getSource();
    }

    public String getCustomName() {
        return m_customName;
    }

    public String getDescription() {
        return m_description;
    }

}
