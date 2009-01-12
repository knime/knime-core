/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Jan 8, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeContentPersistor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class NodeExecutionResult implements NodeContentPersistor {
    
    private BufferedDataTable[] m_internalHeldTables;
    private ReferencedFile m_nodeInternDir;
    private PortObject[] m_portObjects;
    private PortObjectSpec[] m_portObjectSpecs;
    private String m_warningMessage;
    private boolean m_needsResetAfterLoad;

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalHeldTables() {
        return m_internalHeldTables;
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeInternDirectory() {
        return m_nodeInternDir;
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject(final int outportIndex) {
        return m_portObjects[outportIndex];
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        return m_portObjectSpecs[outportIndex];
    }

    /** {@inheritDoc} */
    @Override
    public String getPortObjectSummary(final int outportIndex) {
        return m_portObjects[outportIndex].getSummary();
    }

    /** {@inheritDoc} */
    @Override
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }
    
    /**
     * @param internalHeldTables the internalHeldTables to set
     */
    public void setInternalHeldTables(
            final BufferedDataTable[] internalHeldTables) {
        m_internalHeldTables = internalHeldTables;
    }
    
    /**
     * @param nodeInternDir the referencedFile to set
     */
    public void setNodeInternDir(final ReferencedFile nodeInternDir) {
        m_nodeInternDir = nodeInternDir;
    }

    /**
     * @param warningMessage the warningMessage to set
     */
    public void setWarningMessage(final String warningMessage) {
        m_warningMessage = warningMessage;
    }

    /**
     * @param portObjects the portObjects to set
     */
    public void setPortObjects(final PortObject[] portObjects) {
        m_portObjects = portObjects;
    }

    /**
     * @param portObjectSpecs the portObjectSpecs to set
     */
    public void setPortObjectSpecs(final PortObjectSpec[] portObjectSpecs) {
        m_portObjectSpecs = portObjectSpecs;
    }

}
