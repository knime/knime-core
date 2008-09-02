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
package org.knime.base.node.mine.svm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;

/**
 *
 * @author Cebron, University of Konstanz
 */
public class PMMLSVMModelsParser extends TestCase {

    private SAXParser m_parser;

    private PMMLSVMHandler m_handler;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SAXParserFactory fac = SAXParserFactory.newInstance();
        m_parser = fac.newSAXParser();
        m_handler = new PMMLSVMHandler();
    }

    /**
     * Test with iris data (see readme.txt in files directory).
     * @throws Exception
     */
    public void testIrisSVMFile() throws Exception {
        File f = new File(getClass().getResource("files/Iris_SVM.xml").toURI());
        m_parser.parse(f, m_handler);
        // test kernel
        Kernel kernel = m_handler.getKernel();
        assertTrue(kernel.getType().equals(KernelType.RBF));
        assertEquals(kernel.getParameter(0), 1.0);

        // test resulting SVMs
        ArrayList<Svm> svms = m_handler.getSVMs();
        assertEquals(svms.size(), 3);
        Svm svm1 = svms.get(0);
        Svm svm2 = svms.get(1);
        Svm svm3 = svms.get(2);
        assertEquals("Iris-setosa", svm1.getPositive());
        assertEquals("Iris-versic", svm2.getPositive());
        assertEquals("Iris-virgin", svm3.getPositive());

        DoubleVector[] svm1_vectors = svm1.getSupportVectors();
        double[] svm1_alphas = svm1.getAlphas();
        double svm1_beta = svm1.getThreshold();
        assertEquals(5, svm1_vectors.length);
        // pick out a special support vector
        DoubleVector svm1_vec3 = svm1_vectors[2];
        assertEquals(svm1_vec3.getValue(0), 0.055555601);
        assertEquals(svm1_vec3.getValue(1), 0.125);
        assertEquals(svm1_vec3.getValue(2), 0.050847501);
        assertEquals(svm1_vec3.getValue(3), 0.083333299);

        // check alphas and beta
        assertEquals(5, svm1_alphas.length);
        assertTrue(Arrays.equals(new double[]{0.45199634470252,
                2.99551801098074, -4.21266562508397, -1.8326848375528,
                2.59783610695351}, svm1_alphas));
        assertEquals(svm1_beta, -0.1125281);

        DoubleVector[] svm2_vectors = svm2.getSupportVectors();
        double[] svm2_alphas = svm2.getAlphas();
        double svm2_beta = svm2.getThreshold();
        assertEquals(23, svm2_vectors.length);
        // pick out a special support vector
        DoubleVector svm2_vec23 = svm2_vectors[22];
        assertEquals(svm2_vec23.getValue(0), 0.305556);
        assertEquals(svm2_vec23.getValue(1), 0.41666701);
        assertEquals(svm2_vec23.getValue(2), 0.59322);
        assertEquals(svm2_vec23.getValue(3), 0.58333302);

        // check alphas and beta
        assertEquals(23, svm2_alphas.length);
        assertTrue(Arrays.equals(new double[]{-10, 0.997867892811911, 10, -10,
                7.38288855106825, 10, -10, 10, -10, 6.27223397561835, 10, -10,
                7.95828514700341, 10, -2.69950324313558, 10, -10,
                1.28671521403663, -1.19848753740294, -10, -10, 10, -10},
                svm2_alphas));
        assertEquals(svm2_beta, 3.6416813);

        DoubleVector[] svm3_vectors = svm3.getSupportVectors();
        double[] svm3_alphas = svm3.getAlphas();
        double svm3_beta = svm3.getThreshold();
        assertEquals(18, svm3_vectors.length);
        // pick out a special support vector
        DoubleVector svm3_vec1 = svm3_vectors[0];
        assertEquals(svm3_vec1.getValue(0), 0.52777803);
        assertEquals(svm3_vec1.getValue(1), 0.33333299);
        assertEquals(svm3_vec1.getValue(2), 0.644068);
        assertEquals(svm3_vec1.getValue(3), 0.70833302);

        // check alphas and beta
        assertEquals(18, svm3_alphas.length);
        assertTrue(Arrays.equals(new double[]{-10, 10, -4.36787034664627, 10,
                -10, 10, -10, -10, 3.12054051236728, -3.01478624343503, 10, 10,
                -10, 10, 10, -10, 2.46028574315568, -8.1981696654417},
                svm3_alphas));
        assertEquals(svm3_beta, -0.58830825);
    }
}
