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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange;

import static java.lang.System.identityHashCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.exec.SandboxedNodeCreator;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Static repository of {@link PortObject PortObjects}. It is used to virtually set output objects of port object
 * in node models.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class PortObjectRepository {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PortObjectRepository.class);

    private static final Map<UUID, PortObject> MAP = new HashMap<>();

    private static final Map<Integer, UUID> BEFORE_COPY_TO_ID_MAP = new HashMap<>();

    private PortObjectRepository() {
        // empty
    }

    /** Add new port object to repository.
     * @param object Object to be added
     * @return the unique id this object is associated with
     * @throws NullPointerException If argument is null.
     */
    public static synchronized UUID add(final PortObject object) {
        CheckUtils.checkArgumentNotNull(object);
        UUID id = UUID.randomUUID();
        add(id, object);
        return id;
    }

    /**
     * Adds a copy of the passed port object (by using the provided execution context).
     *
     * Note: if the port object is of type {@link FileStorePortObject} the
     * {@link NotInWorkflowWriteFileStoreHandler} will be used.
     *
     * @param po the port object to copy and add
     * @param exec the execution context for the copy
     * @return the new id
     * @throws IOException if the copy failed
     * @throws CanceledExecutionException if the copy process has been interrupted
     */
    public static synchronized UUID addCopy(final PortObject po, final ExecutionContext exec)
        throws IOException, CanceledExecutionException {
        UUID id = add(copy(po, exec, exec));
        BEFORE_COPY_TO_ID_MAP.put(identityHashCode(po), id);
        return id;
    }

    /**
     * Returns the id for a port object, if known. Identity (==) will be used to look up the port objects.
     *
     * Only will return an id for port objects that have been added via {@link #addCopy(PortObject, ExecutionContext)}.
     *
     * @param po the original port object to get the id for
     * @return the id or an empty optional of not found
     */
    public static synchronized Optional<UUID> getIDFor(final PortObject po) {
        return Optional.ofNullable(BEFORE_COPY_TO_ID_MAP.get(identityHashCode(po)));
    }

    /**
     * Removes the stored id for a port object. Only has an effect if the very same port object has been added via
     * {@link #addCopy(PortObject, ExecutionContext)} before.
     *
     * @param po the port object to remove the id for
     * @return the id or an empty optional if there is no mapped id
     */
    public static synchronized Optional<UUID> removeIDFor(final PortObject po) {
        return Optional.ofNullable(BEFORE_COPY_TO_ID_MAP.remove(identityHashCode(po)));
    }

    /** Add new port object to repository.
     * @param object Object to be added
     * @param id the id of the added port object
     * @throws NullPointerException If argument is null.
     */
    public static synchronized void add(final UUID id, final PortObject object) {
        CheckUtils.checkArgumentNotNull(object);
        MAP.put(id, object);
        LOGGER.debug("Added port object (" + object.getClass().getSimpleName()
                + ") to static repository, assigned ID " + id
                + " (total count " + MAP.size() + ")");
    }

    /** Remove the port object that is associated with the given id.
     * @param id The id of the object
     * @return The removed object or null if it was not contained.
     */
    public static synchronized PortObject remove(final UUID id) {
        PortObject object = MAP.remove(id);
        if (object != null) {
            LOGGER.debug("Removed port object with id " + id + " ("
                    + object.getClass().getSimpleName()
                    + ") from static repository (" + MAP.size()
                    + " remaining)");
        } else {
            LOGGER.debug("Failed to remove port object with id " + id
                    + " from repository, no such id");
        }
        return object;
    }

    /** Get the port object that is associated with the given id.
     * @param id The id of the object
     * @return The object or an empty optional if it is not contained.
     */
    public static synchronized Optional<PortObject> get(final UUID id) {
        return Optional.ofNullable(MAP.get(id));
    }

    /** Copies the argument object by means of the associated serializer.
     * @param object The port object to be copied.
     * @param exec Host for BDTs being created
     * @param progress For progress/cancelation
     * @return The deep copy.
     * @throws IOException In case of exceptions while accessing the streams
     * @throws CanceledExecutionException If canceled.
     */
    public static PortObject copy(final PortObject object, final ExecutionContext exec, final ExecutionMonitor progress)
        throws IOException, CanceledExecutionException {
        if (object instanceof BufferedDataTable in) {
            final var settings = DataContainerSettings.internalBuilder() //
                .withInitializedDomain(true) //
                .withMaxCellsInMemory(0) //
                .withForceCopyOfBlobs(true) //
                .build();

            final long rowCount = in.size();
            long row = 0;
            try (final var rowContainer = exec.createRowContainer(in.getSpec(), settings);
                    final var writeCursor = rowContainer.createCursor();
                    final var readCursor = in.cursor()) {
                while (readCursor.canForward()) {
                    writeCursor.commit(readCursor.forward());
                    final var finalRow = row;
                    progress.setProgress(row / (double)rowCount, () -> "Copied row " + finalRow + "/" + rowCount);
                    progress.checkCanceled();
                    row++;
                }
                return rowContainer.finish();
            }
        }
        return Node.copyPortObject(object, exec);
    }

    /**
     * Adds a "Port Object Reference Reader" node to the workflow, which will read the object passed in as argument.
     *
     * The added reference reader node references the data (i.e. port object) by referencing the port (original node id
     * plus port index).
     *
     * @param outport the outport to get the port object and flow variables from that the to be added node will provide,
     *            too (the port object is just referenced by the original node id and port index)
     * @param srcParentID the id of the workflow manager the referenced node (port) is part of
     * @param wfm the workflow manager the new node should be added to
     * @param nodeIDSuffix the id the to be added node will have (will be ignored if there is a node with the id
     *            already!)
     * @return the id of the newly added node
     */
    // TODO we might have to revisit this when implementing AP-13335
    public static NodeID addPortObjectReferenceReaderWithNodeReference(final NodeOutPort outport,
        final NodeID srcParentID, final WorkflowManager wfm, final int nodeIDSuffix) {
        NodeID sourceNodeID = outport.getConnectedNodeContainer().getID();
        int portIndex =
            outport.getConnectedOutport().orElseThrow(() -> new IllegalStateException("Node is set, but no port"));

        List<FlowVariable> variables = outport.getFlowObjectStack().getAllAvailableFlowVariables().values().stream()
            .filter(f -> f.getScope() == Scope.Flow).collect(Collectors.toList());
        PortObjectIDSettings portObjectIDSettings = new PortObjectIDSettings();
        portObjectIDSettings.setNodeReference(NodeIDSuffix.create(srcParentID, sourceNodeID), portIndex);
        portObjectIDSettings.setFlowVariables(variables);
        boolean isTable = outport.getPortType().equals(BufferedDataTable.TYPE);
        return addPortObjectReferenceReader(wfm, portObjectIDSettings, isTable, nodeIDSuffix);
    }

    /**
     * Adds a "Port Object Reference Reader" node to the workflow, which will read the object passed in as argument.
     *
     * The added reference reader references the data in the {@link PortObjectRepository} by a unique port object id.
     *
     * @param outport the outport to get the port object and flow variables from that the to be added node will provide,
     *            too (the port object is just referenced by the original node id and port index)
     * @param idInPortObjectRepo the port object id in the port object repository
     * @param wfm the workflow manager the new node should be added to
     * @param nodeIDSuffix the id the to be added node will have (will be ignored if there is a node with the id
     *            already!)
     * @return the id of the newly added node
     */
    public static NodeID addPortObjectReferenceReaderWithRepoReference(final NodeOutPort outport,
        final UUID idInPortObjectRepo, final WorkflowManager wfm, final int nodeIDSuffix) {
        List<FlowVariable> variables = outport.getFlowObjectStack().getAllAvailableFlowVariables().values().stream()
            .filter(f -> f.getScope() == Scope.Flow).collect(Collectors.toList());
        PortObjectIDSettings portObjectIDSettings = new PortObjectIDSettings();
        portObjectIDSettings.setId(idInPortObjectRepo);
        portObjectIDSettings.setFlowVariables(variables);
        boolean isTable = outport.getPortType().equals(BufferedDataTable.TYPE);
        return addPortObjectReferenceReader(wfm, portObjectIDSettings, isTable, nodeIDSuffix);
    }

    private static NodeID addPortObjectReferenceReader(final WorkflowManager wfm,
        final PortObjectIDSettings portObjectIDSettings, final boolean isTable, final int nodeIDSuffix) {
        NodeFactory<?> factory =
            isTable ? SandboxedNodeCreator.TABLE_READ_NODE_FACTORY : SandboxedNodeCreator.OBJECT_READ_NODE_FACTORY;
        NodeID inID;
        if (wfm.containsNodeContainer(wfm.getID().createChild(nodeIDSuffix))) {
            //create a new node id
            inID = wfm.addNodeAndApplyContext(factory, null, -1);
        } else {
            //re-use node id
            inID = wfm.addNodeAndApplyContext(factory, null, nodeIDSuffix);
        }
        NodeSettings s = new NodeSettings("temp_data_in");
        try {
            wfm.saveNodeSettings(inID, s);
            PortObjectInNodeModel.setInputNodeSettings(s, portObjectIDSettings);
            wfm.loadNodeSettings(inID, s);
        } catch (InvalidSettingsException ex) {
            //should never happen
            throw new IllegalStateException("Most likely an implementation error", ex);
        }

        return inID;
    }
}
