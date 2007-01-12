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
 *    07.11.2006 (Tobias Koetter): created
 */
package org.knime.exp.ainet.unittests;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.exp.ainet.agents.geneontology.GOTerm;
import org.knime.exp.ainet.core.AnnotationType;
import org.knime.exp.ainet.core.AssociationLink;
import org.knime.exp.ainet.core.AssociationNet;
import org.knime.exp.ainet.core.AssociationNetType;
import org.knime.exp.ainet.core.AssociationNetUtil;
import org.knime.exp.ainet.core.AssociationNode;
import org.knime.exp.ainet.core.XMLPersistenceManager;
import org.knime.exp.ainet.core.exceptions.NetConfigurationException;
import org.knime.exp.ainet.core.netimpl.AssociationNetFactory;
import org.knime.exp.ainet.data.geneontology.GOEntry;
import org.knime.exp.ainet.data.geneontology.GOEntryFactory;
import org.knime.exp.ainet.data.genesubgroup.GeneExprSubgroupEntry;

/**
 * Tests the {@link XMLPersistenceManager} and {@link XMLPersistenceParser} if
 * they work properly.
 * Creates a network with some links, writes it into a file and reads the same
 * file to compare both networks if they are the same.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class XMLPersistenceManagerTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger
        .getLogger(XMLPersistenceManagerTest.class);
    
    private static final int NO_OF_LINKS = 50;
    
    private static final String UNIT_NODE_PREFIX = "UNIT_TEST@";
    
    private static final double LINK_WEIGHT = 0.9;
    
    private static final AnnotationType ANNOTATION_TYPE = 
        AnnotationType.GENE_EXPRESSION_SUBGROUP;
    
    private static final AssociationNetType NET_TYPE = 
        AssociationNetType.MEMORY_NET;
    
    private AssociationNet m_expectedNet;
    private File m_tempFile;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_expectedNet = createTestNet(NO_OF_LINKS, NET_TYPE);
        m_tempFile = File.createTempFile("junitTestFile", ".aiNet");
        final XMLPersistenceManager expectedManager = 
            new XMLPersistenceManager(m_expectedNet, 
                m_tempFile.getAbsolutePath());
        expectedManager.save(null);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_tempFile != null) {
            m_tempFile.delete();
        }
    }

//    /**
//     * Test method for {@link org.knime.exp.ainet.core.XMLPersistenceManager#
//     * save(org.knime.core.node.ExecutionMonitor)}.
//     */
//    public final void testSave() {
//        try {
//            final XMLPersistenceManager expectedManager = 
//                new XMLPersistenceManager(m_expectedNet, 
//                    m_tempFile.getAbsolutePath());
//            expectedManager.save(null);
//        } catch (PersistenceException e) {
//           fail(e.getMessage());
//        }
//    }

    /**
     * Test method for {@link org.knime.exp.ainet.core.XMLPersistenceManager#
     * load(org.knime.core.node.ExecutionMonitor)}.
     */
    public final void testLoad() {
        try {
            final AssociationNet actualNet = 
                AssociationNetFactory.createNet(NET_TYPE, "test");
            XMLPersistenceManager.loadNet(m_tempFile, actualNet, null);
//            final XMLPersistenceManager manager = 
//                new XMLPersistenceManager(actualNet, 
//                    m_tempFile.getAbsolutePath());
//            manager.load(null);
            assertEquals(true, 
                    AssociationNetUtil.compare(m_expectedNet, actualNet));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private static AssociationNet createTestNet(final int noOfLinks, 
            final AssociationNetType netType) throws NetConfigurationException {
        final int noOfNodes = 2 * noOfLinks;
        final AssociationNet netInstance = 
            AssociationNetFactory.createNet(netType, "test");
        LOGGER.debug("Start to create " + noOfNodes + " nodes...");
        //the go term used for all nodes
        final Set<String> termParents = new HashSet<String>(2);
        termParents.add("GO:0002506");
        termParents.add("GO:0004806");
        final GOTerm term = new GOTerm("GO:0001709", "GO name 2", "B", 
                "This could be a comment", termParents);
        final GOEntry goEntry = GOEntryFactory.create(term);
        for (int i = 1; i <= noOfNodes; i++) {
            try {
                final AssociationNode node = 
                    netInstance.createNode(UNIT_NODE_PREFIX + i);
                //use the parents to add more then one annotation source id
                node.addAnnotation(AnnotationType.GO_TERM, 
                        goEntry.getSourceID());
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
        //the entry to use for all links
        final GeneExprSubgroupEntry geneEntry = 
            new GeneExprSubgroupEntry();
        geneEntry.setOrganism("GeneExprSubgroupEntry.CFG_ORGANISM");
        geneEntry.setPlatform("GeneExprSubgroupEntry.CFG_PLATFORM");
        geneEntry.setThresholdType(
                GeneExprSubgroupEntry.ThresholdType.CHANGE_P_VALUE);
        geneEntry.setOverExprVal(0.9);
        geneEntry.setUnderExprVal(0.1);
        geneEntry.setSupport(0.8);
        geneEntry.setRemarks("remarks");
        LOGGER.debug("Start to create " + noOfLinks + " links...");
        for (int i = 1; i <= noOfLinks; i++) {
            final AssociationNode node1 = netInstance.getNodeByName(
                    UNIT_NODE_PREFIX + i);
            final AssociationNode node2 = netInstance.getNodeByName(
                    UNIT_NODE_PREFIX + (2 * i));
            try {
                final AssociationLink link = netInstance.createLink(node1, 
                        node2);
                link.addAnnotation("Loop: " + i, ANNOTATION_TYPE, 
                        geneEntry.getSourceID(), 
                        LINK_WEIGHT);
                link.addAnnotation(null, AnnotationType.GO_TERM, 
                        goEntry.getSourceID(), 0.5);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
        return netInstance;
    }
}
