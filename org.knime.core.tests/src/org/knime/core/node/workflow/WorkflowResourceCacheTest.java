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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Test;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.Mockito;

/**
 * Unit tests for {@link WorkflowResourceCache}.
 */
@SuppressWarnings("resource") // caches disposed by tests as needed
public class WorkflowResourceCacheTest {

    @After
    public void tearDown() {
        // clean up any lingering context to not leak across tests
        while (NodeContext.getContext() != null) {
            NodeContext.removeLastContext();
        }
    }

    @Test
    public void testComputeIfAbsentCachesPerWorkflow() throws IOException {
        var cache = new WorkflowResourceCache();
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        Mockito.when(wfm.getWorkflowResourceCache()).thenReturn(cache);

        NodeContext.pushContext(wfm);

        var calls = new AtomicInteger();
        Supplier<org.knime.core.node.workflow.WorkflowResourceCacheTest.TestResource> testResourceSupplier = () -> {
            calls.incrementAndGet();
            return new TestResource();
        };
        var first = WorkflowResourceCache.computeIfAbsent(TestResource.class, testResourceSupplier);
        var second = WorkflowResourceCache.computeIfAbsent(TestResource.class, testResourceSupplier);

        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    public void testPutAndGetFromCache() {
        var cache = new WorkflowResourceCache();

        assertFalse(cache.getFromCache(TestResource.class).isPresent());

        var first = new TestResource();
        assertNull(cache.put(TestResource.class, first));
        assertSame(first, cache.getFromCache(TestResource.class).orElseThrow());

        var second = new TestResource();
        assertSame(first, cache.put(TestResource.class, second));
        assertSame(second, cache.getFromCache(TestResource.class).orElseThrow());
    }

    @Test
    public void testDisposeClearsEntriesAndDisposesResources() {
        var cache = new WorkflowResourceCache();
        var resource = new DisposingResource();
        cache.put(DisposingResource.class, resource);

        cache.dispose();

        assertTrue("dispose() should clear cached entries", cache.getFromCache(DisposingResource.class).isEmpty());
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
