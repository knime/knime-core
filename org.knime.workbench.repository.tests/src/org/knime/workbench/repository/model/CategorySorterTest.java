/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.11.2013 (thor): created
 */
package org.knime.workbench.repository.model;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Testcases for {@link CategorySorter}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
public class CategorySorterTest {
    @Test
    public void testKnimeBeforeExternal() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "C1", "com.external");
        root.addChild(c1);
        Category c2 = new Category("c2", "C2", "org.knime.bla");
        root.addChild(c2);
        Category c3 = new Category("c3", "C3", "com.something.bla");
        root.addChild(c3);

        IRepositoryObject[] children = root.getChildren();
        assertThat("Unexpected item in first place", children[0], is(sameInstance((IRepositoryObject) c2)));
        assertThat("Unexpected item in second place", children[1], is(sameInstance((IRepositoryObject) c1)));
        assertThat("Unexpected item in third place", children[2], is(sameInstance((IRepositoryObject) c3)));
    }

    @Test
    public void testLexicographicSorting() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "C", "org.knime.bla");
        root.addChild(c1);
        Category c2 = new Category("c2", "A", "org.knime.bla");
        root.addChild(c2);
        Category c3 = new Category("c3", "B", "org.knime.bla");
        root.addChild(c3);

        IRepositoryObject[] children = root.getChildren();
        assertThat("Unexpected item in first place", children[0], is(sameInstance((IRepositoryObject) c2)));
        assertThat("Unexpected item in second place", children[1], is(sameInstance((IRepositoryObject) c3)));
        assertThat("Unexpected item in third place", children[2], is(sameInstance((IRepositoryObject) c1)));
    }


    @Test
    public void testAfterIDs() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "B", "org.knime.bla");
        root.addChild(c1);
        Category c2 = new Category("c2", "Z", "org.knime.bla");
        root.addChild(c2);
        Category c3 = new Category("c3", "A", "org.knime.bla");
        root.addChild(c3);
        Category c4 = new Category("c4", "V", "org.knime.bla");
        root.addChild(c4);

        c1.setAfterID("c2");
        c2.setAfterID("");
        c3.setAfterID("_last_");
        c4.setAfterID("c2");

        IRepositoryObject[] children = root.getChildren();
        assertThat("Unexpected item in first place", children[0], is(sameInstance((IRepositoryObject) c2)));
        assertThat("Unexpected item in second place", children[1], is(sameInstance((IRepositoryObject) c1)));
        assertThat("Unexpected item in third place", children[2], is(sameInstance((IRepositoryObject) c4)));
        assertThat("Unexpected item in fourth place", children[3], is(sameInstance((IRepositoryObject) c3)));
    }


    @Test
    public void testWrongAfterIDs() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "B", "org.knime.bla");
        root.addChild(c1);
        Category c2 = new Category("c2", "Z", "org.knime.bla");
        root.addChild(c2);
        Category c3 = new Category("c3", "A", "org.knime.bla");
        root.addChild(c3);
        Category c4 = new Category("c4", "V", "org.knime.bla");
        root.addChild(c4);

        c1.setAfterID(null);
        c2.setAfterID("");
        c3.setAfterID("c2");
        c4.setAfterID("non-existing");

        IRepositoryObject[] children = root.getChildren();
        assertThat("Unexpected item in first place", children[0], is(sameInstance((IRepositoryObject) c2)));
        assertThat("Unexpected item in second place", children[1], is(sameInstance((IRepositoryObject) c3)));
        assertThat("Unexpected item in third place", children[2], is(sameInstance((IRepositoryObject) c1)));
        assertThat("Unexpected item in fourth place", children[3], is(sameInstance((IRepositoryObject) c4)));
    }

    @Test
    public void testCycle() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "B", "org.knime.bla");
        root.addChild(c1);
        Category c2 = new Category("c2", "Z", "org.knime.bla");
        root.addChild(c2);
        Category c3 = new Category("c3", "A", "org.knime.bla");
        root.addChild(c3);

        c1.setAfterID("c3");
        c2.setAfterID("c1");
        c3.setAfterID("c2");

        IRepositoryObject[] children = root.getChildren();
        // order with a cycle is not defined, but all items must be returned
        assertThat("First category not found", children.length, is(3));
    }

    @Test
    public void testEmptyCategory() {
        Root root = new Root();
        root.setSortChildren(true);
        IRepositoryObject[] children = root.getChildren();
        assertThat("First category not found", children.length, is(0));
    }

    @Test
    public void testMixedCategory() {
        Root root = new Root();
        root.setSortChildren(true);

        Category c1 = new Category("c1", "C1", "com.external");
        root.addChild(c1);
        NodeTemplate n1 = new NodeTemplate("n1", "N1", "org.knime.base");
        root.addChild(n1);

        Category c2 = new Category("c2", "C2", "org.knime.bla");
        root.addChild(c2);
        NodeTemplate n2 = new NodeTemplate("n2", "N2", "com.external");
        root.addChild(n2);

        Category c3 = new Category("c3", "C3", "org.knime.bla");
        root.addChild(c3);
        NodeTemplate n3 = new NodeTemplate("n3", "N3", "org.knime.base");
        root.addChild(n3);

        c2.setAfterID("c3");
        n1.setAfterID("n3");

        IRepositoryObject[] children = root.getChildren();
        assertThat("Unexpected item in first place", children[0], is(sameInstance((IRepositoryObject) c3)));
        assertThat("Unexpected item in second place", children[1], is(sameInstance((IRepositoryObject) c2)));
        assertThat("Unexpected item in third place", children[2], is(sameInstance((IRepositoryObject) c1)));
        assertThat("Unexpected item in fourth place", children[3], is(sameInstance((IRepositoryObject) n3)));
        assertThat("Unexpected item in fifth place", children[4], is(sameInstance((IRepositoryObject) n1)));
        assertThat("Unexpected item in sixth place", children[5], is(sameInstance((IRepositoryObject) n2)));
    }

}
