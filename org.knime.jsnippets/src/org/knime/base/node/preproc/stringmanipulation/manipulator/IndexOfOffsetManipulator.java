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

import org.apache.commons.lang3.StringUtils;

/**
 * A StringManipulator to search for a substring. The search is performed
 * from an offset to the right.
 *
 * @author Heiko Hofer
 */
public class IndexOfOffsetManipulator implements Manipulator {

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is
     * not found.
     *
     * @param s the string
     * @param needle the character sequence to search
     * @param start characters in s before this will be ignored
     * @return the index of the first occurrence of needle in s
     */
    public static int indexOf(final CharSequence s,
            final CharSequence needle,
            final int start) {
        return StringUtils.indexOf(s, needle, start);
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
        return getName() + "(str, toSearch, start)";
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
        return "Gives the first position of <i>toSearch</i> in the string. "
                + "The search is performed from the <i>start</i> to the right. "
                + "The function returns -1 if "
                + "<i>toSearch</i> is not found. A negative value of "
                + "<i>start</i> is treated as zero."
                + ""
                + "<br/><br/>"
                + "<strong>Examples:</strong>"
                + "<br/>"
                + "<table>"
                + "<tr><td>indexOf(\"abcabc\", \"ab\", 0)</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"ab\", 1)</td>"
                + "<td>=&nbsp;3</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"ab\", 4)</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOf(\"abcabc\", \"ab\", -10)</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOf(\"\", \"\", 0)</td>"
                + "<td>=&nbsp;0</td></tr>"

                + "<tr><td>indexOf(\"\", *, 0)</td>"
                + "<td>=&nbsp;-1 (except when * = \"\")</td></tr>"

                + "<tr><td>indexOf(null, *, **)</td>"
                + "<td>=&nbsp;-1</td></tr>"

                + "<tr><td>indexOf(*, null, **)</td>"
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
