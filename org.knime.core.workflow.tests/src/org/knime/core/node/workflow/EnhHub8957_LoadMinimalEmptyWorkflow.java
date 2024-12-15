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
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.util.LoadVersion;

/**
 * Test to load a minimal workflow; i.e. a workflow file that contains a little
 * information as possible with as little assumptions about the workflow as
 * possible (e.g. AP version).
 * 
 * This minimal workflow is being used by the Hub (catalog service) in order to
 * 'create' an empty workflow (where the catalog service only should have as
 * little knowledge about AP internals as possible).
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhHub8957_LoadMinimalEmptyWorkflow extends WorkflowTestCase {

	/**
	 * Tests to load a minimal empty workflow.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLoadMinimalEmptyWorkflow() throws Exception {
		var loadResult = loadWorkflow(getDefaultWorkflowDirectory(), new ExecutionMonitor());
		assertThat(Arrays.stream(loadResult.getChildren()).map(LoadResultEntry::getType)
				.anyMatch(t -> t == LoadResultEntryType.Error), is(false));

		var wfm = loadResult.getWorkflowManager();
		setManager(wfm);

		assertThat(wfm.getName(), is("enhHub8957_LoadMinimalEmptyWorkflow"));
		assertThat(wfm.getNodeContainers(), empty());
		assertThat(wfm.getLoadVersion(), is(LoadVersion.UNKNOWN));
	}

}