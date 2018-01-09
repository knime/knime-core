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
 *   20 Apr 2017 (albrecht): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;
import org.knime.core.wizard.SinglePageManager;
import org.knime.core.wizard.SubnodeViewValue;
import org.knime.core.wizard.SubnodeViewableModel;
import org.knime.core.wizard.SubnodeViewableModel.CollectionValidationError;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.testing.node.blocking.BlockingRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class TestSubnodeView extends WorkflowTestCase {

    private static final String LOCK_ID = "lock_node_7";

    private SinglePageManager m_spm;
    private NodeID m_subnodeID;
    private NodeIDSuffix m_stringInputID;
    private NodeIDSuffix m_tableViewID;
    private NodeID m_blockID;

    private static final String DEFAULT_URL = "https://www.knime.com/";
    private static final String CHANGED_URL = "http://knime.org/";

    /**
     * Load workflow, setup node ids
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_subnodeID = new NodeID(baseID, 6);
        m_stringInputID = NodeID.NodeIDSuffix.create(baseID, new NodeID(new NodeID(m_subnodeID, 0), 4));
        m_tableViewID = NodeID.NodeIDSuffix.create(baseID, new NodeID(new NodeID(m_subnodeID, 0), 3));
        m_blockID = new NodeID(baseID, 7);
        m_spm = SinglePageManager.of(getManager());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        BlockingRepository.remove(LOCK_ID);
    }

    /**
     * Simple execute test, just a sanity check
     * @throws Exception
     */
    @Test
    public void testExecuteAll() throws Exception {
        executeAllAndWait();
        final WorkflowManager wfm = getManager();
        checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

    private void initialExecute() throws Exception {
        executeAndWait(m_subnodeID);
        assertTrue("Subnode should be executed.", getManager().getNodeContainer(m_subnodeID).getNodeContainerState().isExecuted());
    }

    /**
     * Simple test if a combined view can succesfully be created and contains the layout and expected nodes for display
     * @throws Exception
     */
    @Test
    public void testExecuteAndCreateSubnodeView() throws Exception {
        initialExecute();
        SinglePageWebResourceController spc = new SinglePageWebResourceController(getManager(), m_subnodeID);
        assertTrue("Should have subnode view", spc.isSubnodeViewAvailable());
        WizardPageContent page = spc.getWizardPage();
        assertNotNull("Page content should be available", page);
        @SuppressWarnings("rawtypes")
        Map<NodeIDSuffix, WizardNode> pageMap = page.getPageMap();
        assertNotNull("Page map should be available", pageMap);
        assertEquals("Page should contain three nodes", 3, pageMap.size());
        String layout = page.getLayoutInfo();
        assertNotNull("Page layout should be available", layout);
        assertTrue("Layout should contain test string", layout.contains("testString"));
    }

    /**
     * Simple test if a serializable page object (view) can be created.
     * @throws Exception
     */
    @Test
    public void testExecuteAndCreateSerializablePageView() throws Exception {
        initialExecute();
        JSONWebNodePage page = m_spm.createWizardPage(m_subnodeID);
        validateJSONPage(page);
    }

    private void validateJSONPage(final JSONWebNodePage page) {
        assertNotNull("Page should have config object", page.getWebNodePageConfiguration());
        assertNotNull("Page should have map of nodes", page.getWebNodes());
        assertNotNull("Page should have layout information", page.getWebNodePageConfiguration().getLayout());
        JSONLayoutPage layout = page.getWebNodePageConfiguration().getLayout();
        assertNotNull("Layout should contain list of rows", layout.getRows());
        assertEquals("Layout should contain four rows", 4, layout.getRows().size());
        assertEquals("Page should contain three nodes", 3, page.getWebNodes().size());
    }

    private Map<String, String> buildValueMap() throws Exception {
        Map<String, String> valueMap = m_spm.createWizardPageViewValueMap(m_subnodeID);
        assertNotNull("Value map of page should exist", valueMap);
        assertTrue("Value map should contain three entries", valueMap.size() == 3);
        return valueMap;
    }

    private Map<String, String> changeStringInputTo(final String newValue, final Map<String, String> valueMap) throws Exception {
        //Map<String, String> valueMap = buildValueMap();
        String stringInputValue = valueMap.get(m_stringInputID.toString());
        assertNotNull("Value for string input node should exist", stringInputValue);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonValue = mapper.readTree(stringInputValue);
        assertNotNull("String input value should contain default string", jsonValue.get("string"));
        assertEquals("String input default value should be '" + DEFAULT_URL + "'", DEFAULT_URL, jsonValue.get("string").asText());
        ((ObjectNode)jsonValue).put("string", newValue);
        valueMap.put(m_stringInputID.toString(), mapper.writeValueAsString(jsonValue));
        return valueMap;
    }

    /**
     * Tests if changing the value of one of the nodes to an invalid string causes the correct validation error
     * @throws Exception
     */
    @Test
    public void testValidationError() throws Exception {
        initialExecute();
        Map<String, String> valueMap = changeStringInputTo("foo", buildValueMap());
        Map<String, ValidationError> errorMap = m_spm.validateViewValues(valueMap, m_subnodeID);
        assertNotNull("Error map should exist", errorMap);
        assertEquals("Error map should contain one entry", 1, errorMap.size());
        ValidationError error = errorMap.get(m_stringInputID.toString());
        assertNotNull("Error for string input should exist", error);
        assertEquals("Error for string input incorrect", "The given input 'foo' is not a valid URL", error.getError());
    }

    /**
     * Test if changing the value of one of the nodes to a valid string causes no validation error
     * @throws Exception
     */
    @Test
    public void testValidationSucceed() throws Exception {
        initialExecute();
        Map<String, String> valueMap = changeStringInputTo(CHANGED_URL, buildValueMap());
        Map<String, ValidationError> errorMap = m_spm.validateViewValues(valueMap, m_subnodeID);
        assertTrue("There should not be any validation errors", errorMap == null || errorMap.isEmpty());
    }

    /**
     * Tests if a changed value of one of the nodes validates and is applied correctly
     * @throws Exception
     */
    @Test
    public void testValidationAndReexecute() throws Exception {
        initialExecute();
        // change URL in string input
        Map<String, String> valueMap = changeStringInputTo(CHANGED_URL, buildValueMap());
        selectRowInTable(valueMap);

        // validate and reexecute
        Map<String, ValidationError> errorMap = m_spm.validateViewValues(valueMap, m_subnodeID);
        assertTrue("There should not be any validation errors", errorMap == null || errorMap.isEmpty());
        try (WorkflowLock lock = getManager().lock()) {
            m_spm.applyValidatedValuesAndReexecute(valueMap, m_subnodeID, false);
        }
        waitWhileNodeInExecution(m_subnodeID);
        assertTrue("Subnode should be executed.", getManager().getNodeContainer(m_subnodeID).getNodeContainerState().isExecuted());
        validateReexecutionResult();
    }

    /**
     * Tests if downstream nodes get reset as part of a re-execute of the subnode
     * @throws Exception
     */
    @Test
    public void testResetDownstreamNodes() throws Exception {
        initialExecute();
        executeAllAndWait();
        // change URL in string input
        Map<String, String> valueMap = changeStringInputTo(CHANGED_URL, buildValueMap());
        selectRowInTable(valueMap);

        try (WorkflowLock lock = getManager().lock()) {
            m_spm.applyValidatedValuesAndReexecute(valueMap, m_subnodeID, false);
        }
        waitWhileNodeInExecution(m_subnodeID);
        checkState(m_subnodeID, InternalNodeContainerState.EXECUTED);
        checkState(m_blockID, InternalNodeContainerState.CONFIGURED);
    }

    /**
     * Tests if downstream nodes get reset as part of a re-execute of the subnode
     * @throws Exception
     */
    @Test(expected = IllegalStateException.class)
    public void testApplyNewValueWhileDownstreamIsExecuting() throws Exception {
        initialExecute();
        ReentrantLock blockLock = BlockingRepository.get(LOCK_ID);
        blockLock.lock();
        try {
            getManager().executeUpToHere(m_blockID);
            NodeContainer blockNC = getManager().getNodeContainer(m_blockID);
            Assert.assertThat("Blocking node 7 should be executing",
                blockNC.getNodeContainerState().isExecutionInProgress(), CoreMatchers.is(Boolean.TRUE));
            // change URL in string input
            Map<String, String> valueMap = changeStringInputTo(CHANGED_URL, buildValueMap());
            selectRowInTable(valueMap);
            try (WorkflowLock lock = getManager().lock()) {
                m_spm.applyValidatedValuesAndReexecute(valueMap, m_subnodeID, false);
            }
            Assert.fail("Shouldn't be able to apply values while downstream nodes are executing");
        } finally {
            blockLock.unlock();
        }
    }

    private void selectRowInTable(final Map<String, String> valueMap) throws IOException, JsonProcessingException {
        // select one value in table
        String tableValueString = valueMap.get(m_tableViewID.toString());
        assertNotNull("Value for table view node should exist", tableValueString);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tableValue = mapper.readTree(tableValueString);
        assertEquals("Default selection should not exist", NullNode.instance, tableValue.get("selection"));
        ArrayNode selectionArray = ((ObjectNode)tableValue).putArray("selection");
        selectionArray.add("Row1");
        valueMap.put(m_tableViewID.toString(), mapper.writeValueAsString(tableValue));
    }

    /**
     * Creates and tests the {@link SubnodeViewableModel}
     * @throws Exception
     */
    @Test
    public void testSubnodeViewableModel() throws Exception {
        initialExecute();

        SubNodeContainer snc = getManager().getNodeContainer(m_subnodeID, SubNodeContainer.class, true);
        SubnodeViewableModel svm = new SubnodeViewableModel(snc, "testView");
        JSONWebNodePage page = svm.getViewRepresentation();
        validateJSONPage(page);
        SubnodeViewValue viewValue = svm.getViewValue();
        assertNotNull("Combined view value should exist", viewValue);
        assertNotNull("Combined view value map should exist", viewValue.getViewValues());
        assertEquals("Combined view value should contain three entries", 3, viewValue.getViewValues().size());

        Map<String, String> changedValueMap = new HashMap<String, String>(3);
        changedValueMap.putAll(viewValue.getViewValues());
        changedValueMap = changeStringInputTo("foo", changedValueMap);
        SubnodeViewValue newViewValue = new SubnodeViewValue();
        newViewValue.setViewValues(changedValueMap);
        ValidationError validationError = svm.validateViewValue(newViewValue);
        assertTrue("Validation error should be instance of CollectionValidationError", validationError instanceof CollectionValidationError);
        Map<String, String> errorMap = ((CollectionValidationError)validationError).getErrorMap();
        assertNotNull("Error map should exist", errorMap);

        changedValueMap.putAll(viewValue.getViewValues());
        changeStringInputTo(CHANGED_URL, changedValueMap);
        selectRowInTable(changedValueMap);
        validationError = svm.validateViewValue(newViewValue);
        assertNull("There should not be any validation errors", validationError);
        svm.loadViewValue(newViewValue, false);

        waitWhileNodeInExecution(m_subnodeID);
        assertTrue("Subnode should be executed.", getManager().getNodeContainer(m_subnodeID).getNodeContainerState().isExecuted());
        validateReexecutionResult();

        svm.discard();
        assertNull("Page should be null after reset", svm.getViewRepresentation());
        assertNull("View value should be null after reset", svm.getViewValue());
    }

    private void validateReexecutionResult() throws Exception {
        // validate results
        Map<String, String> newValueMap = buildValueMap();
        String stringInputValue = newValueMap.get(m_stringInputID.toString());
        assertNotNull("Value for string input node should exist", stringInputValue);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonValue = mapper.readTree(stringInputValue);
        assertNotNull("String input value should contain new string value", jsonValue.get("string"));
        assertEquals("String input value should be '" + CHANGED_URL + "'", CHANGED_URL, jsonValue.get("string").asText());

        // check one row was selected and filtered
        NodeContainer container = getManager().getNodeContainer(m_subnodeID);
        BufferedDataTable table = (BufferedDataTable)container.getOutPort(1).getPortObject();
        assertEquals("Table should contain one selected row", 1, table.size());
        try (CloseableRowIterator it = table.iterator()) {
            DataRow row = it.next();
            assertEquals("Filtered row should be the previously selected one", "Row1", row.getKey().toString());
        }

        // check flow variable created from string input
        NodeContainer cacheContainer = getManager().getNodeContainer(m_blockID);
        FlowVariable var = cacheContainer.getFlowObjectStack().getAvailableFlowVariables().get("string-input");
        assertNotNull("Flow variable 'string input' should exist", var);
        assertEquals("Flow variable should contain new string value", CHANGED_URL, var.getStringValue());
    }

}
