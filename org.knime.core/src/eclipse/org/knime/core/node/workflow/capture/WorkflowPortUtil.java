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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Pending implementation!
 *
 * @since 4.6
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class WorkflowPortUtil {

    private WorkflowPortUtil() {
        // utility
    }

    /**
     * @param po
     * @param workflowName
     * @param exec
     * @return
     * @throws Exception
     */
    public static File writeWorkflowPortObject(final WorkflowPortObject po, final String workflowName,
        final ExecutionMonitor exec) throws Exception {

        var segment = po.getSpec().getWorkflowSegment();
        var poCopy = po.transformAndCopy((wfm, dir) -> {
            var dataDir = new File(dir, "data");
            dataDir.mkdir();
            wfm.setName(workflowName);
            try {
                writeReferenceReaderNodeData(segment, wfm, dataDir, exec);
            } catch (IOException | CanceledExecutionException | URISyntaxException | InvalidSettingsException ex) {
                ExceptionUtils.rethrow(ex);
            }
        });
        final var tmpFile = FileUtil.createTempFile("workflow-port-object", ".portobject", true);
        PortUtil.writeObjectToFile(poCopy, tmpFile, exec);
        return tmpFile;
    }

    /**
     * @param segment
     * @param wfm
     * @param tmpDataDir
     * @param exec
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws URISyntaxException
     * @throws InvalidSettingsException
     */
    private static void writeReferenceReaderNodeData(final WorkflowSegment segment, final WorkflowManager wfm,
        final File tmpDataDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, URISyntaxException, InvalidSettingsException {
        // reconfigure reference reader nodes and store their data in temp directory
        exec.setMessage(() -> "Introducing reference reader nodes.");
        final Set<NodeIDSuffix> portObjectReaderSufIds = segment.getPortObjectReferenceReaderNodes();
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

    private static PortObject writeReferenceReaderDataForWorkflowPort(final WorkflowPortObject wpo, final File dataDir,
        final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, URISyntaxException, InvalidSettingsException {
        var spec = wpo.getSpec();
        var segment = spec.getWorkflowSegment();
        var wfm = segment.loadWorkflow();
        var newSegment = new WorkflowSegment(wfm, segment.getConnectedInputs(), segment.getConnectedOutputs(),
            segment.getPortObjectReferenceReaderNodes());
        try {
            writeReferenceReaderNodeData(segment, wfm, dataDir, exec);
            return new WorkflowPortObject(new WorkflowPortObjectSpec(newSegment, spec.getWorkflowName(),
                spec.getInputIDs(), spec.getOutputIDs()));
        } finally {
            newSegment.serializeAndDisposeWorkflow();
            segment.disposeWorkflow();
        }
    }

    /**
     * Checks a {@link WorkflowLoadResult} and possibly turns it into a warning message or an exception.
     *
     * @param lr the load result to check
     *
     * @return a warning message if there are warnings, or else <code>null</code>
     * @throws IllegalStateException thrown if there are loading errors
     */
    private static String checkLoadResult(final WorkflowLoadResult lr) {
        switch (lr.getType()) {
            case Warning:
                return "Problem(s) while loading the workflow:\n" + lr;
            case Error:
                throw new IllegalStateException("Error(s) while loading the workflow: \n" + lr);
            case Ok:
            case DataLoadError: // ignore data load errors
            default:
                return null;

        }
    }

    /**
     * Helper method to load the workflow from a {@link WorkflowSegment}.
     *
     * @param ws the segment to load the workflow from
     * @param warningConsumer called if there was a warning while loading
     * @return the load workflow manager
     *
     * @throws IllegalStateException if there were loading errors
     */
    private static WorkflowManager loadWorkflow(final WorkflowSegment ws, final Consumer<String> warningConsumer) {
        AtomicReference<IllegalStateException> exception = new AtomicReference<>();
        var wfm = ws.loadWorkflow(lr -> { // NOSONAR
            try {
                String warning = checkLoadResult(lr);
                if (warning != null) {
                    warningConsumer.accept(warning);
                }
            } catch (IllegalStateException e) {
                exception.set(e);
                return;
            }
        });

        if (exception.get() != null) {
            ws.disposeWorkflow();
            throw exception.get();
        }
        return wfm;
    }
}
