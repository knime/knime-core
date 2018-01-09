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

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;


/** Fully executes a flow and then loads bogus settings into the nodes - expected to choke and should not reset flow.
 * Bug 4754: NodeContainer classes need dedicated validateSettings method
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug4754_settingsValidationBeforeReset extends WorkflowTestCase {

    private NodeID m_tableCreator_1;
    private NodeID m_metaNode_2;
    private NodeID m_subNode_3;
    private NodeID m_rowSampler_4;
    private NodeID m_tableChecker_5;
    private NodeID m_tableChecker_6;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreator_1 = new NodeID(baseID, 1);
        m_metaNode_2 = new NodeID(baseID, 2);
        m_subNode_3 = new NodeID(baseID, 3);
        m_rowSampler_4 = new NodeID(baseID, 4);
        m_tableChecker_5 = new NodeID(baseID, 5);
        m_tableChecker_6 = new NodeID(baseID, 6);
    }

    /** **Disabled** as meta nodes do no reasonable settings validation yet. Currently no bug open to "fix" it as
     * the old quickform nodes in conjunction with meta nodes are likely not be deprecated in the future.
     * @throws Exception ...
     */
    @Test
    public void testLoadBogusIntoMetaNode() throws Exception {
        Assume.assumeTrue("No settings validation in metanodes (yet)", false);
        WorkflowManager manager = getManager();
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableCreator_1, m_metaNode_2, m_tableChecker_5);
        NodeSettings s = new NodeSettings("meta-node");
        manager.saveNodeSettings(m_metaNode_2, s);
        NodeSettings bogusSettings = new NodeSettings("meta-node");
        s.copyTo(bogusSettings);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
        try {
            manager.loadNodeSettings(m_metaNode_2, bogusSettings);
            fail("not expected to reach this point");
        } catch (InvalidSettingsException ise) {
            // expected
        }
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableCreator_1, m_metaNode_2, m_tableChecker_5);
        reset(m_tableCreator_1);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
    }

    /** Try to load invalid settings into executed sub node.
     * @throws Exception ...
     */
    @Test
    public void testLoadBogusIntoSubNode() throws Exception {
        WorkflowManager manager = getManager();
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableCreator_1);
        checkStateOfMany(InternalNodeContainerState.IDLE, m_subNode_3, m_tableChecker_6);
        NodeSettings s = new NodeSettings("sub-node");
        manager.saveNodeSettings(m_subNode_3, s);
        NodeSettings bogusSettings = new NodeSettings("sub-node");
        s.copyTo(bogusSettings);
        breakFirstIntSettings(bogusSettings);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
        try {
            manager.loadNodeSettings(m_subNode_3, bogusSettings);
            fail("not expected to reach this point");
        } catch (InvalidSettingsException ise) {
            // expected
        }
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableCreator_1, m_subNode_3, m_tableChecker_6);
        reset(m_tableCreator_1);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
    }

    /** Try to load invalid settings into native node.
     * @throws Exception ...
     */
    @Test
    public void testLoadBogusIntoNativeNode() throws Exception {
        WorkflowManager manager = getManager();
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableCreator_1, m_rowSampler_4, m_tableChecker_5);
        NodeSettings s = new NodeSettings("native-node");
        manager.saveNodeSettings(m_rowSampler_4, s);
        NodeSettings bogusSettings = new NodeSettings("native-node");
        s.copyTo(bogusSettings);
        breakFirstIntSettings(bogusSettings);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
        try {
            manager.loadNodeSettings(m_rowSampler_4, bogusSettings);
            fail("not expected to reach this point");
        } catch (InvalidSettingsException ise) {
            // expected
        }
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableCreator_1, m_rowSampler_4, m_tableChecker_5);
        reset(m_tableCreator_1);
        executeAllAndWait();
        checkState(manager, InternalNodeContainerState.EXECUTED);
    }

    /** Modifies the argument by changing an "xint" config element into a "xstring" element. */
    private NodeSettings breakFirstIntSettings(final NodeSettings settings) throws Exception {
        final NodeSettings modelSettings = settings.getNodeSettings("model");
        for (String key : modelSettings.keySet()) {
            NodeSettings childSettings = modelSettings;
            try {
                childSettings = modelSettings.getNodeSettings(key);
            } catch (InvalidSettingsException ise) {
                // native node doesn't have settings children - it's all in place
            }
            for (String childKey : childSettings.keySet()) {
                int value = childSettings.getInt(childKey, -1);
                if (value != -1) {
                    childSettings.addInt(childKey, -value);
                    return settings;
                }
            }
        }
        throw new IllegalStateException("No 'int' settings found - can't create bogus node-settings object");

    }

}
