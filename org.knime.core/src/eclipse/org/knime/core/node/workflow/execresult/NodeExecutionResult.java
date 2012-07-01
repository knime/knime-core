/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 8, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.data.filestore.internal.FileStoreHandler;
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

    /** {@inheritDoc} */
    @Override
    public boolean hasContent() {
        return m_nodeInternDir != null;
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

    /** {@inheritDoc}
     * @since 2.6*/
    @Override
    public FileStoreHandler getFileStoreHandler() {
        return null;
    }

}
