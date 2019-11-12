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
 *   Nov 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.core.data.probability.nominal.NominalDistributionUtil;

/**
 * Unit tests for NominalDistributionUtil.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NominalDistributionUtilTest {

    private static String[] s(final String... strings) {
        return strings;
    }

    /**
     * Tests the general functionality of the toTrimmedSet method.
     */
    @Test
    public void testToTrimmedSet() {
        final String[] noWhitespaces = s("A", "B", "C");
        final LinkedHashSet<String> expected =
            Arrays.stream(noWhitespaces).collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(expected, NominalDistributionUtil.toTrimmedSet(noWhitespaces));
        final String[] whitespaces = s(" A", "B ", " C ");
        assertEquals(expected, NominalDistributionUtil.toTrimmedSet(whitespaces));
    }

    /**
     * Verifies that providing null as argument to toTrimmedSet results in a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToTrimmedSetFailsOnNullValues() {
        NominalDistributionUtil.toTrimmedSet(null);
    }

    /**
     * Verifies that an empty string in the argument to toTrimmedSet results in an {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToTrimmedSetFailsOnEmptyString() {
        NominalDistributionUtil.toTrimmedSet(s("A", ""));
    }

    /**
     * Verifies that a string consisting only of whitespaces leads to an {@link IllegalArgumentException} in
     * toTrimmedSet.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToTrimmedSetFailsOnOnlyWhitespaces() {
        NominalDistributionUtil.toTrimmedSet(s("A", " "));
    }

    /**
     * Verifies that an empty array leads to an {@link IllegalArgumentException} in toTrimmedSet.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToTrimmedSetFailsOnEmptyArray() {
        NominalDistributionUtil.toTrimmedSet(new String[0]);
    }

    /**
     * Verifies that toTrimmedSet fails with an {@link IllegalArgumentException} on duplicates.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToTrimmedSetFailsOnDuplicates() {
        NominalDistributionUtil.toTrimmedSet(s("A", "B", "A"));
    }

    /**
     * Verifies that toTrimmedSet fails with an {@link IllegalArgumentException} if two values become equal through
     * trimming.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testToTrimmedSetFailsOnDuplicatesThroughTrimming() {
        NominalDistributionUtil.toTrimmedSet(s("A", "B", " A"));
    }
}
