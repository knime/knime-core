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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link NodeContainer#isResetable()}-method.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugWEBP824_IsResetable extends WorkflowTestCase {

	/**
	 * Tests that nodes (workflows and components) are resetable if they are just
	 * marked for execution (i.e. neither in execution nor queued).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIsResetableIfMarkedForExec() throws Exception {
		loadAndSetWorkflow();
		WorkflowManager wfm = getManager();
		wfm.executeAll();

		NodeContainer metanode = wfm.getNodeContainer(wfm.getID().createChild(5));
		assertTrue(metanode instanceof WorkflowManager);
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).untilAsserted(
				() -> assertThat(metanode.getInternalState(), is(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC)));
		assertTrue(metanode.isResetable(), "workflow expected to be resetable if marked for exec");

		NodeContainer component = wfm.getNodeContainer(wfm.getID().createChild(6));
		assertTrue(component instanceof SubNodeContainer);
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).untilAsserted(
				() -> assertThat(component.getInternalState(), is(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC)));
		assertTrue(component.isResetable(), "workflow expected to be resetable if marked for exec");

		NodeContainer node = wfm.getNodeContainer(wfm.getID().createChild(4));
		assertTrue(node instanceof NativeNodeContainer);
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).untilAsserted(
				() -> assertThat(node.getInternalState(), is(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC)));
		assertTrue(node.isResetable(), "workflow expected to be resetable if marked for exec");
	}

	@AfterEach
	public void cancelWorkflow() {
		getManager().cancelExecution();
	}

}