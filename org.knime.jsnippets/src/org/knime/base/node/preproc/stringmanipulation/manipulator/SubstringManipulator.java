/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   05.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Heiko Hofer
 */
public class SubstringManipulator implements StringManipulator {

    /**
     * Get a substring.
     *
     * @param str the string to get the substring from
     * @param start the start position starting from 0. Negative means to count
     *  back from the end of the string.
     * @param length the length of the substring
     * @return the substring of size length to the
     */
    public static String substr(final String str, final int start,
            final int length) {
        // the start
        int s = start < 0 ? str.length() + start : start;
        // the end
        int e = s + length;
        // when end position is lower thatn start position
        if (e < s) {
            // swap
            int h = s;
            s = e;
            e = h;
        }
        // when start is negativ
        s = s < 0 ? 0 : s;
        return StringUtils.mid(str, s, e - s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory() {
        return "Extract parts";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "substr";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str, pos, length)";
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
    public String getDescription() {
        return "Get <i>length</i> characters starting from <i>pos</i>."
            + "The position counting is zero based, i.e. to start from the "
            + "beginning use <i>pos = 0</i>. Negative <i>pos</i> values are"
            + "offsets counting from the end of the string."
            + "<br/><br/>"
            + "A <i>length</i> of zero gives an empty string and a negative "
            + "length gives the characters at the left of <i>pos</i>. The "
            + "absolute value of pos may exceed the number of characters at"
            + "the right or at the left of <i>pos</i>, respectively."
            + "<br/><br/>"
            + "<strong>Examples:</strong>"
            + "<br/>"
            + "<table>"
            + "<tr><td>substr(\"abcdef\", 0, 2)</td>"
            + "<td>=</td><td>\"ab\"</td></tr>"
            + ""
            + "<tr><td>substr(\"abcdef\", 3, 10)</td>"
            + "<td>=</td><td>\"456\"</td></tr>"
            + ""
            + "<tr><td>substr(\"123456\", 3, -2)</td>"
            + "<td>=</td><td>\"23\"</td></tr>"
            + ""
            + "<tr><td>substr(\"123456\", -2, 1)</td>"
            + "<td>=</td><td>\"5\"</td></tr>"
            + ""
            + "<tr><td>substr(\"123456\", -2, -3)</td>"
            + "<td>=</td><td>\"234\"</td></tr>"
            + ""
            + "<tr><td>substr(\"123456\", 10, 2)</td>"
            + "<td>=</td><td>\"\"</td></tr>"
            + ""
            + "<tr><td>substr(\"\", *, *)</td><td>=</td><td>\"\"</td></tr>"
            + "<tr><td>substr(null, *, *)</td><td>=</td><td>null</td></tr>"
            + "</table>"
            + "* can be any number.";
    }

    public static void main(final String args[]) {
        System.out.println("substr(..., 0, 2) " + SubstringManipulator.substr("123456", 0, 2));
        System.out.println("substr(..., 1, 20) " + SubstringManipulator.substr("123456", 1, 20));
        System.out.println("substr(..., -2, 1) " + SubstringManipulator.substr("123456", -2, 1));
        System.out.println("substr(..., -2, -3) " + SubstringManipulator.substr("123456", -2, -3));
        System.out.println("substr(..., -10, 2) " + SubstringManipulator.substr("123456", -10, 2));
        System.out.println("substr(..., -10, 6) " + SubstringManipulator.substr("123456", -10, 6));
        System.out.println("substr(..., -10, 10) " + SubstringManipulator.substr("123456", -10, 20));
        System.out.println("substr(..., 10, -2) " + SubstringManipulator.substr("123456", 10, -2));
        System.out.println("substr(..., 10, -6) " + SubstringManipulator.substr("123456", 10, -6));
        System.out.println("substr(..., 10, -20) " + SubstringManipulator.substr("123456", 10, -20));
    }
}
