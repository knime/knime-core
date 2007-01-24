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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.agents.geneontology.GOTerm;
import org.knime.ext.ainet.core.AnnotationType;
import org.knime.ext.ainet.core.AssociationLink;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNode;
import org.knime.ext.ainet.core.netimpl.AssociationNetFactory;
import org.knime.ext.ainet.core.netimpl.DBAssociationNet;
import org.knime.ext.ainet.data.geneontology.GOEntry;
import org.knime.ext.ainet.data.geneontology.GOEntryFactory;

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
                LOGGER.debug("Node " + i + " of " + noOfNodes + " created");
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
               link.addAnnotation("Loop: " + i, m_annoTypeGeneExpr, 
                       "AnnotID of Loop " + i, weightPerLoop * i);
               LOGGER.debug("Link " + i + " of " + noOfLinks + " created");
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
    
    /**
     * This test case is used to test the Hibernate generated SQL
     */
    public void testSQL() {
        LOGGER.debug("\n\n****Start SQL Test\n\n");
        //repeat the test several times
        for(int i = 1; i <= 500; i++){
            try {
                LOGGER.debug("Creating Node");
                final long node1Start = System.currentTimeMillis();
                AssociationNode node1 = netInstance.createNode(
                        AINetTestSuite.UNIT_NODE_PREFIX + "SQL1" + i);
                final long node1End = System.currentTimeMillis();
                LOGGER.debug("Node created");
                
                LOGGER.debug("Adding node annotation");
                final long node1AnnotStart = System.currentTimeMillis();
                node1.addAnnotation(m_annoTypeGO, 
                        "Fake annotation id 4711 Loop: " + i);
                final long node1AnnotEnd = System.currentTimeMillis();
                LOGGER.debug("Node annotation added");
                
                LOGGER.debug("Creating second node");
                final long node2Start = System.currentTimeMillis();
                AssociationNode node2 = netInstance.createNode(
                        AINetTestSuite.UNIT_NODE_PREFIX + "SQL2" + i);
                final long node2End = System.currentTimeMillis();
                LOGGER.debug("Second node created");
                
                LOGGER.debug("Adding second node annotation");
                final long node2AnnotStart = System.currentTimeMillis();
                node2.addAnnotation(m_annoTypeGO, 
                        "Fake annotation id 4712 Loop: " + i);
                final long node2AnnotEnd = System.currentTimeMillis();
                LOGGER.debug("Second node annotation added");
                LOGGER.debug("Retrieving annotations");
                node2.getAnnotations().size();
                LOGGER.debug("Annotations retrieved");
                
                LOGGER.debug("Creating link");
                final long linkStart = System.currentTimeMillis();
                final AssociationLink link = netInstance.createLink(node1, 
                        node2);
                final long linkEnd = System.currentTimeMillis();
                LOGGER.debug("Link created");
                
                LOGGER.debug("Adding link annotation");
                final long linkAnnotStart = System.currentTimeMillis();
                link.addAnnotation("Performance test annotation" + i, 
                        m_annoTypeGeneExpr, 
                        "Fake annotation source id 4713 Loop: " + i, 0.7);
                LOGGER.debug("Link annotation added");
                LOGGER.debug("Retrieving annotations");
                link.getAnnotations().size();
                LOGGER.debug("Annotations retrieved");
                final long linkAnnotEnd = System.currentTimeMillis();
                final long node1Time = node1End - node1Start;
                final long node1AnnotTime = node1AnnotEnd - node1AnnotStart;
                final long node2Time = node2End - node2Start;
                final long node2AnnotTime = node2AnnotEnd - node2AnnotStart;
                final long linkTime = linkEnd - linkStart;
                final long linkAnnotTime = linkAnnotEnd - linkAnnotStart;
                final long totalTime = node1Time + node1AnnotTime + node2Time 
                    + node2AnnotTime + linkTime + linkAnnotTime;
                LOGGER.info("End of sqlTest (NetType: " 
                        + netInstance.getNetType().toString() + ") Round:" + i);
                LOGGER.info("\tTime for node1 creation: " 
                        + node1Time + "(millis.)\n");
                LOGGER.info("\tTime for node1 annotation creation: " 
                        + node1AnnotTime + "(millis.)\n");
                LOGGER.info("\tTime for node2 creation: " 
                        + node2Time + "(millis.)\n");
                LOGGER.info("\tTime for node2 annotation creation: " 
                        + node2AnnotTime + "(millis.)\n");
                LOGGER.info("\tTime for link creation: " 
                        + linkTime + "(millis.)\n");
                LOGGER.info("\tTime for link annotation creation: " 
                        + linkAnnotTime + "(millis.)\n");
                LOGGER.info("\tTotal time: "
                        + totalTime + "(millis.) or "
                        + (totalTime / 1000) + "(sec) or " 
                        + (totalTime / 1000 / 60) + "(min)");
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
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
