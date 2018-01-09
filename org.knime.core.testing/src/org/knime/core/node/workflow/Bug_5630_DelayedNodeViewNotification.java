/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.Node;
import org.knime.core.node.NodeView;
import org.knime.core.node.util.ViewUtils;

/**
 * Tests that an open view is correctly modified (or not) even if the workflow is closed already. Indirectly relates to
 * 5630: API change: NodeView#modelChanged now always called in EventDispatchThread
 * (partners & community asked to test their custom view implementations)
 * https://bugs.knime.org/show_bug.cgi?id=5630
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug_5630_DelayedNodeViewNotification extends WorkflowTestCase {

    private NodeID m_crossTab2;

    private final List<LoggingEvent> m_logEvents = new ArrayList<LoggingEvent>();

    private final AppenderSkeleton m_logAppender = new AppenderSkeleton() {
        {
            LevelRangeFilter filter = new LevelRangeFilter();
            filter.setLevelMin(Level.ERROR);
            filter.setLevelMax(Level.FATAL);
            addFilter(filter);
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            m_logEvents.clear();
        }

        @Override
        protected void append(final LoggingEvent event) {
            m_logEvents.add(event);
        }
    };

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_crossTab2 = new NodeID(baseID, 2);
        Logger.getRootLogger().addAppender(m_logAppender);
    }

    /**
     * Opens a view, executes the workflows, closes the workflow and checks whether view updates after closing the
     * workflow are filtered.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testExecuteAndClose() throws Exception {
        NativeNodeContainer crossTabNode = (NativeNodeContainer)findNodeContainer(m_crossTab2);
        NodeView<?> view = (NodeView<?>)crossTabNode.getView(0);
        Node.invokeOpenView(view, "Test View");
        ViewUtils.invokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                synchronized (Bug_5630_DelayedNodeViewNotification.this) {
                    try {
                        Bug_5630_DelayedNodeViewNotification.this.wait(10000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
        executeAllAndWait();
        checkState(m_crossTab2, InternalNodeContainerState.EXECUTED);
        closeWorkflow();
        synchronized (this) {
            notifyAll();
        }
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
            }
        });
        if (!m_logEvents.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (LoggingEvent ev : m_logEvents) {
                errors.append(ev.getRenderedMessage()).append("; ");
            }
            errors.delete(errors.length() - 2, errors.length());

            throw new Exception("Unexpected exception while executing test: " + errors.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @After
    public void tearDown() throws Exception {
        Logger.getRootLogger().removeAppender(m_logAppender);
        super.tearDown();
    }
}
