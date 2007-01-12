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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.exp.ainet.agents.geneontology.GOTerm;
import org.knime.exp.ainet.core.AnnotationType;
import org.knime.exp.ainet.core.AssociationLink;
import org.knime.exp.ainet.core.AssociationNet;
import org.knime.exp.ainet.core.AssociationNode;
import org.knime.exp.ainet.core.exceptions.NetConfigurationException;
import org.knime.exp.ainet.core.netimpl.AssociationNetFactory;
import org.knime.exp.ainet.data.geneontology.GOEntry;
import org.knime.exp.ainet.data.geneontology.GOEntryFactory;

/**
 * Test the performance of the network by adding the specified no of
 * links and nodes to the network. It can also perform an memory test.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AssociationNetPerformanceTest extends TestCase {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AssociationNetPerformanceTest.class);
    /**
     * Runtime.
     */
    public static final Runtime RUNTIME = Runtime.getRuntime();
    
    private static int noOfLinks = 50;
    
    private static boolean memoryTest = false;
//test values
    
    private final AnnotationType m_annoTypeGO = AnnotationType.GO_TERM;
    
    private final AnnotationType m_annoTypeGeneExpr = 
        AnnotationType.GENE_EXPRESSION_SUBGROUP;
    
    private static AssociationNet netInstance;
    
    /**
     * @param memoryTest set to <code>true</code> if you want memory usage data
     */
    protected static void setMemoryTest(final boolean memoryTest) {
        AssociationNetPerformanceTest.memoryTest = memoryTest;
    }

    /**
     * @param noOfLinks defines the number of links to create which implies
     * that 2 time noOfLinks nodes will be created as well
     */
    protected static void setNoOfLinks(final int noOfLinks) {
        AssociationNetPerformanceTest.noOfLinks = noOfLinks;
    }
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
     * Adds several nodes and links into the network to test the performance.
     * After adding a node or a link it also retrieve it again to test the 
     * select performance as well.
     */
    public void testPerformance() {
        long heap1 = 0;
        final int noOfNodes = 2 * noOfLinks;
        if (memoryTest) {
//      Warm up all classes/methods we will use
            runGC();
            usedMemory();
            runGC();
            heap1 = usedMemory(); // Take a before heap snapshot
        }
        try {
            LOGGER.info("Start to create " + noOfNodes + " nodes...");
            final long nodeStart = System.currentTimeMillis();
            for (int i = 1; i <= noOfNodes; i++) {
                AssociationNode node = null;
                //create a node with a GO entry
                node = netInstance.createNode(
                        AINetTestSuite.UNIT_NODE_PREFIX + i);
                final Set<String> termParents = new HashSet<String>(2);
                termParents.add("GO:0002506");
                termParents.add("GO:0004806");
                final GOTerm term = 
                    new GOTerm("GO:0001709", "GO name " + i, 
                        "B", "This could be a comment", termParents);
                GOEntry goEntry = GOEntryFactory.create(term); 
                node.addAnnotation(m_annoTypeGO, goEntry.getSourceID());
            }
            final long nodeEnd = System.currentTimeMillis();
            //LOGGER.info("No of nodes: " + netInstance.getNoOfNodes());
            //the entry to use for the link
            LOGGER.info("Start to create " + noOfLinks + " links...");
            final long linkStart = System.currentTimeMillis();
            final double weightPerLoop = 1.0 / noOfLinks;
            for (int i = 1; i <= noOfLinks; i++) {
                final AssociationNode node1 = netInstance.getNodeByName(
                        AINetTestSuite.UNIT_NODE_PREFIX + i);
                final AssociationNode node2 = netInstance.getNodeByName(
                        AINetTestSuite.UNIT_NODE_PREFIX + (2 * i));
                final AssociationLink link = netInstance.createLink(node1, 
                        node2);
    //                final GeneExprSubgroupEntry geneEntry = 
    //                    new GeneExprSubgroupEntry();
    //        geneEntry.setOrganism("GeneExprSubgroupEntry.CFG_ORGANISM" + i);
    //        geneEntry.setPlatform("GeneExprSubgroupEntry.CFG_PLATFORM" + i);
    //        geneEntry.setThresholdType(
    //                     GeneExprSubgroupEntry.ThresholdType.CHANGE_P_VALUE);
    //             geneEntry.setOverExprVal(0.9);
    //             geneEntry.setUnderExprVal(0.1);
    //             geneEntry.setSupport(0.8);
    //             geneEntry.setRemarks("remarks");
               link.addAnnotation("Loop: " + i, m_annoTypeGeneExpr, null, 
                       weightPerLoop * i);
            }
            final long linkEnd = System.currentTimeMillis();
            final long nodeTime = nodeEnd - nodeStart;
            final long linkTime = linkEnd - linkStart;
            final long totalTime = nodeTime + linkTime;
            LOGGER.info("End of performanceTest (NetType: " 
                    + netInstance.getNetType().toString() + ") with:");
            LOGGER.info("\t No of nodes: " + noOfNodes);
            LOGGER.info("\t No of links: " + noOfLinks);
            LOGGER.info("\t Flag memoryTest: " + memoryTest);
            LOGGER.info("\tTime for node creation: " 
                    + nodeTime + "(millis.)");
            LOGGER.info("\tTime for link creation: " 
                    + linkTime + "(millis.)");
            LOGGER.info("\tTotal time: " 
                    + (totalTime / 1000) + "(sec) or " 
                    + (totalTime / 1000 / 60) + "(min)");
            
            if (memoryTest) {
                runGC();
                long heap2 = usedMemory(); // Take an after heap snapshot:
                final long usedMemory = heap2 - heap1;
                LOGGER.info("MEMORY TEST RESULT:");
                LOGGER.info("===================");
                LOGGER.info("\tNodes (" + noOfNodes + ")/Links(" 
                        + noOfLinks + "):");
                LOGGER.info("\t'before' heap: " + heap1
                        + ", 'after' heap: " + heap2);
                LOGGER.info("\theap delta: " 
                        + usedMemory / 1024 + " kBytes "
                        + usedMemory / 1024 / 1024 + " mBytes");
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //LOGGER.info("No of links: " + netInstance.getNoOfLinks());
    }
    
    private static long usedMemory() {
        return RUNTIME.totalMemory() - RUNTIME.freeMemory();
    }
    
    private static void runGC() {
        // It helps to call Runtime.gc()
        // using several method calls:
        for (int r = 0; r < 4; ++r) {
            rungc();
        }
    }

    private static void rungc() {
        long usedMem1 = usedMemory(), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
            RUNTIME.runFinalization();
            RUNTIME.gc();
            //Thread.currentThread().yield();
            Thread.yield();
            
            usedMem2 = usedMem1;
            usedMem1 = usedMemory();
        }
    }
}
