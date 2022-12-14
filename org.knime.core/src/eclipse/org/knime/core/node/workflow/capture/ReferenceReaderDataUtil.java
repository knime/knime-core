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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings.ReferenceType;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.AbstractPortObjectRepositoryNodeModel;

/**
 * Utility methods mainly to transfer data of so called 'PortObject- or BufferedDataTable- Reference Reader'-nodes (see,
 * e.g., {@link PortObjectInNodeModel})
 *
 * @since 4.6
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ReferenceReaderDataUtil {

    private ReferenceReaderDataUtil() {
        // utility
    }

    /**
     * Writes the 'reference reader node'-data of a workflow to file.
     *
     *
     * @param wfm a {@link WorkflowManager}
     * @param portObjectReaderSufIds a set of the referenced reader node identifiers
     * @param tmpDataDir the data directory
     * @param exec a {@link ExecutionMonitor}
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws URISyntaxException
     * @throws InvalidSettingsException
     */
    public static void writeReferenceReaderData(final WorkflowManager wfm, final Set<NodeIDSuffix> portObjectReaderSufIds,
        final File tmpDataDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, URISyntaxException, InvalidSettingsException {
        // reconfigure reference reader nodes and store their data in temp directory
        exec.setMessage(() -> "Introducing reference reader nodes.");
        for (NodeIDSuffix portObjectReaderSufId : portObjectReaderSufIds) {
            final NodeID portObjectReaderId = portObjectReaderSufId.prependParent(wfm.getID());
            final var portObjectReaderNC = wfm.findNodeContainer(portObjectReaderId);
            assert portObjectReaderNC instanceof NativeNodeContainer;
            final var portObjectReaderNM = ((NativeNodeContainer)portObjectReaderNC).getNodeModel();
            assert portObjectReaderNM instanceof PortObjectInNodeModel;
            final PortObjectInNodeModel portObjectReader = (PortObjectInNodeModel)portObjectReaderNM;
            final Optional<PortObject> poOpt = portObjectReader.getPortObject();
            assert poOpt.isPresent();
            PortObject po = poOpt.get();

            if (po instanceof WorkflowPortObject) {
                // also write the data for potential reference reader nodes within a referenced workflow segment
                // AP-16062
                po = writeReferenceReaderDataForWorkflowPort((WorkflowPortObject)po, tmpDataDir, exec);
            }

            final var poFileName =
                portObjectReaderSufId.toString().replace(":", "_") + "_" + System.identityHashCode(po);
            final var poFileRelativeURI = new URI("knime://knime.workflow/data/" + poFileName);
            final var tmpPoFile = new File(tmpDataDir, poFileName);
            final PortObjectIDSettings poSettings = portObjectReader.getInputNodeSettingsCopy();
            if (po instanceof BufferedDataTable) {
                final BufferedDataTable table = (BufferedDataTable)po;
                DataContainer.writeToZip(table, tmpPoFile, exec.createSubProgress(.2 / portObjectReaderSufIds.size()));
                poSettings.setFileReference(poFileRelativeURI, true);
            } else {
                PortUtil.writeObjectToFile(po, tmpPoFile, exec.createSubProgress(.2 / portObjectReaderSufIds.size()));
                poSettings.setFileReference(poFileRelativeURI, false);
            }

            final var settings = new NodeSettings("root");
            portObjectReaderNC.getParent().saveNodeSettings(portObjectReaderId, settings);
            final NodeSettingsWO modelSettings = settings.addNodeSettings("model");
            poSettings.saveSettings(modelSettings);
            portObjectReaderNC.getParent().loadNodeSettings(portObjectReaderId, settings);
        }
    }

    private static WorkflowPortObject writeReferenceReaderDataForWorkflowPort(final WorkflowPortObject wpo,
        final File dataDir, final ExecutionMonitor exec) throws IOException {
        return wpo.transformAndCopy(wfm -> {
            try {
                writeReferenceReaderData(wfm, wpo.getSpec().getWorkflowSegment().getPortObjectReferenceReaderNodes(),
                    dataDir, exec);
            } catch (IOException | CanceledExecutionException | URISyntaxException | InvalidSettingsException ex) {
                ExceptionUtils.rethrow(ex);
            }
        });
    }

    /**
     * Copies the 'reference reader node'-data to the {@link PortObjectRepository}, returns the corresponding nodes id.
     *
     * Note: this operation potentially manipulates the passed workflow manager (changing the port object reference
     * type, e.g. from 'file' to 'repository')
     *
     * @param wfm <T> The specific PortObject class of interest.
     * @param exec a {@link ExecutionContext}
     * @param portObjRepoNodeModel special node model that receives the port objects and makes them available through
     *            the {@link PortObjectRepository}
     * @return a set of {@link NodeIDSuffix}.
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    public static Set<NodeIDSuffix> copyReferenceReaderData(final WorkflowManager wfm, final ExecutionContext exec,
        final AbstractPortObjectRepositoryNodeModel portObjRepoNodeModel)
        throws InvalidSettingsException, CanceledExecutionException, IOException {
        var nodeIDSuffixes = new HashSet<NodeIDSuffix>();
        copyReferenceReaderDataRecursive(wfm, exec, portObjRepoNodeModel, nodeIDSuffixes);
        return nodeIDSuffixes;
    }

    private static void copyReferenceReaderDataRecursive(final WorkflowManager wfm, final ExecutionContext exec,
        final AbstractPortObjectRepositoryNodeModel portObjRepoNodeModel, final Set<NodeIDSuffix> nodeIDSuffixes)
        throws InvalidSettingsException, CanceledExecutionException, IOException {
        for (var nc : wfm.getNodeContainers()) {
            if (nc instanceof NativeNodeContainer
                && ((NativeNodeContainer)nc).getNodeModel() instanceof PortObjectInNodeModel) {
                exec.setProgress("Copying data for node " + nc.getID());
                PortObjectInNodeModel portObjectReader =
                    (PortObjectInNodeModel)((NativeNodeContainer)nc).getNodeModel();
                final PortObjectIDSettings poSettings = portObjectReader.getInputNodeSettingsCopy();
                if (poSettings.getReferenceType() != ReferenceType.FILE) {
                    throw new IllegalStateException(
                        "Reference reader nodes expected to reference a file. But the reference type is "
                            + poSettings.getReferenceType());
                }
                var uri = poSettings.getUri();
                var wfFile = wfm.getProjectWFM().getNodeContainerDirectory().getFile();
                var absoluteDataFile = new File(wfFile, uri.toString().replace("knime://knime.workflow", ""));
                if (!absoluteDataFile.getCanonicalPath().startsWith(wfFile.getCanonicalPath())) {
                    throw new IllegalStateException(
                        "Trying to read in a data file outside of the workflow directory. Not allowed!");
                }
                var po = readPortObjectFromFile(absoluteDataFile, exec, poSettings.isTable());
                var uuid = UUID.randomUUID();
                portObjRepoNodeModel.addPortObject(uuid, po);
                PortObjectRepository.add(uuid, po);
                updatePortObjectReferenceReaderReference(wfm, nc.getID(), poSettings, uuid);
                nodeIDSuffixes.add(NodeIDSuffix.create(wfm.getProjectWFM().getID(), nc.getID()));
            } else if (nc instanceof WorkflowManager) {
                copyReferenceReaderDataRecursive((WorkflowManager)nc, exec, portObjRepoNodeModel, nodeIDSuffixes);
            }
        }
    }

    private static PortObject readPortObjectFromFile(final File absoluteDataFile, final ExecutionContext exec,
        final boolean isTable) throws CanceledExecutionException, IOException {
        try (InputStream in = absoluteDataFile.toURI().toURL().openStream()) {
            return readPortObject(exec, in, isTable);
        }
    }

    private static void updatePortObjectReferenceReaderReference(final WorkflowManager wfm, final NodeID nodeId,
        final PortObjectIDSettings poSettings, final UUID id) throws InvalidSettingsException {
        poSettings.setId(id);
        final var settings = new NodeSettings("root");
        wfm.saveNodeSettings(nodeId, settings);
        final NodeSettingsWO modelSettings = settings.addNodeSettings("model");
        poSettings.saveSettings(modelSettings);
        wfm.loadNodeSettings(nodeId, settings);
    }

    private static PortObject readPortObject(final ExecutionContext exec, final InputStream in, final boolean isTable)
        throws CanceledExecutionException, IOException {
        PortObject po;
        if (isTable) {
            try (ContainerTable table = DataContainer.readFromStream(in)) {
                po = exec.createBufferedDataTable(table, exec);
            }
        } else {
            po = PortUtil.readObjectFromStream(in, exec);
        }
        return po;
    }
}
