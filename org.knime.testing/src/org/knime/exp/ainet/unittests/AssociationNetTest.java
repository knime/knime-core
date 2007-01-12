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
 *    17.11.2006 (Tobias Koetter): created
 */
package org.knime.exp.ainet.unittests;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.exp.ainet.core.AssociationLink;
import org.knime.exp.ainet.core.AssociationNet;
import org.knime.exp.ainet.core.AssociationNetType;
import org.knime.exp.ainet.core.AssociationNode;
import org.knime.exp.ainet.core.exceptions.NetConfigurationException;
import org.knime.exp.ainet.core.netimpl.AssociationNetFactory;

/**
 * Tests all functions of the association network. The type of the network to
 * test is defined by the NET_TYPE variable.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AssociationNetTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AssociationNetTest.class);
    
    /**Defines the network type for testing.*/
    private static final AssociationNetType NET_TYPE =
        AssociationNetType.DB_NET;
    
//test values
    private static final String UNIT_NODE_PREFIX = "UNIT_TEST@";
    
    private final String m_node1Name = UNIT_NODE_PREFIX + "Node1";

    private AssociationNode m_node1;
    
    private final String m_node2Name = UNIT_NODE_PREFIX + "Node2";
    
    private AssociationNode m_node2;

    private AssociationLink m_link1;
    
    private static AssociationNet netInstance;
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            netInstance =  AssociationNetFactory.createNet(NET_TYPE, 
                    AINetTestSuite.getNetName());
            LOGGER.debug("Using network of type: " + netInstance.getNetType());
        } catch (NetConfigurationException e) {
            e.printStackTrace();
            fail("Constructor failed. Error: " + e.getMessage()
                    + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
        m_node1 = netInstance.createNode(m_node1Name);
        m_node2 = netInstance.createNode(m_node2Name);
        m_link1 = netInstance.createLink(m_node1, m_node2);
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
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * nodeExists(java.lang.String)}.
     */
    public final void testNodeExists() {
        assertFalse(netInstance.nodeExists(""));
        assertFalse(netInstance.nodeExists(null));
        assertTrue(netInstance.nodeExists(m_node1Name));
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * createNode(java.lang.String, 
     * org.knime.exp.ainet.core.AssociationNodeType)}.
     */
    public final void testCreateNode() {
        try {
            final AssociationNode expected = 
                netInstance.createNode(m_node2Name);
            assertNotNull(expected);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            netInstance.createNode(null);
            fail("Node name null should create exception");
        } catch (final Exception e) {
            //nothing to do
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * createLink(org.knime.exp.ainet.core.AssociationNode, 
     * org.knime.exp.ainet.core.AssociationNode, 
     * org.knime.exp.ainet.core.AssociationLinkType)}.
     */
    public final void testCreateLink() {
        //create two different links between the same nodes one exists already
        //and between the same link but other direction
        try {
            netInstance.createLink(m_node1, m_node2);
            netInstance.createLink(m_node1, m_node2);
            //test same type but other direction
            netInstance.createLink(m_node2, m_node1);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //test if the link already exists
        try {
            final AssociationLink link1Copy = netInstance.createLink(m_node1, 
                    m_node2);
            assertEquals(m_link1, link1Copy);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //test the different null arguments
        try {
            netInstance.createLink(null, m_node2);
            fail("Node1 null should throw exception");
        } catch (Exception e) {
            //nothing to do
        }
        try {
            netInstance.createLink(m_node2, null);
            fail("Node2 null should throw exception");
        } catch (Exception e) {
            //nothing to do
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNodes()}.
     */
    public final void testGetNodes() {
        final Iterator<? extends AssociationNode> nodes = 
            netInstance.getNodes();
        boolean found = false;
        long nodeCounter = 0;
        while (nodes.hasNext()) {
            final AssociationNode node = nodes.next();
            nodeCounter++;
            if (node.equals(m_node1)) {
                found = true;
            }
        }
        if (!found) {
            fail("At least the one node we added in setUp should be present");
        }
        long noOfNodes = netInstance.getNoOfNodes();
        if (noOfNodes != nodeCounter) {
            fail("Either no of nodes or getNodes wrong implemented");
        }
    }
    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getLinks()}.
     */
    public final void testGetLinks() {
        final Iterator<? extends AssociationLink> links = 
            netInstance.getLinks();
        boolean found = false;
        long linkCounter = 0;
        while (links.hasNext()) {
            final AssociationLink link = links.next();
            linkCounter++;
            if (link.equals(m_link1)) {
                found = true;
            }
        }
        if (!found) {
            fail("At least the one link we added in setUp should be present");
        }
        long noOfLinks = netInstance.getNoOfLinks();
        if (noOfLinks != linkCounter) {
            fail("Either noOfLinks or getLinks is wrong implemented");
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNodeByName(java.lang.String)}.
     */
    public final void testGetNodeByName() {
        AssociationNode nodeByName = netInstance.getNodeByName(m_node1Name);
        assertEquals(nodeByName, m_node1);
        assertNull(netInstance.getNodeByName(null));
        assertNull(netInstance.getNodeByName(""));
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNodeByID(long)}.
     */
    public final void testGetNodeByID() {
        AssociationNode nodeByID = netInstance.getNodeByID(m_node1.getId());
        assertEquals(nodeByID, m_node1);
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNodeByUnderOrOverExprName(java.lang.String)}.
     */
    public final void testGetNodeByUnderOrOverExprName() {
//        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getOutgoingLinksForNode(org.knime.exp.ainet.core.AssociationNode)}.
     */
    public final void testGetOutgoingLinksForNode() {
        Collection<? extends AssociationLink> outgoingLinksForNode = 
            netInstance.getOutgoingLinksForNode(m_node1);
        boolean found = false;
        for (AssociationLink outLink : outgoingLinksForNode) {
            if (outLink.equals(m_link1)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail("At least the one link we added in setUp should be present");
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getLink(org.knime.exp.ainet.core.AssociationNode, 
     * org.knime.exp.ainet.core.AssociationNode, 
     * org.knime.exp.ainet.core.AssociationLinkType)}.
     */
    public final void testGetLink() {
        AssociationLink link = 
            netInstance.getLink(m_node1, m_node2);
        assertEquals(link, m_link1);
        try {
            netInstance.getLink(null, m_node2);
            fail("Node1 = null should fail");
        } catch (IllegalArgumentException e) {
            //nothing to do
        }
        try {
            netInstance.getLink(m_node1, null);
            fail("Node2 = null should fail");
        } catch (IllegalArgumentException e) {
            //nothing to do
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNoOfNodes()}.
     */
    public final void testGetNoOfNodes() {
        netInstance.getNoOfNodes();
//        try {
//            AssociationNode node = netInstance.createNode(
//                    UNIT_NODE_PREFIX + "noOfNodesTest", 
//                    AssociationNodeType.UNKNOWN);
//            long after = netInstance.getNoOfNodes();
//            assertTrue(after - 1 == before);
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getNoOfLinks()}.
     */
    public final void testGetNoOfLinks() {
//        long before = netInstance.getNoOfLinks();
//        try {
//            AssociationNode node = netInstance.createNode("noOflinks");
//            netInstance.createLink(node, m_node2);
//            long after = netInstance.getNoOfLinks();
//            assertTrue(after - 1 == before);
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * removeNode(org.knime.exp.ainet.core.AssociationNode)}.
     */
    public final void testRemoveNode() {
        try {
            AssociationNode node = 
                netInstance.createNode(UNIT_NODE_PREFIX + "removeTest");
            assertTrue(netInstance.removeNode(node));
            assertFalse(netInstance.removeNode(node));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * removeLink(org.knime.exp.ainet.core.AssociationLink)}.
     */
    public final void testRemoveLink() {
        try {
            AssociationNode node = 
                netInstance.createNode(UNIT_NODE_PREFIX + "removeLinkTest");
            AssociationLink link = netInstance.createLink(node, m_node2);
            assertTrue(netInstance.removeLink(link));
            assertFalse(netInstance.removeLink(link));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.AssociationNet#
     * getRandomNode()}.
     */
    public final void testGetRandomNode() {
        assertNotNull(netInstance.getRandomNode());
    }
}
