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

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.core.AnnotationType;
import org.knime.ext.ainet.core.AssociationLink;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNode;
import org.knime.ext.ainet.core.LinkAnnotation;
import org.knime.ext.ainet.core.NodeAnnotation;
import org.knime.ext.ainet.core.netimpl.AssociationNetFactory;
import org.knime.ext.ainet.core.netimpl.DBAssociationNet;

/**
 * Tests if the network handles the link and node annotations correct.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AssociationNetAnnotationTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AssociationNetAnnotationTest.class);
//test values
    private final String m_node1Name = 
        AINetTestSuite.UNIT_NODE_PREFIX + "Node1";

    private final String m_node2Name = 
        AINetTestSuite.UNIT_NODE_PREFIX + "Node2";
    
//    private final double m_link1Weight = 0.9;
    
    private final AnnotationType m_anno1Type = AnnotationType.GO_TERM;
    
    private final AnnotationType m_anno2Type = 
        AnnotationType.GENE_EXPRESSION_SUBGROUP;
    
    private final AnnotationType m_anno3Type = AnnotationType.TERM_DOCUMENT;
    
    private final AnnotationType m_anno4Type = AnnotationType.SYNONYM;
    
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
            netInstance =  AssociationNetFactory.createNet(
                    AINetTestSuite.getNetType(), AINetTestSuite.getNetName());
            if (netInstance instanceof DBAssociationNet) {
                DBAssociationNet dbNet = (DBAssociationNet) netInstance;
                dbNet.deleteNet();
            }
            netInstance =  AssociationNetFactory.createNet(
                    AINetTestSuite.getNetType(), AINetTestSuite.getNetName());
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
     * Tests all annotation functions.
     * Is called from the testInteraction method.
     */
    public void testAnnotationFunctions() {
        LOGGER.debug("******Start test annotation functions*****");
//test node annotation
        //node1
        try {
            final AssociationNode node1 = 
                netInstance.createNode(m_node1Name);
            LOGGER.debug("Adding annotation");
            NodeAnnotation expectedNodeAnnot = 
                node1.addAnnotation(m_anno1Type, "Test annotation source id");
            LOGGER.debug("Annotation added");
            
            LOGGER.debug("Adding annotation source id");
            expectedNodeAnnot.addSourceID("Test annotation source id2");
            LOGGER.debug("SourceID added");
            LOGGER.debug("Retrieving annotation source id");
            expectedNodeAnnot.getSourceIds().size();
            LOGGER.debug("SourceID retrieved");
            LOGGER.debug("Removing annotation source id");
            expectedNodeAnnot.removeSourceID("Test annotation source id2");
            LOGGER.debug("SourceID removed");
            
            //expected.addAnnotationEntry(goEntry);
            NodeAnnotation foundNodeAnnotation = 
                node1.getAnnotation(m_anno1Type);
            assertEquals(expectedNodeAnnot, foundNodeAnnotation);
    
            //node2
            final AssociationNode node2 = 
                netInstance.createNode(m_node2Name);
            expectedNodeAnnot = 
                node2.addAnnotation(m_anno2Type, "Test annotation source id3");
            foundNodeAnnotation = node2.getAnnotation(m_anno2Type);
            assertEquals(expectedNodeAnnot, foundNodeAnnotation);
//test link annotation using node1 and node2
            Collection<String> sourceIDs = new ArrayList<String>(3);
            sourceIDs.add("Test annotation sourceIDs1");
            sourceIDs.add("Test annotation sourceIDs2");
            sourceIDs.add("Test annotation sourceIDs3");
            final AssociationLink link = 
                netInstance.createLink(node1, node2);
            LOGGER.debug("Adding source ids");
            final LinkAnnotation expectedLinkAnnotation = link.addAnnotation(
                    "Link annotation label", m_anno3Type, 1.0, sourceIDs);
            LOGGER.debug("Source ids added");
            
            LinkAnnotation foundAnnotation = link.getAnnotation(m_anno3Type);
            assertEquals(expectedLinkAnnotation, foundAnnotation);
            
            LOGGER.debug("Removing all source ids");
            expectedLinkAnnotation.removeSourceIDs(sourceIDs);
            LOGGER.debug("All source ids removed");
            
            LOGGER.debug("Removing all but one source ids");
            LOGGER.debug("\tAdding source ids");
            expectedLinkAnnotation.addSourceIDs(sourceIDs);
            LOGGER.debug("\tSource ids added");
            LOGGER.debug("Retrieving annotation source ids");
            expectedLinkAnnotation.getSourceIds().size();
            LOGGER.debug("SourceIDs retrieved");
            LOGGER.debug("\tAdding Not2remove source id ");
            expectedLinkAnnotation.addSourceID("not removed source id");
            LOGGER.debug("\tNot2remove source id added");
            expectedLinkAnnotation.removeSourceIDs(sourceIDs);
            LOGGER.debug("All source ids but one removed");
            
            final LinkAnnotation expectedLinkAnnotation1 = 
                link.addAnnotation(null, m_anno4Type, null, 1.0);
            foundAnnotation = link.getAnnotation(m_anno4Type);
            assertEquals(expectedLinkAnnotation1, foundAnnotation);
//test getting the right annotations from the right object
//all following methods should return another annotation than expected!
//            expectedNodeAnnot = node1.getAnnotation(m_anno1Type);
//            foundAnnotation = link.getAnnotation(m_anno3Type);
//            assertEquals(false, expectedNodeAnnot.equals(foundAnnotation));
            
            expectedNodeAnnot = node2.getAnnotation(m_anno2Type);
            foundAnnotation = link.getAnnotation(m_anno2Type);
            assertEquals(false, expectedNodeAnnot.equals(foundAnnotation));
            
            expectedNodeAnnot = node1.getAnnotation(m_anno1Type);
            foundNodeAnnotation = node2.getAnnotation(m_anno2Type);
            assertEquals(false, expectedNodeAnnot.equals(foundNodeAnnotation));
            
        } catch (Exception e) {
            fail("Test failed: " 
                    + e.getMessage() + "\nNetwork type: " 
                    + netInstance.getNetType().toString());
        }
        LOGGER.debug("******End test annotation functions*****");
    }
}
