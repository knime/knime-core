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
 * History
 *   Feb 2, 2017 (bjoern): created
 */
package org.knime.core.node.workflow.execresult;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test fixtures and assert methods for execution results, specifically relating to Json (de)serialization.
 *
 * @author Bjoern Lohrmann, KNIME.com GmbH
 */
public class ExecutionResultFixtures {

    /**
     * @return a fixture object
     */
    public static NodeExecutionResult createNodeExecutionResultFixture() {
        NodeExecutionResult toReturn = new NodeExecutionResult();
        toReturn.setPortObjects(new PortObject[]{InactiveBranchPortObject.INSTANCE, InactiveBranchPortObject.INSTANCE});
        toReturn.setPortObjectSpecs(
            new PortObjectSpec[]{InactiveBranchPortObjectSpec.INSTANCE, InactiveBranchPortObjectSpec.INSTANCE});
        toReturn.setInternalHeldPortObjects(new PortObject[]{InactiveBranchPortObject.INSTANCE});
        toReturn.setWarningMessage("a warning");
        toReturn.setNeedsResetAfterLoad();
        return toReturn;
    }

    /**
     * @return a fixture object
     */
    public static NativeNodeContainerExecutionResult createNativeNodeContainerExecutionResultFixture() {
        return createNativeNodeContainerExecutionResultFixture(0);
    }

    /**
     * @param someId A number to customize the node message.
     * @return a fixture object
     */
    public static NativeNodeContainerExecutionResult createNativeNodeContainerExecutionResultFixture(final int someId) {
        NativeNodeContainerExecutionResult toReturn = new NativeNodeContainerExecutionResult();
        toReturn.setNodeExecutionResult(createNodeExecutionResultFixture());
        setGeneralNodeContainerProperties(toReturn, "native node error" + someId);
        return toReturn;
    }

    /**
     * @return a fixture object
     */
    public static WorkflowExecutionResult createWorkflowExecutionResultFixture() {
        NodeID baseID = NodeID.fromString("0:1:0");
        WorkflowExecutionResult toReturn = new WorkflowExecutionResult(baseID);
        toReturn.addNodeExecutionResult(new NodeID(baseID, 0), createNativeNodeContainerExecutionResultFixture(0));
        toReturn.addNodeExecutionResult(new NodeID(baseID, 1), createNativeNodeContainerExecutionResultFixture(1));
        setGeneralNodeContainerProperties(toReturn, "workflow error");
        return toReturn;
    }

    /**
     * Sets the properties defined by the abstract {@link NodeContainerExecutionResult} class to fixture values.
     *
     * @param execResult Object to set properties on.
     * @param errorMsg An error message to set (as part of {@link NodeMessage})
     */
    public static void setGeneralNodeContainerProperties(final NodeContainerExecutionResult execResult,
        final String errorMsg) {
        execResult.setMessage(new NodeMessage(Type.ERROR, errorMsg));
        execResult.setNeedsResetAfterLoad();
        execResult.setSuccess(true);
    }

    /**
     * Compares the properties defined by the abstract {@link NodeContainerExecutionResult} class between the given
     * original and a new deserialized object.
     *
     * @param deserialized
     * @param orig
     */
    public static void assertEqualityAfterDeserialization(final NodeContainerExecutionResult deserialized,
        final NodeContainerExecutionResult orig) {
        assertThat("Object is not the same", deserialized, not(sameInstance(orig)));
        assertThat("Class is the same", deserialized.getClass(), sameInstance(orig.getClass()));
        assertThat("Node message is equal", deserialized.getNodeMessage(), is(orig.getNodeMessage()));
        assertThat("Needs reset after load is equal", deserialized.needsResetAfterLoad(),
            is(orig.needsResetAfterLoad()));
        assertThat("Nr port objects is equal", deserialized.isSuccess(), is(orig.isSuccess()));
    }

    /**
     *
     * @return a fixture object
     */
    public static SubnodeContainerExecutionResult createSubnodeContainerExecutionResultFixture() {
        WorkflowExecutionResult wfResult = createWorkflowExecutionResultFixture();
        SubnodeContainerExecutionResult toReturn =
            new SubnodeContainerExecutionResult(wfResult.getBaseID().getPrefix());
        toReturn.setWorkflowExecutionResult(wfResult);
        return toReturn;
    }

    /**
     * @return a Jackson object mapper for use within unit tests (needs the OSGI bundle class loader)
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withClassLoader(ExecutionResultFixtures.class.getClassLoader()));
        return mapper;
    }
}
