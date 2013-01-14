/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
