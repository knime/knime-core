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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.node.adapter.AdapterNodeFactory;
import org.knime.core.node.workflow.node.adapter.AdapterNodeModel;
import org.knime.core.util.FileUtil;

import java.io.File;

import static org.junit.jupiter.api.Assertions.fail;

public class BugAP15550_NPEinWorkflowContext extends WorkflowTestCase {

    private NodeID m_metaNode;

    private static boolean m_accessContextFailed = false;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow(getDefaultWorkflowDirectory());
        m_metaNode = new NodeID(baseID, 2);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        m_accessContextFailed = false;
    }

    /**
     * Test saving a metanode as template, like via rightclick > metanode > share.
     * @throws Exception
     */
    @Test
    public void testSaveAsTemplate() throws Exception {
        WorkflowManager mnMgr = getManager().getNodeContainer(m_metaNode, WorkflowManager.class, true);
        assert mnMgr != null;
        mnMgr.createAndAddNode(new CheckAccessContextNodeFactory());
        File tmpDir = FileUtil.createTempDir(this.getClass().getName());
        try {
            mnMgr.saveAsTemplate(tmpDir, new ExecutionMonitor());
            if (m_accessContextFailed) {
                fail();
            }
        } finally {
            FileUtil.deleteRecursively(tmpDir);
        }
    }

    public static final class CheckAccessContextNodeFactory extends AdapterNodeFactory {

        private void checkAccessContext() {
            WorkflowContext wfCtx = NodeContext.getContext().getWorkflowManager().getContext();
            if (wfCtx == null) {
                m_accessContextFailed = true;
            }
        }

        @Override
        public AdapterNodeModel createNodeModel() {
            return new AdapterNodeModel(0,0) {
                @Override
                protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                    checkAccessContext();
                    return super.configure(inSpecs);
                }

                @Override
                protected void onDispose() {
                    super.onDispose();
                    checkAccessContext();
                }
            };
        }
    }
}
