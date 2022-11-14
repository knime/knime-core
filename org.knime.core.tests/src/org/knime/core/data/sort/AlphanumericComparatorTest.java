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
 *   10 Nov 2022 ("Manuel Hotz &lt;manuel.hotz@knime.com&gt;"): created
 */
package org.knime.core.data.sort;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

/**
 * Tests the alphanumeric comparator.
 *
 * @author "Manuel Hotz &lt;manuel.hotz@knime.com&gt;"
 */
public class AlphanumericComparatorTest {

    private static final Comparator<String> COMP = new AlphanumericComparator(Comparator.naturalOrder());


    private static List<String> asMutableList(final String... items) {
        return new ArrayList<>(List.of(items));
    }

    @Test
    public void testEmpty() {
        assertEquals(0, COMP.compare("", ""));
        assertTrue(COMP.compare("", "1") < 0);
        assertTrue(COMP.compare("1", "") > 0);
        assertTrue(COMP.compare("", "a") < 0);
        assertTrue(COMP.compare("a", "") > 0);
    }

    @Test
    public void testDifferentChunkTypesAtFront() {
        assertTrue(COMP.compare("a", "1") > 0 && COMP.compare("1", "a") < 0);
    }

    @Test
    public void testDifferentLengths() {
        // different string lengths
        assertTrue(COMP.compare("aa", "a") > 0);
        assertTrue(COMP.compare("a", "aa") < 0);

        // different number of chunks
        assertTrue(COMP.compare("0.0", "0.") > 0);
        assertTrue(COMP.compare("0.", "0.0") < 0);
    }

    @Test
    public void testLeadingZeros() {
        final var ref = List.of(
            "0", "00", "000", "0000", "00000", "000000",
            "1", "01", "001", "0001", "00001", "000001",
            "2", "02", "002", "0002", "00002", "000002"
            );
        final var data = new ArrayList<>(ref);
        Collections.shuffle(data);
        Collections.sort(data, COMP);
        assertEquals(ref, data, "Leading zeros not correctly handled.");
    }

    @Test
    public void testSymmetry() {
        symmetricEquals("42", "42", "Equal values not symmetric");
        symmetricEquals("a", "b", "Non-digits not symmetric");
        symmetricEquals("41", "42", "Digits not symmetric");
        symmetricEquals("41a", "42a", "Mixed not symmetric");
    }

    private static void symmetricEquals(final String a, final String b, final String message) {
        assertEquals((int) Math.signum(COMP.compare(a, b)), -((int)Math.signum(COMP.compare(b, a))), message);
    }

    /**
     * Tests that that {@code ((compare(x, y)>0) && (compare(y, z)>0))} implies {@code compare(x, z)>0}
     * for (some) {@code x,y,z}.
     */
    @Test
    public void testTransitivity() {
        final String[][] xyzs = {
            {"", "", ""},
            {"a", "b", "c"},
            {"1", "2", "3"},
            {"a1", "a2", "a3"},
            {"a1", "b2", "b3"},
            {"10a10", "10b10", "10a11"},
            {"10a1", "0100b10", "10a10"},
        };

        for (String[] xyz : xyzs) {
            final var x = xyz[0];
            final var y = xyz[1];
            final var z = xyz[2];
            assertTrue("Comparison not transitive",
                !(COMP.compare(x, y) > 0 && COMP.compare(y, z) > 0) || COMP.compare(x, z) > 0);
        }
    }

    /**
     * Tests that {@code compare(x, y)==0} implies {@code sgn(compare(x, z))==sgn(compare(y, z))}
     * for (some) {@code x,y,z}.
     */
    @Test
    public void testTriangle() {

        final String[][] xyzs = {
            {"", "", ""},
            {"a", "b", "c"},
            {"1", "2", "3"},
            {"a1", "a2", "a3"},
            {"a1", "b2", "b3"},
            {"10a10", "10b10", "10a11"},
            {"10a1", "100b10a", "10a10a1a"},
        };

        for (String[] xyz : xyzs) {
            final var x = xyz[0];
            final var y = xyz[1];
            final var z = xyz[2];
            assertTrue(!(COMP.compare(x, y) == 0)
                || (int)Math.signum(COMP.compare(x, z)) == (int)Math.signum(COMP.compare(y, z)));
        }
    }

    /**
     * Tests that strings containing no numbers are still alphabetically sorted using the comparator.
     */
    @Test
    public void testOnlyStringsLexicographicallySorted() {
        final var data = asMutableList("a", "zz", "ab", "bz", "az", "za");
        Collections.sort(data, COMP);
        final var ref = List.of("a", "ab", "az", "bz", "za", "zz");
        assertEquals(ref, data, "String-only list is not sorted lexicographically.");
    }

    /**
     * Tests some filenames from the ticket.
     */
    @Test
    public void testSomeFilenames() {
        final var ref = List.of("test1a.txt", "test2a.txt", "test2b.txt", "test10a.txt", "test12c.txt",
            "test100.txt", "test123.txt");
        final var data = new ArrayList<>(ref);
        Collections.shuffle(data);
        Collections.sort(data, COMP);
        assertEquals(ref, data, "List is not sorted alphanumerically.");
    }

}
