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
import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;

import org.opentest4j.AssertionFailedError;

/**
 * Load a workflow created by older version of KNIME and load it. Don't expect any errors on load.
 *
 * @author wiswedel, KNIME AG
 */
public class BugAP7982_FutureKNIMEVersion_AllCompatible extends WorkflowTestCase {

    /** Load workflow, expect no errors. */
    @Test
    public void loadWorkflow() throws Exception {
        File wkfDir = getDefaultWorkflowDirectory();
        final WorkflowContextV2 workflowContext = WorkflowContextV2.forTemporaryWorkflow(wkfDir.toPath(), null);
        WorkflowLoadResult loadWorkflow = loadWorkflow(wkfDir, new ExecutionMonitor(),
        		new WorkflowLoadHelper(workflowContext, DataContainerSettings.getDefault()) {
            @Override
            public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
                final LoadVersion workflowKNIMEVersion, final Version createdByKNIMEVersion,
                final boolean isNightlyBuild) {
                throw new AssertionFailedError("Not to be called - workflow is expected to be compatible");
            }
        });
        setManager(loadWorkflow.getWorkflowManager());
        assertThat("Expected to loaded without errors", loadWorkflow.getType(), is(LoadResultEntryType.Ok));
        assertThat("Workflow version incorrect", getManager().getLoadVersion(), is(LoadVersion.V280));
    }
}