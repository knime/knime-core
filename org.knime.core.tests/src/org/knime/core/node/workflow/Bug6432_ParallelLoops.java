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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Bug 6432: Premature state notification in case workflow contains parallel loops
 * https://bugs.knime.org/show_bug.cgi?id=6432
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6432_ParallelLoops extends WorkflowTestCase {

    @Before
    public void setUp() throws Exception {
        loadAndSetWorkflow();
    }

    /** Loads workflow and executes as is - expects certain event count. */
    @Test
    public void testExecutePlain() throws Exception {
        final WorkflowManager manager = getManager();
        checkState(manager, InternalNodeContainerState.IDLE);
        checkListenerAndStateAfterExecAll(manager);
    }

    /**
     * @param manager
     * @throws Exception
     */
    private void checkListenerAndStateAfterExecAll(final WorkflowManager manager) throws Exception {
        final StringBuilder msg = new StringBuilder();
        final List<InternalNodeContainerState> eventList = new ArrayList<InternalNodeContainerState>();
        manager.addNodeStateChangeListener(new NodeStateChangeListener() {
            @Override
            public void stateChanged(final NodeStateEvent state) {
                final InternalNodeContainerState curState = manager.getInternalState();
                final int count = eventList.size();
                eventList.add(curState);
                switch (count) {
                    case 0:
                        if (!curState.equals(InternalNodeContainerState.EXECUTING)) {
                            msg.append(msg.length() == 0 ? "" : "\n");
                            msg.append("First event should be EXECUTING but is ").append(curState);
                        }
                        break;
                    case 1:
                        if (!curState.equals(InternalNodeContainerState.EXECUTED)) {
                            msg.append(msg.length() == 0 ? "" : "\n");
                            msg.append("Second event should be EXECUTED but is ").append(curState);
                        }
                        break;
                    default:
                }
            }
        });
        executeAllAndWait();
        if (eventList.size() != 2) {
            msg.append(msg.length() == 0 ? "" : "\n");
            msg.append("Too many events received: ").append(eventList.size()).append(" -- ").append(
                ConvenienceMethods.getShortStringFrom(eventList, 100));
        }
        checkState(manager, InternalNodeContainerState.EXECUTED);
        if (msg.length() > 0) {
            fail(msg.toString());
        }
    }

}
