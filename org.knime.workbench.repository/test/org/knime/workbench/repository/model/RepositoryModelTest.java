/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import junit.framework.TestCase;

/**
 * @author Florian Georg, University of Konstanz
 */
public class RepositoryModelTest extends TestCase {

    /**
     * 
     * @param args ignored
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(RepositoryModelTest.class);
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test the fundamental function of the model (add/remove/move elements
     * within the tree).
     * 
     */
    public void testModelStructure() {
        Root root = new Root();
        NodeTemplate one = new NodeTemplate("one");
        NodeTemplate two = new NodeTemplate("two");
        NodeTemplate three = new NodeTemplate("three");
        NodeTemplate four = new NodeTemplate("four");
        NodeTemplate five = new NodeTemplate("five");
        NodeTemplate six = new NodeTemplate("six");

        Category cat1 = new Category("cat1");
        Category cat2 = new Category("cat2");
        Category cat3 = new Category("cat3");

        //
        // Test 'add' operations
        //

        // Add two nodes and two categories to the root
        root.addChild(one);
        root.addChild(two);
        root.addChild(cat1);
        root.addChild(cat2);

        assertTrue(root.hasChildren());
        assertEquals(4, root.getChildren().length);
        assertFalse(cat1.hasChildren());
        assertFalse(cat2.hasChildren());
        assertEquals(root, one.getParent());
        assertEquals(root, two.getParent());

        // add sub-category
        cat2.addChild(cat3);

        assertTrue(cat2.hasChildren());
        assertEquals(1, cat2.getChildren().length);
        assertEquals(cat2, cat3.getParent());

        // add children to the categories
        cat1.addChild(three);
        cat2.addChild(four);
        cat2.addChild(five);
        cat3.addChild(six);

        assertTrue(cat2.hasChildren());
        assertEquals(3, cat2.getChildren().length);

        //
        // Consistency checks
        //
        try {
            root.setParent(new Root());
            fail("setParent on root should throw Exception");
        } catch (UnsupportedOperationException e) {
            // O.K.
        }
        try {
            cat3.setParent(cat3);
            fail("setParent to 'this' should throw exception");
        } catch (IllegalArgumentException e) {
            // O.K.
        }
        try {
            cat2.addChild(cat2);
            fail("addChild with 'this' should throw exception");
        } catch (IllegalArgumentException e) {
            // O.K.
        }
        try {
            cat1.addChild(new Root());
            fail("add root as a child should throw exception");
        } catch (IllegalArgumentException e) {
            // O.K.
        }

        //
        // "Move" operations
        //
        assertTrue(five.getParent() == cat2);
        assertEquals(3, cat2.getChildren().length);

        five.move(root);
        assertEquals(2, cat2.getChildren().length);
        assertEquals(5, root.getChildren().length);
        assertTrue(root == five.getParent());

        //
        // Remove Children
        //
        assertEquals(1, cat3.getChildren().length);
        cat3.removeChild(six);
        assertNull(six.getParent());
        assertFalse(cat3.hasChildren());

        // re-add
        cat3.addChild(six);
        assertEquals(1, cat3.getChildren().length);

        // remove complete category
        assertEquals(5, root.getChildren().length);
        root.removeChild(cat2);
        assertEquals(4, root.getChildren().length);
        assertTrue(cat2.getParent() == null);

        // re-add
        root.addChild(cat2);
        assertEquals(5, root.getChildren().length);
        assertTrue(cat2.getParent() == root);

        // Consistency checks

        try {
            cat3.removeChild(cat2);
            fail("removeChild with non-child arg should throw exception");
        } catch (IllegalArgumentException e) {
            // O.K.
        }

    }

}
