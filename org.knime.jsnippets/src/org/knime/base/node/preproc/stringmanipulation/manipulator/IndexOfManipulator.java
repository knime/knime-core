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


/**
 * A StringManipulator to search for a substring.
 *
 * @author Heiko Hofer
 */
public class IndexOfManipulator implements Manipulator {

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is
     * not found.
     *
     * @param s the string
     * @param needle the character sequence to search
     * @return the index of the first occurrence of needle in s
     */
    public static int indexOf(final CharSequence s,
            final CharSequence needle) {
        return IndexOfOffsetManipulator.indexOf(s, needle, 0);
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
        return "indexOf";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str, toSearch)";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Gives the first positon of <i>toSearch</i> in the string or -1 "
                + "if <i>toSearch</i> is not found in the string. The returned "
                + "value is an integer between 0 (the first character) and "
                + "length(str) - 1 (the last character).<br/>"
                + "<br/>"
                + "<strong>Examples:</strong>"
                + "<br/>"
                + "<table>"
                + "<tr><td>indexOf(\"abcabc\", \"a\")</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"c\")</td>"
                + "<td>=&nbsp;2</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"ca\")</td>"
                + "<td>=&nbsp;2</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"d\")</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOf(\"\", \"\")</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOf(\"\", *)</td>"
                + "<td>=&nbsp;-1 (except when * = \"\")</td></tr>"

                + "<tr><td>indexOf(null, *)</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOf(*, null)</td>"
                + "<td>=&nbsp;-1</td></tr>"
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
