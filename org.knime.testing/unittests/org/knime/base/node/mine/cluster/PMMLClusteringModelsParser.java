/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.base.node.mine.cluster;

import java.io.File;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLClusteringModelsParser extends TestCase {
    
    private SAXParser m_parser;
    private PMMLClusterHandler m_handler;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SAXParserFactory fac = SAXParserFactory.newInstance();
        m_parser = fac.newSAXParser();
        m_handler = new PMMLClusterHandler();
    }
    
    
    public void testIrisClusterKMeansFile() throws Exception {
        File f = new File(getClass().getResource(
                "files/IRIS_CLUSTER_KMEANS.xml").toURI());
        m_parser.parse(f, m_handler);
        // nr of clusters
        assertEquals(3, m_handler.getNrOfClusters());
        // cluster coverage
        assertEquals(50, m_handler.getClusterCoverage()[0]);
        assertEquals(39, m_handler.getClusterCoverage()[1]);
        assertEquals(61, m_handler.getClusterCoverage()[2]);
        // cluster names 
        assertEquals("1", m_handler.getLabels()[0]);
        assertEquals("2", m_handler.getLabels()[1]);
        assertEquals("3", m_handler.getLabels()[2]);
        // max values
        assertEquals(6.9, m_handler.getMaxs()[0]);
        assertEquals(2.5, m_handler.getMaxs()[1]);
        assertEquals(7.9, m_handler.getMaxs()[2]);
        assertEquals(4.4, m_handler.getMaxs()[3]);
        // min values
        assertEquals(1.0, m_handler.getMins()[0]);
        assertEquals(0.1, m_handler.getMins()[1]);
        assertEquals(4.3, m_handler.getMins()[2]);
        assertEquals(2.0, m_handler.getMins()[3]);

        // prototypes
        assertEquals(0.0786441, m_handler.getPrototypes()[0][0]);
        assertEquals(0.06, m_handler.getPrototypes()[0][1]);
        assertEquals(0.196111, m_handler.getPrototypes()[0][2]);
        assertEquals(0.590833, m_handler.getPrototypes()[0][3]);
        
        assertEquals(0.797045, m_handler.getPrototypes()[1][0]);
        assertEquals(0.824786, m_handler.getPrototypes()[1][1]);
        assertEquals(0.707265, m_handler.getPrototypes()[1][2]);
        assertEquals(0.450855, m_handler.getPrototypes()[1][3]);
        assertTrue(Arrays.equals(
                new double[] {0.575715, 0.54918, 0.441257, 0.307377},
                m_handler.getPrototypes()[2]));
        
        
        // used columns
        assertTrue(m_handler.getUsedColumns().contains("petal length"));
        assertTrue(m_handler.getUsedColumns().contains("petal width"));
        assertTrue(m_handler.getUsedColumns().contains("sepal length"));
        assertTrue(m_handler.getUsedColumns().contains("sepal width"));
        
        // general 
        assertEquals(m_handler.getNrOfClusters(), 
                m_handler.getPrototypes().length);
        assertEquals(m_handler.getUsedColumns().size(), 
                m_handler.getPrototypes()[0].length);
    }
    
    
    public void testCenterbasedClusteringFile() throws Exception {
        File f = new File(getClass().getResource(
                "files/CenterbasedClustering.xml").toURI());
        m_parser.parse(f, m_handler);
        // TODO: evaluate result 
        // check number of clusters
        assertEquals(6, m_handler.getNrOfClusters());
        // check prototypes
        assertEquals(m_handler.getUsedColumns().size(), 
                m_handler.getPrototypes()[0].length);
        assertTrue(Arrays.equals(
                new double[] {0.552542, 0.510833, 0.454444, 0.320833, 0}, 
                m_handler.getPrototypes()[0]));
        assertTrue(Arrays.equals(
                new double[] {0.0710561, 0.0576923, 0.24359, 0.685897, 0},
                m_handler.getPrototypes()[1]));
        assertTrue(Arrays.equals(
                new double[] {0.08686439999999999, 0.0625, 0.144676, 0.487847, 0},
                m_handler.getPrototypes()[2]));
        assertTrue(Arrays.equals(
                new double[] {0.83116, 0.866987, 0.755342, 0.482372, 1},
                m_handler.getPrototypes()[3]));
        assertTrue(Arrays.equals(
                new double[] {0.717797, 0.7375, 0.5375, 0.345833, 1},
                m_handler.getPrototypes()[4]));
        assertTrue(Arrays.equals(
                new double[] {0.652542, 0.708333, 0.347222, 0.208333, 1},
                m_handler.getPrototypes()[5]));        
        // check min & max
        assertEquals(10.0, m_handler.getMins()[0]);
        assertEquals(69.0, m_handler.getMaxs()[0]);
        assertEquals(1.0, m_handler.getMins()[1]);
        assertEquals(25.0, m_handler.getMaxs()[1]);
        assertEquals(43.0, m_handler.getMins()[2]);
        assertEquals(79.0, m_handler.getMaxs()[2]);
        assertEquals(20.0, m_handler.getMins()[3]);
        assertEquals(44.0, m_handler.getMaxs()[3]);
        // check used columns
        assertTrue(m_handler.getUsedColumns().contains("PETALLEN"));
        assertTrue(m_handler.getUsedColumns().contains("PETALWID"));
        assertTrue(m_handler.getUsedColumns().contains("SEPALLEN"));
        assertTrue(m_handler.getUsedColumns().contains("SEPALWID"));
        assertTrue(m_handler.getUsedColumns().contains("SPECIES"));
        // check cluster names
        assertEquals("0", m_handler.getLabels()[0]);
        assertEquals("2", m_handler.getLabels()[1]);
        assertEquals("5", m_handler.getLabels()[2]);
        assertEquals("6", m_handler.getLabels()[3]);
        assertEquals("7", m_handler.getLabels()[4]);
        assertEquals("8", m_handler.getLabels()[5]);
    }
   
    
    public void testPMMLClusteringExample() throws Exception {
        File f = new File(getClass().getResource(
        "files/PMMLClusteringExample.xml").toURI());
        m_parser.parse(f, m_handler);
        // check number of clusters
        assertEquals(2, m_handler.getNrOfClusters());
        // check prototypes
        assertEquals(m_handler.getUsedColumns().size(), 
                m_handler.getPrototypes()[0].length);
        
        assertTrue(Arrays.equals(
                new double[] {0.524561, 0.486321, 0.128427},
                m_handler.getPrototypes()[0]));
        assertTrue(Arrays.equals(
                new double[] {0.69946, 0.419037, 0.591226},
                m_handler.getPrototypes()[1]));
        // check min & max
        // column 0 is marital status -> nominal column
        assertEquals(45.0, m_handler.getMins()[1]);
        assertEquals(105.0, m_handler.getMaxs()[1]);
        assertEquals(39000.0, m_handler.getMins()[2]);
        assertEquals(41000.0, m_handler.getMaxs()[2]);
        // check used columns
        assertTrue(m_handler.getUsedColumns().contains("marital status"));
        assertTrue(m_handler.getUsedColumns().contains("age"));
        assertTrue(m_handler.getUsedColumns().contains("salary"));
        // check cluster names
        assertEquals("marital status is d or s", m_handler.getLabels()[0]);
        assertEquals("marital status is m", m_handler.getLabels()[1]);
        
    }
}
