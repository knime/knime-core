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
 *   Feb 10, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * Persistor that is used when a workflow (a project) is loaded. It is used
 * to past a single (meta-) node into ROOT.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class InsertWorkflowPersistor implements WorkflowPersistor {

    private final WorkflowPersistor m_wfmPersistor;

    /**
     *
     */
    InsertWorkflowPersistor(final WorkflowPersistor wfmPersistor) {
        if (wfmPersistor == null) {
            throw new NullPointerException();
        }
        m_wfmPersistor = wfmPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        throw new IllegalStateException("no table repository for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        throw new IllegalStateException("no imports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getInPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoadVersionString() {
        return m_wfmPersistor.getLoadVersionString();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throw new IllegalStateException("can't set name on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowVariable> getWorkflowVariables() {
        throw new IllegalStateException("can't set workflow variables on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<Credentials> getCredentials() {
        throw new IllegalStateException("can't set credentials on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<WorkflowAnnotation> getWorkflowAnnotations() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<ReferencedFile> getObsoleteNodeDirectories() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return Collections.singletonMap(
                m_wfmPersistor.getMetaPersistor().getNodeIDSuffix(),
                (NodeContainerPersistor)m_wfmPersistor);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        throw new IllegalStateException("no outports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getOutPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return m_wfmPersistor.mustWarnOnDataLoadError();
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        throw new IllegalStateException("no meta persistor for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        throw new IllegalStateException("root has no parent, can't add node");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        throw new IllegalStateException("root has not meaningful state");
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final NodeSettingsRO parentSettings,
            final LoadResult loadResult)
            throws InvalidSettingsException, IOException {
        throw new IllegalStateException("root can't be loaded");
    }

}
