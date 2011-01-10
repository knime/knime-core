/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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

        // create the linear norms here
        LinearNorm plNorm = new LinearNorm("petal length");
        plNorm.addInterval(1, 0);
        plNorm.addInterval(6.9, 1);

        LinearNorm pwNorm = new LinearNorm("petal width");
        pwNorm.addInterval(0.1, 0);
        pwNorm.addInterval(2.5, 1);
        
        LinearNorm slNorm = new LinearNorm("sepal length");
        slNorm.addInterval(4.3, 0);
        slNorm.addInterval(7.9, 1);
        
        LinearNorm swNorm = new LinearNorm("sepal width");
        swNorm.addInterval(2, 0);
        swNorm.addInterval(4.4, 1);
        
        // prototypes
        assertEquals(plNorm.unnormalize(0.0786441), 
                m_handler.getPrototypes()[0][0]);
        assertEquals(pwNorm.unnormalize(0.06), 
                m_handler.getPrototypes()[0][1]);
        assertEquals(slNorm.unnormalize(0.196111), 
                m_handler.getPrototypes()[0][2]);
        assertEquals(swNorm.unnormalize(0.590833), 
                m_handler.getPrototypes()[0][3]);
        
        assertEquals(plNorm.unnormalize(0.797045), 
                m_handler.getPrototypes()[1][0]);
        assertEquals(pwNorm.unnormalize(0.824786), 
                m_handler.getPrototypes()[1][1]);
        assertEquals(slNorm.unnormalize(0.707265), 
                m_handler.getPrototypes()[1][2]);
        assertEquals(swNorm.unnormalize(0.450855), 
                m_handler.getPrototypes()[1][3]);
        assertTrue(Arrays.equals(
                new double[] {
                        plNorm.unnormalize(0.575715), 
                        pwNorm.unnormalize(0.54918), 
                        slNorm.unnormalize(0.441257), 
                        swNorm.unnormalize(0.307377)},
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
        
        // create linear norms
        LinearNorm plNorm = new LinearNorm("PETALLEN");
        plNorm.addInterval(10, 0);
        plNorm.addInterval(39.5, 0.5);
        plNorm.addInterval(69, 1);

        LinearNorm pwNorm = new LinearNorm("PETALWID");
        pwNorm.addInterval(1, 0);
        pwNorm.addInterval(13, 0.5);
        pwNorm.addInterval(25, 1);

        LinearNorm slNorm = new LinearNorm("SEPALLEN");
        slNorm.addInterval(43, 0);
        slNorm.addInterval(61, 0.5);
        slNorm.addInterval(79, 1);

        LinearNorm swNorm = new LinearNorm("SEPALWID");
        swNorm.addInterval(20, 0);
        swNorm.addInterval(32, 0.5);
        swNorm.addInterval(44, 1);
        
        // check prototypes
        assertEquals(m_handler.getUsedColumns().size(), 
                m_handler.getPrototypes()[0].length);
        // cluster 0
        double[] cluster0 = new double[] {
                plNorm.unnormalize(0.552542), 
                pwNorm.unnormalize(0.510833), 
                slNorm.unnormalize(0.454444), 
                swNorm.unnormalize(0.320833), 
                0};
        double[] readCluster0 = m_handler.getPrototypes()[0];
        System.out.println("************ cluster 0 *************");
        for (int i = 0; i < cluster0.length; i++) {
            System.out.println(cluster0[i] + " vs " + readCluster0[i]);
        }
        // cluster 1
        double[] cluster1 = new double[] {
                plNorm.unnormalize(0.0710561), 
                pwNorm.unnormalize(0.0576923), 
                slNorm.unnormalize(0.24359), 
                swNorm.unnormalize(0.685897), 
                0};
        double[] readCluster1 = m_handler.getPrototypes()[1];  
        System.out.println("************ cluster 1 *************");
        for (int i = 0; i < cluster1.length; i++) {
            System.out.println(cluster1[i] + " vs " + readCluster1[i]);
        }
        // cluster 2 
        double[] cluster2 = new double[] {
                plNorm.unnormalize(0.08686439999999999), 
                pwNorm.unnormalize(0.0625), 
                slNorm.unnormalize(0.144676), 
                swNorm.unnormalize(0.487847), 
                0} ;
        double[] readCluster2 = m_handler.getPrototypes()[2]; 
        System.out.println("************ cluster 2 *************");
        for (int i = 0; i < cluster2.length; i++) {
            System.out.println(cluster2[i] + " vs " + readCluster2[i]);
        }
        // cluster 3 
        double[] cluster3 = new double[] {
                plNorm.unnormalize(0.83116), 
                pwNorm.unnormalize(0.866987), 
                slNorm.unnormalize(0.755342), 
                swNorm.unnormalize(0.482372), 
                1}; 
        double[] readCluster3 = m_handler.getPrototypes()[3];
        System.out.println("************ cluster 3 *************");
        for (int i = 0; i < cluster3.length; i++) {
            System.out.println(cluster3[i] + " vs " + readCluster3[i]);
        }
        // cluster 4 
        double[] cluster4 = new double[] {
                plNorm.unnormalize(0.717797), 
                pwNorm.unnormalize(0.7375), 
                slNorm.unnormalize(0.5375), 
                swNorm.unnormalize(0.345833), 
                1}; 
        double[] readCluster4 = m_handler.getPrototypes()[4];  
        System.out.println("************ cluster 4 *************");
        for (int i = 0; i < cluster4.length; i++) {
            System.out.println(cluster4[i] + " vs " + readCluster4[i]);
        }        
        // cluster 5
        double[] cluster5 = new double[] {
                plNorm.unnormalize(0.652542), 
                pwNorm.unnormalize(0.708333), 
                slNorm.unnormalize(0.347222), 
                swNorm.unnormalize(0.208333), 
                1}; 
        double[] readCluster5 = m_handler.getPrototypes()[5];
        System.out.println("************ cluster 5 *************");
        for (int i = 0; i < cluster5.length; i++) {
            System.out.println(cluster5[i] + " vs " + readCluster5[i]);
        } 
        
        assertTrue(Arrays.equals(
                cluster0, 
                readCluster0));
        assertTrue(Arrays.equals(
                cluster1,
                readCluster1));
        assertTrue(Arrays.equals(
                cluster2,
                readCluster2));
        assertTrue(Arrays.equals(
                cluster3,
                readCluster3));
        assertTrue(Arrays.equals(
                cluster4,
                readCluster4));
        assertTrue(Arrays.equals(
                cluster5,
                readCluster5));        

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
        
        LinearNorm ageNorm = new LinearNorm("age");
        ageNorm.addInterval(45, 0);
        ageNorm.addInterval(82, 0.5);
        ageNorm.addInterval(105, 1);
        
        LinearNorm salaryNorm = new LinearNorm("salary");
        salaryNorm.addInterval(39000, 0);
        salaryNorm.addInterval(39800, 0.5);
        salaryNorm.addInterval(41000, 1);
        
        assertTrue(Arrays.equals(
                new double[] {
                        0.524561, 
                        ageNorm.unnormalize(0.486321), 
                        salaryNorm.unnormalize(0.128427)},
                m_handler.getPrototypes()[0]));
        assertTrue(Arrays.equals(
                new double[] {
                        0.69946, 
                        ageNorm.unnormalize(0.419037), 
                        salaryNorm.unnormalize(0.591226)},
                m_handler.getPrototypes()[1]));
        // check used columns
        assertTrue(m_handler.getUsedColumns().contains("marital status"));
        assertTrue(m_handler.getUsedColumns().contains("age"));
        assertTrue(m_handler.getUsedColumns().contains("salary"));
        // check cluster names
        assertEquals("marital status is d or s", m_handler.getLabels()[0]);
        assertEquals("marital status is m", m_handler.getLabels()[1]);
    }
    
    
    public void testKNIMEClusteringModel() throws Exception {
        File f = new File(getClass().getResource(
        "files/iris_kMeans.pmml").toURI());
        m_parser.parse(f, m_handler);
        // check number of clusters
        assertEquals(3, m_handler.getNrOfClusters());
        // check prototypes
        assertEquals(m_handler.getUsedColumns().size(), 4);
        
        // check prototypes
        
        assertTrue(Arrays.equals(new double[] {
                6.853846153846153, 
                3.0769230769230766, 
                5.715384615384615, 
                2.053846153846153},
                m_handler.getPrototypes()[0]));
        
        assertTrue(Arrays.equals(new double[] {
                5.88360655737705,
                2.740983606557377,
                4.388524590163935,
                1.4344262295081966}, 
                m_handler.getPrototypes()[1]));
        
        assertTrue(Arrays.equals(new double[] {
                5.005999999999999, 
                3.428000000000001,
                1.4620000000000002, 
                0.2459999999999999},
                m_handler.getPrototypes()[2]));
        
    }
}
