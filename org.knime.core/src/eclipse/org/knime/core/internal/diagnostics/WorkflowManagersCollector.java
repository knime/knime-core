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
 *   Sep 29, 2025 (manuelhotz): created
 */
package org.knime.core.internal.diagnostics;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Workflow manager information diagnostic collector.
 *
 * @since 5.8
 */
public final class WorkflowManagersCollector implements Collector {

    /** The singleton instance. */
    public static final WorkflowManagersCollector INSTANCE = new WorkflowManagersCollector();

    private WorkflowManagersCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.workflowManagers();
    }

    @Override
    public String getJsonKey() {
        return "workflowManagers";
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        generator.writeArrayFieldStart("projects");
        walkWorkflowManager(generator, WorkflowManager.ROOT);
        generator.writeEndArray();
    }

    private static boolean walkWorkflowManager(final JsonGenerator generator, final WorkflowManager workflowManager)
        throws IOException {
        // ROOT has children, e.g. METANODE_ROOT, which itself hosts the projects. The logic below is to skip those
        // meta projects (best effort)
        boolean foundProject = false;
        for (NodeContainer nc : workflowManager.getNodeContainers()) {
            if (nc instanceof WorkflowManager wfm && wfm.isProject()) {
                if (!walkWorkflowManager(generator, wfm)) {
                    writeWorkflowProject(generator, wfm);
                    foundProject = true;
                }
            } else {
                return false;
            }
        }
        return foundProject;
    }

    private static void writeWorkflowProject(final JsonGenerator generator, final WorkflowManager workflowManager)
            throws IOException {
        generator.writeStartObject();
        writeNodeStatusInto(generator, workflowManager);
        generator.writeStringField("context", Objects.toString(workflowManager.getContextV2(), ""));
        // TODO this needs to be refactored and moved into the workflow manager package, so that iteration
        // of children can be done without having acquired the lock
        final ReentrantLock lock = workflowManager.getReentrantLockInstance();
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5L, TimeUnit.SECONDS);
            generator.writeBooleanField("isLockable", isLocked);
            if (!isLocked) {
                generator.writeStringField("lockOwner", getLockOwnerNameWithCommons(lock));
            }
            generator.writeBooleanField("isWizardExecution", workflowManager.isInWizardExecution());
            writeWorkflowChildren(generator, workflowManager);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while trying to acquire workflow manager lock", ex);
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
        generator.writeEndObject();
    }

    private static String getLockOwnerNameWithCommons(final ReentrantLock lock) {
        try {
            Thread owner = (Thread) MethodUtils.invokeMethod(lock, true, "getOwner");
            return owner != null ? owner.getName() : "No owner";
        } catch (Exception e) { // NOSONAR -- accepted, best effort
            return "Unable to determine owner";
        }
    }

    private static void writeWorkflowChildren(final JsonGenerator generator, final WorkflowManager workflowManager)
            throws IOException {
        generator.writeArrayFieldStart("children");
        for (final NodeContainer nc : workflowManager.getNodeContainers()) {
            generator.writeStartObject();
            writeNodeStatusInto(generator, nc);
            if (nc instanceof WorkflowManager wfm) {
                writeWorkflowChildren(generator, wfm);
            } else if (nc instanceof SubNodeContainer snc) {
                writeWorkflowChildren(generator, snc.getWorkflowManager());
            }
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    private static void writeNodeStatusInto(final JsonGenerator generator, final NodeContainer nodeContainer)
        throws IOException {
        generator.writeStringField("name", nodeContainer.getNameWithID());
        String type;
        if (nodeContainer instanceof WorkflowManager wfm) {
            type = String.format("WorkflowManager (%s)", wfm.isProject() ? "project" : "metanode");
        } else if (nodeContainer instanceof SubNodeContainer){
            type = "SubNodeContainer";
        } else {
            type = "NativeNodeContainer";
        }
        generator.writeStringField("type", type);
        generator.writeStringField("status", nodeContainer.getNodeContainerState().toString());
    }
}
