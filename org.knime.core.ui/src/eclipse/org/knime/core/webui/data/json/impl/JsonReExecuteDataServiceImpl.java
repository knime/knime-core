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
 *   Sep 14, 2021 (hornm): created
 */
package org.knime.core.webui.data.json.impl;

import java.io.IOException;

import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.ReExecutable;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.json.JsonApplyDataService;
import org.knime.core.webui.data.json.JsonReExecuteDataService;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the {@link JsonReExecuteDataService} which re-executes the associated node in order to 'apply' the
 * data.
 *
 * If {@link JsonApplyDataService#applyData(Object)} is called on this implementation, the node won't be re-executed but
 * the data just passed via {@link ReExecutable#preReExecute(Object, boolean)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <D> the type of the data object to apply
 * @param <T> the node model of the node to re-execute
 *
 * @since 4.5
 */
public class JsonReExecuteDataServiceImpl<D, T extends NodeModel & ReExecutable<D>>
    implements JsonReExecuteDataService<D> {

    private final T m_reExecutableNodeModel;

    private final ObjectMapper m_mapper;

    private final Class<D> m_dataType;

    private final WorkflowManager m_wfm;

    private final NodeID m_nodeId;

    /**
     * @param reExecutableNodeModel the model of the node to re-execute
     * @param dataType the type of the data object to apply
     */
    public JsonReExecuteDataServiceImpl(final T reExecutableNodeModel, final Class<D> dataType) {
        this(reExecutableNodeModel, dataType, ObjectMapperUtil.getInstance().getObjectMapper());
    }

    /**
     * @param reExecutableNodeModel the model of the node to re-execute
     * @param dataType the type of the data object to apply
     * @param mapper a custom object mapper for data deserialization
     */
    public JsonReExecuteDataServiceImpl(final T reExecutableNodeModel, final Class<D> dataType,
        final ObjectMapper mapper) {
        m_reExecutableNodeModel = reExecutableNodeModel;
        m_dataType = dataType;
        m_mapper = mapper;
        NodeContainer nc = NodeContext.getContext().getNodeContainer();
        m_wfm = nc.getParent();
        m_nodeId = nc.getID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyData(final D data) {
        m_reExecutableNodeModel.preReExecute(data, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reExecute(final D data) {
        m_wfm.reExecuteNode(m_nodeId, data, false);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public D fromJson(final String data) throws IOException {
        if (String.class.isAssignableFrom(m_dataType)) {
            return (D)data;
        }
        try {
            return m_mapper.readValue(data, m_dataType);
        } catch (JsonProcessingException ex) {
            throw new IOException(ex);
        }
    }

}
