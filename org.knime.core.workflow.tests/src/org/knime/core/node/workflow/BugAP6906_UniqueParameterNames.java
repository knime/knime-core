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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.OutputNode;


/** AP-6906: Usability of Call Workflow Nodes with Respect to Input and Output Parameters
 * https://knime-com.atlassian.net/browse/AP-6906
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP6906_UniqueParameterNames extends WorkflowTestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        loadAndSetWorkflow();
    }

    /** Just list the two JSON Output nodes. */
    @Test
    public void testOutputNamesPositive() throws Exception {
        List<ExternalParameterHandle<ExternalNodeData>> outputNodeList = getManager().getExternalParameterHandles(
            OutputNode.class, o -> o.getExternalOutput().getID(), o -> o.getExternalOutput(), true, true);
        Assert.assertThat("Wrong number of output nodes", outputNodeList.size(), is(2));
        List<String> shortNames = outputNodeList.stream()
                .map(ExternalParameterHandle::getParameterNameShort)
                .collect(Collectors.toList());
        Assert.assertThat("Wrong 'uniquified' output parameter names", shortNames,
            hasItems("output-not-unique-4", "output-not-unique-8"));

        List<String> fullyQualifiedNames = outputNodeList.stream()
                .map(ExternalParameterHandle::getParameterNameFullyQualified)
                .collect(Collectors.toList());
        Assert.assertThat("Wrong 'fully qualified' output parameter names", fullyQualifiedNames,
            hasItems("output-not-unique-4", "output-not-unique-8"));
    }

    /** List the various input nodes with their name, either as-is or appended by node id. */
    @Test
    public void testInputNamesPositive() throws Exception {
        List<ExternalParameterHandle<ExternalNodeData>> inputNodes = getManager().getExternalParameterHandles(
            InputNode.class, i -> i.getInputData().getID(), InputNode::getInputData, true, true);
        Assert.assertThat("Wrong number of input nodes", inputNodes.size(), is(5));

        List<String> shortNames = inputNodes.stream()
                .map(ExternalParameterHandle::getParameterNameShort)
                .collect(Collectors.toList());

        Assert.assertThat("Wrong 'uniquified' input parameter names", shortNames, hasItems("input-unique",
            // these items are all duplicates, so uniquified with the node id suffix (partly meta or sub nodes)
            "input-not-unique-2", "input-not-unique-10:2", "input-not-unique-9", "input-not-unique-7:9:2"));

        List<String> fullyQualifiedNames = inputNodes.stream()
                .map(ExternalParameterHandle::getParameterNameFullyQualified)
                .collect(Collectors.toList());
        Assert.assertThat("Wrong 'fully qualifed' input parameter names", fullyQualifiedNames,
            hasItems("input-unique-1", "input-not-unique-2", "input-not-unique-10:2", "input-not-unique-9",
                "input-not-unique-7:9:2"));
    }

    /** Setting a unique parameter. */
    @Test
    public void testSettingSingleUniqueInputNodePositive() throws Exception {
        Map<String, ExternalNodeData> inputNodes = getManager().getInputNodes();
        getManager().setInputNodes(Collections.singletonMap("input-unique", inputNodes.get("input-unique-1")));
    }

    /** Setting a non-unique parameter. */
    @Test
    public void testSettingNonUniqueInputNodePositive() throws Exception {
        Map<String, ExternalNodeData> inputNodes = getManager().getInputNodes();
        getManager().setInputNodes(Collections.singletonMap("input-not-unique-9", inputNodes.get("input-not-unique-9")));
    }

    /** Setting a parameter, which isn't defined in the workflow. Expected to fail. */
    @Test
    public void testSettingInvalidParameterNegative() throws Exception {
        Map<String, ExternalNodeData> inputNodes = getManager().getInputNodes();

        exception.expect(InvalidSettingsException.class);
        exception.expectMessage("Parameter name \"invalid\" doesn't match any node in the workflow");
        getManager().setInputNodes(Collections.singletonMap("invalid", inputNodes.get("input-not-unique-2")));
    }

    /** Setting a parameter, which is ambigious but not fully qualified. Expected to fail. */
    @Test
    public void testSettingDuplicateParameterNegative() throws Exception {
        Map<String, ExternalNodeData> inputNodes = getManager().getInputNodes();

        exception.expect(InvalidSettingsException.class);
        exception.expectMessage("doesn't match");
        getManager().setInputNodes(Collections.singletonMap("input-not-unique", inputNodes.get("input-not-unique-2")));
    }

}
