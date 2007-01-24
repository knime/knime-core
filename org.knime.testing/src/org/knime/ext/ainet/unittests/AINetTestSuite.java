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
 *    18.10.2006 (Tobias Koetter): created
 */
package org.knime.ext.ainet.unittests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.knime.core.node.NodeLogger;
import org.knime.ext.ainet.core.AssociationNet;
import org.knime.ext.ainet.core.AssociationNetType;
import org.knime.ext.ainet.core.netimpl.DBAssociationNet;

/**
 * Performs all aiNet test in a row.
 * @author Tobias Koetter, University of Konstanz
 */
public class AINetTestSuite {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(AINetTestSuite.class);
    /**
     * Runtime.
     */
    protected static final Runtime RUNTIME = Runtime.getRuntime();
//test values
    /**The prefix which should be used for all node names.*/
    protected static final String UNIT_NODE_PREFIX = "UNIT_TEST@";
    
    /**The type of the network to test.*/
    private static AssociationNetType networkType = AssociationNetType.DB_NET; 
        //AssociationNetType.getDefaultType();
    private static boolean cleanUp = true;
    
    /**The name of the network.*/
    private static final String NETWORK_NAME = "JUnitTestNetwork";
    /**
     * @return the test which will be performed
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for org.knime.ext.ainet.unittests");
        //set the network type you want to test or it uses the default type
        networkType = AssociationNetType.DB_NET;
        //$JUnit-BEGIN$
//        suite.addTestSuite(GOEntryTest.class);
//        suite.addTestSuite(GOTermTest.class);
        suite.addTestSuite(AssociationNetNodeTest.class);
        suite.addTestSuite(AssociationNetLinkTest.class);
        suite.addTestSuite(AssociationNetAnnotationTest.class);
        suite.addTestSuite(AssociationNetTest.class);
        suite.addTestSuite(XMLPersistenceManagerTest.class);
        //set the performance test parameter
        AssociationNetPerformanceTest.setNoOfLinks(50);
        AssociationNetPerformanceTest.setMemoryTest(true);
        suite.addTestSuite(AssociationNetPerformanceTest.class);

        //$JUnit-END$
        return suite;
    }


    /**
     * Used in all ainet test methods to remove all inserted objects from the
     * network. 
     * @param net the network to reset
     * @param nodes2del the nodes to remove from the network
     * @param links2del the links to remove from the network
     * @throws Exception if an object couldn't be removed
     */
    protected static void networkTearDown(final AssociationNet net) 
    throws Exception {
        LOGGER.debug("Start networkTearDown");
        if (!cleanUp) {
            return;
        }
//        return;
            //delete all unit test entries
            //remove all test elements from the network
        //remove the links first and then the nodes we will get an 
        //NodeNotExistence exception otherwise!
        try {
            if (net instanceof DBAssociationNet) {
                DBAssociationNet dbNet = (DBAssociationNet) net;
                dbNet.deleteNet();
            }
//            
//            if (links2del != null) {
//                LOGGER.debug("removing " + links2del.size() + " links ...");
//                for (AssociationLink link : links2del) {
//                    net.removeLink(link);
//                }
//                //check if all links were deleted
//                for (AssociationLink link : links2del) {
//                    if (net.getLink(link.getNode1(), link.getNode2(), 
//                            link.getType()) != null) {
//                        throw new Exception("Link couldn't be removed: " 
//                                + link.toString());
//                    }
//                }
//            }
//            
//            if (nodes2del != null) {
//                LOGGER.debug("removing " + nodes2del.size() + " node");
//                for (AssociationNode node : nodes2del) {
//                    net.removeNode(node);
//                }
//                //check if all nodes were deleted
//                for (AssociationNode node : nodes2del) {
//                    if (net.nodeExists(node.getName())) {
//                        throw new Exception("Node couldn't be removed: " 
//                                + node.toString());
//                    }
//                }
//            }
            net.cleanUp();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
//        LOGGER.debug("End networkTearDown");
    }

    /**
     * @return the network type to use in the test run
     */
    protected static AssociationNetType getNetType() {
        return networkType;
    }
    
    /**
     * @return the name to use for test runs
     */
    protected static String getNetName() {
        return NETWORK_NAME;
    }
}
