/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Aug 13, 2009 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * A persistor cloning a node's settings. It does not retain port objects or
 * node internals. Used by copy&paste and undo.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CopyNodePersistor implements NodePersistor {
    
    private final NodeSettingsRO m_settings;
    
    /** Create a new persistor.
     * @param original The node to copy.
     */
    CopyNodePersistor(final Node original) {
        NodeSettings settings = new NodeSettings("copy");
        original.saveSettingsTo(settings);
        m_settings = settings;
    }
    
    /** Apply the settings to the new node.
     * @param node the node just created.
     */
    public void loadInto(final Node node) {
        try {
            node.load(this, new ExecutionMonitor(), new LoadResult("ignored"));
        } catch (CanceledExecutionException e) {
            // ignored, can't happen
        }
    }

    /** {@inheritDoc} */
    @Override
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        return LoadNodeModelSettingsFailPolicy.IGNORE;
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getSettings() {
        return m_settings;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConfigured() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecuted() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setDirtyAfterLoad() {
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalHeldTables() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeInternDirectory() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject(final int outportIndex) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPortObjectSummary(final int outportIndex) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getWarningMessage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasContent() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void setNeedsResetAfterLoad() {
    }

}
