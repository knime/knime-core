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

import java.util.Collection;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.core.AnnotationType;
import org.knime.ext.ainet.core.AssociationLink;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNode;
import org.knime.ext.ainet.core.exceptions.NetConfigurationException;
import org.knime.ext.ainet.core.netimpl.AssociationNetFactory;

/**
 * Tests the creation and getting of links.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AssociationNetLinkTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AssociationNetLinkTest.class);
    
//test values
    private static final String UNIT_NODE_PREFIX = "UNIT_TEST@";
    
    private final String m_node1Name = UNIT_NODE_PREFIX + "Node1";
    
    private final String m_node2Name = UNIT_NODE_PREFIX + "Node2";
    
    private final AnnotationType m_annot1Type = AnnotationType.GO_TERM;
    private final double m_link1Weight = 0.9;

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
    public void testLinkFunctions() {
        LOGGER.debug("******Start test link functions*****");
        try {
            final AssociationNode node1 = netInstance.createNode(m_node1Name);
            final AssociationNode node2 = netInstance.createNode(m_node2Name);
            final AssociationLink expected = netInstance.createLink(node1, 
                    node2);
            expected.addAnnotation(null, m_annot1Type, null, m_link1Weight);
            AssociationLink actual = netInstance.createLink(node1, node2);
            actual.addAnnotation(null, m_annot1Type, null, m_link1Weight);
            assertEquals(expected, actual);
            actual = netInstance.getLink(node1, node2);
            assertEquals(expected, actual);
            
            final Collection<? extends AssociationLink> outgoingLinksForNode = 
                netInstance.getOutgoingLinksForNode(node1);
            boolean linkFound = false;
            for (AssociationLink link : outgoingLinksForNode) {
                if (link.equals(expected)) {
                    linkFound = true;
                    break;
                }
            }
            if (!linkFound) {
                fail("Outgoinglink not found"
                        + "\nNetwork type: " 
                        + netInstance.getNetType().toString());
            }
        } catch (Exception e) {
            fail("Link failed: " + e.getMessage()
                    + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
        LOGGER.debug("******End test link functions*****");
    }
}
