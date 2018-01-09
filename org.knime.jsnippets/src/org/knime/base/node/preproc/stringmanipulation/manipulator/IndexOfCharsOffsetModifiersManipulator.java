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
 *   04.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * A StringManipulator to search for characters. This Manipulator has
 * various binary modifiers like 'ignore case' and 'right to left'.
 *
 * @author Heiko Hofer
 */
public class IndexOfCharsOffsetModifiersManipulator
    implements Manipulator {

    /**
     * Gives the occurrence of a character in searchChars in the string or
     * -1 if the characters in searchChars are not found in the string.
     *
     * @param str the string
     * @param searchChars the character sequence to search
     * @param start characters in s before this will be ignored
     * @param modifiers string with binary options
     * @return the index of the first occurrence of toSearch in s
     */
    public static int indexOfChars(final CharSequence str,
            final String searchChars, final int start,
            final String modifiers) {
        if (StringUtils.isEmpty(str) || StringUtils.isEmpty(searchChars)) {
            return StringUtils.INDEX_NOT_FOUND;
        }
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        return indexOfAny(str, start, opt, searchChars.toCharArray());
    }


    /**
     *  Search for characters in searchChars.
     */
    private static int indexOfAny(final CharSequence cs, final int start,
            final String modifiers, final char... searchChars) {
        boolean backward = StringUtils.contains(modifiers, 'b');

        // forward search
        if (!backward) {
            return indexOfAnyForward(cs, start, modifiers, searchChars);
        } else { // search right to left (backward)
            return indexOfAnyBackward(cs, start, modifiers, searchChars);
        }
    }

    /**
     * Forward search (left to right).
     */
    private static int indexOfAnyForward(final CharSequence cs, final int start,
            final String modifiers, final char... searchChars) {
        boolean ignoreCase = StringUtils.contains(modifiers, 'i');
        boolean matchOpposite = StringUtils.contains(modifiers, 'v');
        int offset = Math.max(start, 0);

        for (int i = offset; i < cs.length(); i++) {
            int c = Character.codePointAt(cs, i);
            if (doesMatch(c, ignoreCase, matchOpposite, searchChars)) {
                return i;
            }
        }
        return StringUtils.INDEX_NOT_FOUND;
    }

    /**
     * Backward search (right to left).
     */
    private static int indexOfAnyBackward(final CharSequence cs,
            final int start,
            final String modifiers, final char... searchChars) {
        boolean ignoreCase = StringUtils.contains(modifiers, 'i');
        boolean matchOpposite = StringUtils.contains(modifiers, 'v');
        int offset = Math.max(start, 0);
        offset = Math.min(cs.length() - 1, offset);

        for (int i = offset; i >= 0; i--) {
            int c = Character.codePointAt(cs, i);
            if (doesMatch(c, ignoreCase, matchOpposite, searchChars)) {
                return i;
            }

        }
        return StringUtils.INDEX_NOT_FOUND;
    }

    /**
     * If matchOpposite is false the method gives true when c is in the
     * searchChars. If matchOpposite is true the method gives false when c is
     * in the searchChars.
     */
    private static boolean doesMatch(final int c, final boolean ignoreCase,
            final boolean matchOpposite, final char... searchChars) {
        boolean match = false;
        for (int j = 0; j < searchChars.length; j++) {
            int cRef = Character.codePointAt(searchChars, j);
            if (!ignoreCase) { // match case
                if (c == cRef) {
                    match = true;
                }
            } else { // ignore case
                if (Character.toUpperCase(c) == Character.toUpperCase(cRef)) {
                    match = true;
                }
            }
            // c matches cReff && this should considered to be a match
            if (match && !matchOpposite) {
                return true;
            }
        }
        // c is not in searchChars && this case should considered as a
        // match
        if (!match && matchOpposite) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory() {
        return "Search";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "indexOfChars";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str, chars, start, modifiers)";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Search a string to find the first position of any character "
                + "in the given set of characters. "
                + "The search is performed from the character at "
                + "<i>start</i>. <i>modifiers</i> gives several options "
                + "to control the search:"
                + "<br/>"
                + "<table>"
                + "<tr><td style=\"padding: 0px 8px 0px 5px;\">i</td> "
                + "<td>ignore case</td></tr>"
                + "<tr><td style=\"padding: 0px 8px 0px 5px;\">b</td> "
                + "<td>backward search</td></tr>"
                + "<tr><td style=\"padding: 0px 8px 0px 5px;\">v</td> "
                + "<td>find any character not in <i>chars</i></td></tr>"
                + "</table>"
                + ""
                + "<br/>"
                + "<strong>Examples:</strong>"
                + "<br/>"
                + "<table>"
                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 0, \"\")</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 1, \"\")</td>"
                + "<td>=&nbsp;1</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 2, \"\")</td>"
                + "<td>=&nbsp;6</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 2, \"i\")</td>"
                + "<td>=&nbsp;3</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 0, \"b\")</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 9, \"b\")</td>"
                + "<td>=&nbsp;7</td></tr>"

                + "<tr><td>indexOf(\"abcABCabc\", \"ab\", 0, \"v\")</td>"
                + "<td>=&nbsp;2</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 3, \"v\")</td>"
                + "<td>=&nbsp;3</td></tr>"

                + "<tr><td>indexOfChars(\"abcABCabc\", \"ab\", 3, \"vi\")</td>"
                + "<td>=&nbsp;5</td></tr>"

                + "<tr><td>indexOfChars(\"\", *, 0, \"\")</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOfChars(null, *, **, \"\")</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOfChars(*, null, **, \"\")</td>"
                + "<td>=&nbsp;-1</td></tr>"
                + "</table>"
                + "* can be any character sequence.<br/>"
                + "** can be any integer.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return Integer.class;
    }
}
