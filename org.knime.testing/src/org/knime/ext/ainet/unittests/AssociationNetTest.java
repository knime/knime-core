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
package org.knime.ext.ainet.unittests;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.core.AssociationLink;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNetType;
import org.knime.ext.ainet.core.AssociationNode;
import org.knime.ext.ainet.core.netimpl.AssociationNetFactory;
import org.knime.ext.ainet.core.netimpl.DBAssociationNet;

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

    private static AssociationNode m_node1;
    
    private final String m_node2Name = UNIT_NODE_PREFIX + "Node2";
    
    private final String m_node3Name = UNIT_NODE_PREFIX + "Node3";
    
    private static AssociationNode m_node2;

    private static AssociationLink m_link1;
    
    private static AssociationNet netInstance;
    
    private static boolean isSetup = false;
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (isSetup) {
            return;
        }
        try {
            netInstance =  AssociationNetFactory.createNet(NET_TYPE, 
                    AINetTestSuite.getNetName());
            if (netInstance instanceof DBAssociationNet) {
                DBAssociationNet dbNet = (DBAssociationNet) netInstance;
                dbNet.deleteNet();
            }
            netInstance =  AssociationNetFactory.createNet(NET_TYPE, 
                    AINetTestSuite.getNetName());
            m_node1 = netInstance.createNode(m_node1Name);
            m_node2 = netInstance.createNode(m_node2Name);
            m_link1 = netInstance.createLink(m_node1, m_node2);
            isSetup = true;
            LOGGER.debug("Using network of type: " + netInstance.getNetType());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Constructor failed. Error: " + e.getMessage()
                    + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
    }
    
    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * nodeExists(java.lang.String)}.
     */
    public final void testNodeExists() {
        LOGGER.info("Entering testNodeExists() of class AssociationNetTest.");
        assertFalse(netInstance.nodeExists(""));
        assertFalse(netInstance.nodeExists(null));
        assertTrue(netInstance.nodeExists(m_node1Name));
        LOGGER.info("Exiting testNodeExists() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * createNode(java.lang.String, 
     * org.knime.ext.ainet.core.AssociationNodeType)}.
     */
    public final void testCreateNode() {
        LOGGER.info("Entering testCreateNode() of class AssociationNetTest.");
        //try to create an existing node
        try {
            final AssociationNode expected = 
                netInstance.createNode(m_node2Name);
            assertEquals(expected, m_node2);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //create a new node
        try {
            final AssociationNode expected = 
                netInstance.createNode(m_node3Name);
            assertNotNull(expected);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //create a node with name null
        try {
            netInstance.createNode(null);
            fail("Node name null should create exception");
        } catch (final Exception e) {
            //nothing to do
        }
        LOGGER.info("Exiting testCreateNode() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * createLink(org.knime.ext.ainet.core.AssociationNode, 
     * org.knime.ext.ainet.core.AssociationNode, 
     * org.knime.ext.ainet.core.AssociationLinkType)}.
     */
    public final void testCreateLink() {
        LOGGER.info("Entering testCreateLink() of class AssociationNetTest.");
        //create two different links between the same nodes one exists already
        //and between the same link but other direction
        try {
            AssociationLink link = netInstance.createLink(m_node1, m_node2);
            assertNotNull(link);
            AssociationLink link2 = netInstance.createLink(m_node2, m_node1);
            assertNotNull(link2);
            assertTrue(!link.equals(link2));
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
        LOGGER.info("Exiting testCreateLink() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNodes()}.
     */
    public final void testGetNodes() {
        LOGGER.info("Entering testGetNodes() of class AssociationNetTest.");
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
        LOGGER.info("Exiting testGetNodes() of class AssociationNetTest.");
    }
    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getLinks()}.
     */
    public final void testGetLinks() {
        LOGGER.info("Entering testGetLinks() of class AssociationNetTest.");
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
        LOGGER.info("Exiting testGetLinks() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNodeByName(java.lang.String)}.
     */
    public final void testGetNodeByName() {
        LOGGER.info(
                "Entering testGetNodeByName() of class AssociationNetTest.");
        AssociationNode nodeByName = 
            netInstance.getNodeByName(m_node1.getName());
        assertEquals(nodeByName, m_node1);
        assertNull(netInstance.getNodeByName(null));
        assertNull(netInstance.getNodeByName(""));
        LOGGER.info(
                "Exiting testGetNodeByName() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNodeByID(long)}.
     */
    public final void testGetNodeByID() {
        LOGGER.info("Entering testGetNodeByID() of class AssociationNetTest.");
        AssociationNode nodeByID = netInstance.getNodeByID(m_node1.getId());
        assertEquals(nodeByID, m_node1);
        assertNull(netInstance.getNodeByID(-1));
        LOGGER.info("Exiting testGetNodeByID() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNodeByUnderOrOverExprName(java.lang.String)}.
     */
    public final void testGetNodeByUnderOrOverExprName() {
//        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getOutgoingLinksForNode(org.knime.ext.ainet.core.AssociationNode)}.
     */
    public final void testGetOutgoingLinksForNode() {
        LOGGER.info("Entering testGetOutgoingLinksForNode() " 
                + "of class AssociationNetTest.");
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
        LOGGER.info("Exiting testGetOutgoingLinksForNode() of " 
                + "class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getLink(org.knime.ext.ainet.core.AssociationNode, 
     * org.knime.ext.ainet.core.AssociationNode, 
     * org.knime.ext.ainet.core.AssociationLinkType)}.
     */
    public final void testGetLink() {
        LOGGER.info("Entering testGetLink() of class AssociationNetTest.");
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
        LOGGER.info("Exiting testGetLink() of class AssociationNetTest.");
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNoOfNodes()}.
     */
    public final void testGetNoOfNodes() {
        LOGGER.info("Entering testGetNoOfNodes() of class AssociationNetTest.");
        netInstance.getNoOfNodes();
        LOGGER.info("Exiting testGetNoOfNodes() of class AssociationNetTest.");
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
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getNoOfLinks()}.
     */
    public final void testGetNoOfLinks() {
        LOGGER.info("Entering testGetNoOfLinks() of class AssociationNetTest.");
        netInstance.getNoOfLinks();
        LOGGER.info("Exiting testGetNoOfLinks() of class AssociationNetTest.");
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
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * removeNode(org.knime.ext.ainet.core.AssociationNode)}.
     */
    public final void testRemoveNode() {
        LOGGER.info("Entering testRemoveNode() of class AssociationNetTest.");
        try {
            AssociationNode node = 
                netInstance.createNode(UNIT_NODE_PREFIX + "removeTest");
            assertTrue(netInstance.removeNode(node));
            assertFalse(netInstance.removeNode(node));
            LOGGER.info("Exiting testRemoveNode() of class AssociationNetTest.");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * removeLink(org.knime.ext.ainet.core.AssociationLink)}.
     */
    public final void testRemoveLink() {
        LOGGER.info("Entering testRemoveLink() of class AssociationNetTest.");
        try {
            AssociationNode node = 
                netInstance.createNode(UNIT_NODE_PREFIX + "removeLinkTest");
            AssociationLink link = netInstance.createLink(node, m_node2);
            assertTrue(netInstance.removeLink(link));
            assertFalse(netInstance.removeLink(link));
            LOGGER.info(
                    "Exiting testRemoveLink() of class AssociationNetTest.");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.knime.ext.ainet.core.AssociationNet#
     * getRandomNode()}.
     */
    public final void testGetRandomNode() {
        LOGGER.info(
                "Entering testGetRandomNode() of class AssociationNetTest.");
        assertNotNull(netInstance.getRandomNode());
        LOGGER.info(
                "Exiting testGetRandomNode() of class AssociationNetTest.");
    }
}
