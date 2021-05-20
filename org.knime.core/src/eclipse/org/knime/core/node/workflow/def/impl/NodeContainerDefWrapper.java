/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow.def.impl;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeMessageDef;
import org.knime.core.workflow.def.NodeUIInfoDef;

/**
 *
 * @author hornm
 */
public class NodeContainerDefWrapper implements NodeDef {

    private NodeContainer m_nc;

    public NodeContainerDefWrapper(final NodeContainer nc) {
        m_nc = nc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "NodeDef";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomDescription() {
        return m_nc.getCustomDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeAnnotationDef getNodeAnnotation() {
        return CoreToDefUtil.toNodeAnnotationDef(m_nc.getNodeAnnotation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInfoDef getNodeUIInfo() {
        return CoreToDefUtil.toNodeUIInfoDef(m_nc.getUIInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeLocksDef getNodeLocks() {
        return CoreToDefUtil.toNodeLocksDef(m_nc.getNodeLocks());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return m_nc.getNodeContainerState().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeMessageDef getNodeMessage() {
        return CoreToDefUtil.toNodeMessageDef(m_nc.getNodeMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigMapDef getNodeExecutionJobManagerSettings() {
        NodeSettings ns = new NodeSettings("jobmanager");
        m_nc.getJobManager().save(ns);
        try {
            return CoreToDefUtil.toConfigMapDef(ns);
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

}
