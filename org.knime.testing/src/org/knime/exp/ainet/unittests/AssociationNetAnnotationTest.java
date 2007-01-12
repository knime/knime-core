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
package org.knime.exp.ainet.unittests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.exp.ainet.agents.geneontology.GOTerm;
import org.knime.exp.ainet.core.AnnotationType;
import org.knime.exp.ainet.core.AssociationLink;
import org.knime.exp.ainet.core.AssociationNet;
import org.knime.exp.ainet.core.AssociationNode;
import org.knime.exp.ainet.core.LinkAnnotation;
import org.knime.exp.ainet.core.NodeAnnotation;
import org.knime.exp.ainet.core.exceptions.NetConfigurationException;
import org.knime.exp.ainet.core.netimpl.AssociationNetFactory;
import org.knime.exp.ainet.data.document.DocumentAuthorFactory;
import org.knime.exp.ainet.data.document.DocumentFactory;
import org.knime.exp.ainet.data.document.TermFactory;
import org.knime.exp.ainet.data.geneontology.GOEntry;
import org.knime.exp.ainet.data.geneontology.GOEntryFactory;
import org.knime.exp.ainet.data.genesubgroup.GeneExprSubgroupEntry;
import org.knime.exp.ainet.data.hibernateresources.AnnotHibernateUtil;
import org.knime.exp.textmining.data.Document;
import org.knime.exp.textmining.data.DocumentAuthor;
import org.knime.exp.textmining.data.DocumentType;
import org.knime.exp.textmining.data.PartOfSpeechTag;
import org.knime.exp.textmining.data.PublicationDate;

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
    
//    private final double m_link1Weight = 0.9;
    
    private final AnnotationType m_anno1Type = AnnotationType.GO_TERM;
    
    private final AnnotationType m_anno2Type = 
        AnnotationType.GENE_EXPRESSION_SUBGROUP;
    
    private final AnnotationType m_anno3Type = AnnotationType.TERM_DOCUMENT;
    
    private final AnnotationType m_anno4Type = AnnotationType.SYNONYM;
    
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
    //test the go entry annotation        
            final Set<String> termParents = new HashSet<String>(2);
            termParents.add("GO:0002506");
            termParents.add("GO:0004806");
            final GOTerm term1 = new GOTerm("GO:0001709", "GO name 1", "C", 
                    "This could be a comment", termParents);
            final GOEntry goEntry1 = GOEntryFactory.create(term1);
            AnnotHibernateUtil.saveObject(goEntry1);
            termParents.add("GO:0002506");
            termParents.add("GO:0001709");
            final GOTerm term = new GOTerm("GO:0004806", "GO name 2", "P", 
                    "This is a comment", termParents);
            final GOEntry goEntry2 = GOEntryFactory.create(term);
            AnnotHibernateUtil.saveObject(goEntry2);
            NodeAnnotation expectedNodeAnnot = 
                node1.addAnnotation(m_anno1Type, goEntry1.getSourceID());
            expectedNodeAnnot.addSourceID(goEntry2.getSourceID());
            //expected.addAnnotationEntry(goEntry);
            NodeAnnotation foundNodeAnnotation = 
                node1.getAnnotation(m_anno1Type);
            assertEquals(expectedNodeAnnot, foundNodeAnnotation);
    
            //node2
            final AssociationNode node2 = 
                netInstance.createNode(m_node1Name);
    //test the gene expression sub group annotation        
            final GeneExprSubgroupEntry geneEntry = new GeneExprSubgroupEntry();
            geneEntry.setOrganism("GeneExprSubgroupEntry.CFG_ORGANISM");
            geneEntry.setPlatform("GeneExprSubgroupEntry.CFG_PLATFORM");
            geneEntry.setThresholdType(
                    GeneExprSubgroupEntry.ThresholdType.CHANGE_P_VALUE);
            geneEntry.setOverExprVal(0.9);
            geneEntry.setUnderExprVal(0.1);
            geneEntry.setSupport(0.8);
            geneEntry.setRemarks("remarks");
            AnnotHibernateUtil.saveObject(geneEntry);
            expectedNodeAnnot = 
                node2.addAnnotation(m_anno2Type, geneEntry.getSourceID());
            foundNodeAnnotation = node2.getAnnotation(m_anno2Type);
            assertEquals(expectedNodeAnnot, foundNodeAnnotation);
//test link annotation using node1 and node2
            final AssociationLink link = 
                netInstance.createLink(node1, node2);
//test the term annotation            
            final String file = "junitTestFile";
            final Set<DocumentAuthor> authors = 
                new HashSet<DocumentAuthor>(2);
            authors.add(
                    DocumentAuthorFactory.create("junitLast1", "junitFirst1"));
            authors.add(
                    DocumentAuthorFactory.create("junitLast2", "junitFirst2"));
            final String fullText = "In der einfachen Suche können Sie in die";
            final PublicationDate date = new PublicationDate();
            final String title = "Junit document title";
            final Document testDoc = DocumentFactory.create(title,fullText, 
                    fullText,file, authors, date, DocumentType.UNKNOWN, null, 
                    null);
            final Collection<String> docIds = new ArrayList<String>(1);
            docIds.add(testDoc.getTitle());
            final LinkAnnotation expectedLinkAnnotation = link.addAnnotation(
                    testDoc.getTitle(), m_anno3Type, 1.0, docIds);
            LinkAnnotation foundAnnotation = link.getAnnotation(m_anno3Type);
            assertEquals(expectedLinkAnnotation, foundAnnotation);
            //test the document term
            TermFactory.create("testTerm", PartOfSpeechTag.MD);
//            AnnotHibernateUtil.removeObject(docTerm);
//test a synonym annotation with no sourceID
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
