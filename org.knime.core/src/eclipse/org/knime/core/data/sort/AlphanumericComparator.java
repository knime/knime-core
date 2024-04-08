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
 *   10 Nov 2022 (manuelhotz): created
 */
package org.knime.core.data.sort;

import java.util.Comparator;

/**
 * Comparator that compares strings alpha-numerically.
 *
 * <p>This more human-friendly sort order is sometimes referred to as
 * "<a href="https://en.wikipedia.org/wiki/Natural_sort_order">natural sort order</a>". However, in order to not
 * confuse this order with Java's natural order, we use the term "alpha-numerical" sort order.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public class AlphanumericComparator implements Comparator<String> {

    /**
     * Alpha-numerical comparator which compares non-digit characters using {@link Character#compareTo(Character)}.
     * @since 5.3
     */
    public static final AlphanumericComparator NATURAL_ORDER = new AlphanumericComparator(Comparator.naturalOrder());

    private Comparator<String> m_stringComparator;

    /**
     * The comparator to use for the string chunks.
     *
     * @param stringComparator comparator for string chunks
     */
    public AlphanumericComparator(final Comparator<String> stringComparator) {
        m_stringComparator = stringComparator;
    }

    @Override
    public int compare(final String a, final String b) {
        // The implementation intentionally does not implement the following functionality:
        // - caching of tokenization
        // - foreign language support for digits
        // - collator and locale

        final int aLength = a.length();
        final int bLength = b.length();
        if (aLength == 0) {
            return -bLength;
        } else if (bLength == 0) {
            return 1;
        }

        int aLeft = 0;
        int bLeft = 0;
        boolean number = isDigit(a.charAt(aLeft));
        if (number != isDigit(b.charAt(bLeft))) {
            // different types get handled by delegate comparator
            return m_stringComparator.compare(a, b);
        }

        while (aLeft < aLength && bLeft < bLength) {
            int aRightExcl = nextChunk(a, aLeft, aLength, number);
            int bRightExcl = nextChunk(b, bLeft, bLength, number);
            final int cmp = number ? compareNumber(a, aLeft, aRightExcl, b, bLeft, bRightExcl)
                : m_stringComparator.compare(a.substring(aLeft, aRightExcl), b.substring(bLeft, bRightExcl));
            if (cmp != 0) {
                return cmp;
            }
            aLeft = aRightExcl;
            bLeft = bRightExcl;
            number = !number;
        }

        return Integer.compare(a.length(), b.length());
    }

    private static int compareNumber(final String a, final int aLeft, final int aRightExcl,
        final String b, final int bLeft, final int bRightExcl) {
        // assume: positions [xLeft, xRightExcl) in string only contain digits

        // count (and implicitly trim) leading zeros
        int aStart = skipZeros(a, aLeft, aRightExcl);
        int bStart = skipZeros(b, bLeft, bRightExcl);

        final int aDigits = aRightExcl - aStart;
        final int iCmp = Integer.compare(aDigits, bRightExcl - bStart);
        if (iCmp != 0) {
            // shorter number string (without leading zeros) is smaller
            return  iCmp;
        }

        // actual significant digits are of same length
        // use positional-valued (base 10) comparison
        for (int pos = 0; pos < aDigits; pos++) {
            final int cCmp = Character.compare(a.charAt(aStart + pos), b.charAt(bStart + pos));
            if (cCmp != 0) {
                return cCmp;
            }
        }

        // all significant digits are same, so chunk with less leading zeros should come first
        return Integer.compare(aStart - aLeft, bStart - bLeft);
    }

    /**
     * Find the next offset after the given offset at which the first non-zero character is located.
     *
     * @param s string to skip in
     * @param offset offset to start at
     * @param limit limit until which to skip
     * @return the offset of the first non-zero digit character with leading zeros skipped
     */
    private static int skipZeros(final String s, final int offset, final int limit) {
        int pos = offset;
        while (pos < limit && s.charAt(pos) == '0') {
            pos++;
        }
        return pos;
    }

    /**
     * Gets the position after the end of the next chunk, starting at the given offset until the given limit
     * (the strings length).
     *
     * @param s the string to tokenize
     * @param offset the offset to start at
     * @param limit the limit to search until
     * @param digit whether to look for a digit or another character
     * @return the position after the next chunk of {@code limit} if there is no chunk left
     */
    private static int nextChunk(final String s, final int offset, final int limit, final boolean digit) {
        int curr = offset;
        while (curr < limit && digit == isDigit(s.charAt(curr))) {
            curr++;
        }
        return curr;
    }

    /**
     * Check if the given character is a digit.
     *
     * @param c character to check
     * @return {@code true} if the given character is a digit, {@code false} otherwise
     */
    private static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

}
