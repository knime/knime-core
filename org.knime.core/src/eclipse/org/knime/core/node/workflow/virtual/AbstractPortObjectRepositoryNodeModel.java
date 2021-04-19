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
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;

/**
 * A node which holds a set of port objects and makes them available to down-stream nodes by adding them to the
 * {@link PortObjectRepository}.
 *
 * One use case is for nodes that control the execution of a virtual workflow. I.e. a workflow that is created within
 * this workflow and deleted after it's successful execution. As a result, the {@link FlowVirtualScopeContext}
 * associated with a virtual workflow adds selected port objects to this node model (via
 * {@link #addPortObject(UUID, PortObject)}) such that they are still available even after the deletion of the virtual
 * workflow.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public abstract class AbstractPortObjectRepositoryNodeModel extends NodeModel implements PortObjectHolder {

    private static final String CFG_PORT_OBJECT_IDS = "port_object_ids";

    private static final String INTERNALS_FILE_PORT_OBJECT_IDS = "port_object_ids.xml.gz";

    private final List<UUID> m_portObjectIds = new ArrayList<>();

    private final List<PortObject> m_portObjects = new ArrayList<>();

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected AbstractPortObjectRepositoryNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected AbstractPortObjectRepositoryNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
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
