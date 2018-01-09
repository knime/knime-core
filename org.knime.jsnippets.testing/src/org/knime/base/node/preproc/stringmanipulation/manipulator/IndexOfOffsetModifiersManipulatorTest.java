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
 * ------------------------------------------------------------------------
 *
 * History
 *   20.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import org.junit.Assert;
import org.junit.Test;

/**
 * Basic test for the IndexOf string manipulator.
 * @author Heiko Hofer
 */
public class IndexOfOffsetModifiersManipulatorTest {

    /**
     * Test method for
     * {@link IndexOfOffsetModifiersManipulator#indexOf(CharSequence,
     * CharSequence, int, String)}.
     */
    @Test
    public void testIndexOfExamples() {
        // Test the examples in the description of the indexOf function
        Assert.assertEquals(0, IndexOfOffsetModifiersManipulator.indexOf(
                "abcABCabc", "ab", 0, ""));
        Assert.assertEquals(6, IndexOfOffsetModifiersManipulator.indexOf(
                "abcABCabc", "ab", 1, ""));
        Assert.assertEquals(3, IndexOfOffsetModifiersManipulator.indexOf(
                "abcABCabc", "ab", 1, "i"));

        Assert.assertEquals(0, IndexOfOffsetModifiersManipulator.indexOf(
                "abcABCabc", "ab", 0, "b"));
        Assert.assertEquals(6, IndexOfOffsetModifiersManipulator.indexOf(
                "abcABCabc", "ab", 9, "b"));

        Assert.assertEquals(0, IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 0, "w"));
        Assert.assertEquals(8, IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 1, "w"));
        Assert.assertEquals(3, IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "abab", 1, "w"));

        Assert.assertEquals(0, IndexOfOffsetModifiersManipulator.indexOf(
                "", "", 0, ""));
        Assert.assertEquals(-1, IndexOfOffsetModifiersManipulator.indexOf(
                "", "x", 0, ""));
        Assert.assertEquals(-1, IndexOfOffsetModifiersManipulator.indexOf(
                null, "x", 1, ""));
        Assert.assertEquals(-1, IndexOfOffsetModifiersManipulator.indexOf(
                "x", null, 1, ""));
    }

    /**
     * Test word modifier
     */
    @Test
    public void testIndexWordModifier() {
        Assert.assertEquals(0,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 0, "w"));
        Assert.assertEquals(8,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 1, "w"));
        Assert.assertEquals(8,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 2, "w"));
        Assert.assertEquals(8,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 8, "w"));
        Assert.assertEquals(-1,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 9, "w"));
        Assert.assertEquals(-1,
                IndexOfOffsetModifiersManipulator.indexOf(
                "ab abab ab", "ab", 10, "w"));
    }

}
