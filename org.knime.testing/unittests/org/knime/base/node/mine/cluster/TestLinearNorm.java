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


import junit.framework.TestCase;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TestLinearNorm extends TestCase {
    
    public void testLinearNorm() {
        LinearNorm linearNorm = new LinearNorm("test");
        linearNorm.addInterval(45, 0);
        linearNorm.addInterval(82, 0.5);
        linearNorm.addInterval(102, 1);
        
        assertEquals(45.0, linearNorm.unnormalize(0.0));
        assertEquals(82.0, linearNorm.unnormalize(0.5));
        assertEquals(102.0, linearNorm.unnormalize(1.0));
    }
    
    public void testLinearNormSimple() {
        LinearNorm linearNorm = new LinearNorm("simple");
        linearNorm.addInterval(0.0, 0.0);
        linearNorm.addInterval(50.0, 0.5);
        linearNorm.addInterval(100, 1);
        
        assertEquals(0.0, linearNorm.unnormalize(0.0));
        assertEquals(50.0, linearNorm.unnormalize(0.5));
        assertEquals(100.0, linearNorm.unnormalize(1.0));
        assertEquals(40.0, linearNorm.unnormalize(0.4));
        assertEquals(25.0, linearNorm.unnormalize(0.25));
        
    }

}
