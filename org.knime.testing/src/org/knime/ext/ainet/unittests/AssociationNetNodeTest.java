/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *    16.10.2006 (Tobias Koetter): created
 */
package org.knime.ext.ainet.unittests;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNode;
import org.knime.ext.ainet.core.exceptions.NetConfigurationException;
import org.knime.ext.ainet.core.exceptions.PersistenceException;
import org.knime.ext.ainet.core.netimpl.AssociationNetFactory;

/**
 * Tests the adding and finding of nodes.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AssociationNetNodeTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AssociationNetNodeTest.class);
//test values
    private static final String UNIT_NODE_PREFIX = "UNIT_TEST@";
    
    private final String m_node1Name = UNIT_NODE_PREFIX + "Node1";

    private static AssociationNet netInstance;
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            netInstance =  AssociationNetFactory.createNet(
                    AINetTestSuite.getNetType(), AINetTestSuite.getNetName());
            LOGGER.debug("Using network of type: " + netInstance.getNetType());
        } catch (NetConfigurationException e) {
            e.printStackTrace();
            fail("SetUp failed. Error: " + e.getMessage()
                    + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
    }
    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            AINetTestSuite.networkTearDown(netInstance);
        } catch (Exception e) {
            fail("cleanUp failed exception: " + e.getMessage()
                    + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
    }

    /**
     * Tests all node functions like adding and selecting nodes.
     * Is called from the testInteraction method.
     */
    public void testNodeFunctions() {
        LOGGER.debug("******Start test node functions*****");
        try {
            assertEquals(netInstance.nodeExists(""), false);
            assertEquals(netInstance.nodeExists(null), false);
            final AssociationNode expected = 
                netInstance.createNode(m_node1Name);
            assertEquals(netInstance.nodeExists(m_node1Name), true);
            AssociationNode actual = netInstance.createNode(m_node1Name);
            //they must be the same object
            assertEquals(expected, actual);
            
            //test the different get functions
            actual = netInstance.getNodeByID(expected.getId());
            assertEquals(expected, actual);
            actual = netInstance.getNodeByName(m_node1Name);
            assertEquals(expected, actual);
        } catch (PersistenceException e) {
            fail("Exception thrown: " + e.getMessage());
        }
        LOGGER.debug("******End test node functions*****");
    }
}
