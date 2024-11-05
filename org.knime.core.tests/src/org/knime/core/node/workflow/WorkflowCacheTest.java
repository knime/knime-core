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
 *   5 Nov 2024 (jasper): created
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.workflow.WorkflowResourceCache.WorkflowResource;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;

/**
 * Test the {@link WorkflowResourceCache}.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
final class WorkflowCacheTest {

    @SuppressWarnings("static-method")
    @Test
    void testCacheOnlyAtProjectWFM(@TempDir final Path tempDir) {
        final var contextV2 = WorkflowContextV2.forTemporaryWorkflow(tempDir, null);
        final var wfm = WorkflowManager.ROOT.createAndAddProject(WorkflowCacheTest.class.getSimpleName(),
            new WorkflowCreationHelper(contextV2));
        final var cache = wfm.getWorkflowResourceCache();
        assertNotNull(cache, "Cache should not be null for WFM project");

        final var collapseResult = wfm.collapseIntoMetaNode(new NodeID[0], new WorkflowAnnotationID[0], "MetaNode");
        final WorkflowManager metanode =
            wfm.getNodeContainer(collapseResult.getCollapsedMetanodeID(), WorkflowManager.class, true);
        final var metanodeCache = metanode.getWorkflowResourceCache();
        assertNull(metanodeCache, "Cache should be null for metanode");

        final var convertResult = wfm.convertMetaNodeToSubNode(metanode.getID());
        final SubNodeContainer subnode =
            wfm.getNodeContainer(convertResult.getConvertedNodeID(), SubNodeContainer.class, true);
        final var subnodeCache = subnode.getWorkflowManager().getWorkflowResourceCache();
        assertNull(subnodeCache, "Cache should be null for subnode");

        // no context, no resource cache
        assertThrows(IllegalStateException.class,
            () -> WorkflowResourceCache.computeIfAbsent(MyResource.class, () -> new MyResource(new AtomicBoolean())),
            "Should throw an exception if no context is present");

        final AtomicBoolean disposeCalledFlag = new AtomicBoolean(false);
        NodeContext.pushContext(subnode);
        try {
            assertTrue(WorkflowResourceCache.get(MyResource.class).isEmpty(), "No resource cache should be present");
            MyResource resource =
                WorkflowResourceCache.computeIfAbsent(MyResource.class, () -> new MyResource(disposeCalledFlag));
            assertNotNull(resource, "Resource should not be null");
            assertFalse(disposeCalledFlag.get(), "Dispose should not have been called");
            assertNotNull(cache.getInternalMap().get(MyResource.class), "Cache should contain the Resource object");
            assertTrue(WorkflowResourceCache.get(MyResource.class).isPresent(), "Resource cache should be present");

            WorkflowManager.ROOT.removeProject(wfm.getID());
            assertThrows(IllegalStateException.class, () -> WorkflowResourceCache.computeIfAbsent(MyResource.class,
                () -> new MyResource(new AtomicBoolean())), "Should throw an exception after workflow is disposed");
        } finally {
            NodeContext.removeLastContext();
        }

        assertNull(cache.getInternalMap().get(MyResource.class), "Cache should no longer contain the Resource object");
        assertTrue(disposeCalledFlag.get(), "Dispose should have been called");
        assertTrue(cache.getInternalMap().isEmpty(), "Cache should be empty");

        cache.toString(); // only for coverage
    }


    private static final class MyResource implements WorkflowResource {

        private final AtomicBoolean m_disposeCalledFlag;

        MyResource(final AtomicBoolean disposeCalledFlag) {
            m_disposeCalledFlag = disposeCalledFlag;
        }

        @Override
        public void dispose() {
            m_disposeCalledFlag.set(true);
        }
    }


}
