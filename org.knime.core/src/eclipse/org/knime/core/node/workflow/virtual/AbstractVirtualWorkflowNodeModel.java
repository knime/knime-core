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
 *   Dec 8, 2020 (hornm): created
 */
package org.knime.core.node.workflow.virtual;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowCaptureOperation;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectOutNodeModel;

/**
 * A node which controls the virtual execution of another workflow. The virtual workflow is created within the workflow
 * this node is part of and deleted after its successful execution.
 *
 * With the disposal of the 'virtual' workflow, all port objects created in that workflow (except those that are passed
 * to the virtual output node, {@link VirtualParallelizedChunkPortObjectOutNodeModel}), are disposed, too and not
 * accessible anymore after the finished execution. This, however, is not always desired (see the example use case
 * description below) and there is a mechanism that allows one to keep and store selected port objects with this node
 * model: Right before the virtual workflow is executed,
 * {@link FlowVirtualScopeContext#registerHostNodeForPortObjectPersistence(org.knime.core.node.workflow.NativeNodeContainer, org.knime.core.node.workflow.NativeNodeContainer, org.knime.core.node.ExecutionContext)}
 * needs to be called with this node as parameter. As a result, some port objects (which port objects exactly is not
 * controlled by this node model) are automatically added to the list of port objects (via
 * {@link #addPortObject(UUID, PortObject)}) and subsequently managed (i.e. saved and loaded) by this node model. The
 * port objects are made available to other downstream nodes by registering them at the {@link PortObjectRepository}.
 *
 * An example use case is Integrated Deployment and the Workflow Executor (or the Parallel Chunk Loop): In case the
 * Workflow Executor is supposed to execute a workflow that in turn captures another workflow (i.e. uses the 'Capture
 * Workflow Start' and 'Capture Workflow End' nodes). If this to be captured workflow now has a 'static' input directly
 * connected into the scope, they are usually referenced from the captured workflow segment by their node-id and port
 * index. This, however, is not possible here since the referenced node won't exist anymore after the successful
 * execution of the virtual workflow. Thus, the capture-logic (see {@link WorkflowCaptureOperation}) makes sure that, if
 * a workflow is captured within a virtual (i.e. temporary) workflow, the port objects, which are referenced from the
 * captured workflow segment, are registered in the {@link PortObjectRepository} and passed to the host node of the
 * virtual workflow (i.e. this node model) for persistence. By that, those 'static' inputs are still available to
 * downstream nodes operating on the (in a virtual workflow) captured workflow segment, such as the Workflow Executor
 * or Workflow Writer nodes.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public abstract class AbstractVirtualWorkflowNodeModel extends NodeModel implements PortObjectHolder {

    private static final String CFG_PORT_OBJECT_IDS = "port_object_ids";

    private static final String INTERNALS_FILE_PORT_OBJECT_IDS = "port_object_ids.xml.gz";

    private final List<UUID> m_portObjectIds = new ArrayList<>();

    private final List<PortObject> m_portObjects = new ArrayList<>();

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected AbstractVirtualWorkflowNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected AbstractVirtualWorkflowNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    @Override
    protected void reset() {
        if (!m_portObjectIds.isEmpty()) {
            m_portObjectIds.forEach(PortObjectRepository::remove);
            m_portObjects.forEach(PortObjectRepository::removeIDFor);
            m_portObjectIds.clear();
            m_portObjects.clear();
        }
    }

    /**
     * Adds a port object id this node shall persist and provide on load (via {@link PortObjectRepository}).
     *
     * @param uuid the uuid to add
     * @param po the port object to add
     */
    public synchronized void addPortObject(final UUID uuid, final PortObject po) {
        m_portObjectIds.add(uuid);
        m_portObjects.add(po);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, INTERNALS_FILE_PORT_OBJECT_IDS);
        if (f.exists()) {
            try (InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                NodeSettingsRO settings = NodeSettings.loadFromXML(in);
                if (settings.containsKey(CFG_PORT_OBJECT_IDS)) {
                    m_portObjectIds.clear();
                    m_portObjects.clear();
                    addPortObjectIds(settings.getStringArray(CFG_PORT_OBJECT_IDS));
                    addToPortObjectRepository();
                }
            } catch (InvalidSettingsException ise) {
                throw new IOException("Unable to read port object ids", ise);
            }
        }
    }

    private void addPortObjectIds(final String... ids) {
        for (String id : ids) {
            m_portObjectIds.add(UUID.fromString(id));
        }
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (!m_portObjectIds.isEmpty()) {
            NodeSettings settings = new NodeSettings(CFG_PORT_OBJECT_IDS);
            String[] ids =
                m_portObjectIds.stream().map(UUID::toString).toArray(i -> new String[m_portObjectIds.size()]);
            settings.addStringArray(CFG_PORT_OBJECT_IDS, ids);
            try (GZIPOutputStream gzs = new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(new File(nodeInternDir, INTERNALS_FILE_PORT_OBJECT_IDS))))) {
                settings.saveToXML(gzs);
            }
        }
    }

    @Override
    public void setInternalPortObjects(final PortObject[] portObjects) {
        m_portObjects.clear();
        Collections.addAll(m_portObjects, portObjects);
        addToPortObjectRepository();
    }

    private void addToPortObjectRepository() {
        if (!m_portObjects.isEmpty() && !m_portObjectIds.isEmpty()) {
            assert m_portObjects.size() == m_portObjectIds.size();
            for (int i = 0; i < m_portObjects.size(); i++) {
                PortObjectRepository.add(m_portObjectIds.get(i), m_portObjects.get(i));
            }
        }
    }

    @Override
    public PortObject[] getInternalPortObjects() {
        return m_portObjects.toArray(new PortObject[m_portObjects.size()]);
    }

}
