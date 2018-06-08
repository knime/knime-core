package org.knime.core.node.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/*
 * ------------------------------------------------------------------------
 *
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
 *   17.10.2016 (Jonathan Hale): created
 */

/**
 * Test for {@link FilterableListModel}.
 *
 * @author Jonathan Hale
 */
public class FilterableListModelTest {

    /**
     */
    @Test
    public void testAddBundle() {
        /* Test filtering in BundleListModel */
        final FilterableListModel model =
            new FilterableListModel(Arrays.asList("a", "ab", "b", "abc", "c"));
        assertEquals(5, model.getSize());

        /* Test filtering by "c" yields two results */
        model.setFilter("c");
        assertEquals(model.getSize(), 2);
        assertEquals(model.getElementAt(0), "abc");
        assertEquals(model.getElementAt(1), "c");

        /* Test incremental filtering */
        model.setFilter("a");
        assertEquals(model.getSize(), 3);
        assertEquals(model.getElementAt(0), "a");
        assertEquals(model.getElementAt(1), "ab");
        assertEquals(model.getElementAt(2), "abc");

        model.setFilter("ab");
        assertEquals(model.getSize(), 2);
        assertEquals(model.getElementAt(0), "ab");
        assertEquals(model.getElementAt(1), "abc");

        /* Test clearing the filter */
        model.setFilter("");
        assertEquals(model.getSize(), 5);

        /* Test explicit exclusion */
        model.setExcluded(new String[]{"a", "ab", "b", "c"});
        assertEquals(model.getSize(), 1);
        assertEquals(model.getElementAt(0), "abc");

        model.setExcluded(new String[0]);
        assertEquals(model.getSize(), 5);
    }
}
