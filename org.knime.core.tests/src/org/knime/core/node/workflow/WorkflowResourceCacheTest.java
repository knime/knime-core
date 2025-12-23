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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 23, 2025 (assistant): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;

/**
 * Unit tests for {@link WorkflowResourceCache}.
 */
@SuppressWarnings("resource") // caches disposed by tests as needed
public class WorkflowResourceCacheTest {

    private static NodeID wfmID;

    @Before
    public void setUp() throws IOException {
        var wfm = createEmptyWorkflow();
        wfmID = wfm.getID();
        NodeContext.pushContext(wfm);
    }

    private static WorkflowManager createEmptyWorkflow() throws IOException {
        var dir = FileUtil.createTempDir("workflow");
        var workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        if (workflowFile.createNewFile()) {
            return WorkflowManager.ROOT.createAndAddProject("workflow", new WorkflowCreationHelper(
                WorkflowContextV2.forTemporaryWorkflow(workflowFile.getParentFile().toPath(), null)));
        } else {
            throw new IllegalStateException("Creating empty workflow failed");
        }
    }

    @After
    public void tearDown() {
        NodeContext.removeLastContext();
        if (WorkflowManager.ROOT.containsNodeContainer(wfmID)) {
            WorkflowManager.ROOT.removeProject(wfmID);
        }
    }

    @Test
    public void testComputeIfAbsentCachesPerWorkflow() {
        var calls = new AtomicInteger();
        Supplier<TestResource> testResourceSupplier = () -> {
            calls.incrementAndGet();
            return new TestResource();
        };
        var first = WorkflowResourceCache.computeIfAbsent(TestResource.class, testResourceSupplier);
        var second = WorkflowResourceCache.computeIfAbsent(TestResource.class, testResourceSupplier);

        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    public void testDisposeClearsEntriesAndDisposesResources() {
        var resource = new DisposingResource();
        WorkflowResourceCache.computeIfAbsent(DisposingResource.class, () -> resource);
        assertTrue("Resource should be cached before dispose()",
            !WorkflowResourceCache.get(DisposingResource.class).isEmpty());

        WorkflowManager.ROOT.removeProject(wfmID);

        assertTrue("dispose() should call WorkflowResource.dispose()", resource.disposed);
    }

    private static final class TestResource implements WorkflowResourceCache.WorkflowResource {
        @Override
        public void dispose() {
            // no-op
        }
    }

    private static final class DisposingResource implements WorkflowResourceCache.WorkflowResource {
        boolean disposed;

        @Override
        public void dispose() {
            disposed = true;
        }
    }
}
