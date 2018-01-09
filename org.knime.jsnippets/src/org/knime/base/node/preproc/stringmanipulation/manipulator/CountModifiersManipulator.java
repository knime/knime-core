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
 *   25.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;



/**
 * Count substrings in the string.
 *
 * @author Heiko Hofer
 */
public class CountModifiersManipulator implements Manipulator {

    /**
     * Count substrings in the string.
     *
     * @param str the string
     * @param toCount the characters to count
     * @param modifiers modifiers like ignore case
     * @return the count
     */
    public static int count(final String str, final String toCount,
            final String modifiers) {
        if (str == null || str.isEmpty()
                || toCount == null || toCount.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;
        index = IndexOfOffsetModifiersManipulator.indexOf(str, toCount,
                index, modifiers);
        while (-1 != index) {
            count++;
            index += toCount.length();
            index = IndexOfOffsetModifiersManipulator.indexOf(str, toCount,
                    index, modifiers);
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "count";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str, toCount, modifiers)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory() {
        return "Count";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Counts the number of times the string <i>toCount</i> appears "
        + "in a string. "
        + "<i>modifiers</i> gives options to control the method:"
        + "<br/>"
        + "<table>"
        + "<tr><td style=\"padding: 0px 8px 0px 5px;\">i</td> "
        + "<td>ignore case</td></tr>"
        + "<tr><td style=\"padding: 0px 8px 0px 5px;\">w</td> "
        + "<td>whole word (word boundaries are "
        + "whitespace characters)</td></tr>"
        + "</table>"
        + ""
        + "<br/>"
        + "<strong>Examples:</strong>"
        + "<br/>"
        + "<table>"
        + "<tr><td>count(\"abcABCabc\", \"a\", \"\")</td>"
        + "<td>=&nbsp;2</td></tr>"

        + "<tr><td>count(\"abcABCabc\", \"ae\", \"\")</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>count(\"abcABCabc\", \"abc\", \"\")</td>"
        + "<td>=&nbsp;2</td></tr>"

        + "<tr><td>count(\"abcABCabc\", \"abc\", \"i\")</td>"
        + "<td>=&nbsp;3</td></tr>"

        + "<tr><td>count(\"ab abab ab\", \"ab\", \"\")</td>"
        + "<td>=&nbsp;4</td></tr>"

        + "<tr><td>count(\"ab abab ab\", \"ab\", \"w\")</td>"
        + "<td>=&nbsp;2</td></tr>"

        + "<tr><td>count(*, \"\", *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>count(*, null, *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>count(\"\", *, *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>count(null, *, *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "</table>"
        + "* can be any character sequence.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return Integer.class;
    }
}
