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

import org.apache.commons.lang3.StringUtils;

/**
 * The stripEnd string manipulator for stripping white space characters.
 *
 * @author Heiko Hofer
 */
public class StripEndManipulator implements Manipulator {

    /**
     * Strips any whitespace characters from the end of string.
     *
     * @param str the string to strip
     * @return the stripped string
     */
    public static String stripEnd(final String str) {
        return StringUtils.stripEnd(str, null);
    }

    /**
     * Strips any whitespace characters from the end of strings.
     *
     * @param str the strings to strip
     * @return the list of stripped strings
     */
    public static String[] stripEnd(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = StringUtils.stripEnd(str[i], null);
        }
        return newArr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "stripEnd";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str...)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory() {
        return "Remove";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Strips any whitespace characters from the end of given "
            + "strings. "
            + ""
            + "<br/>"
            + "<br/>"
            + "<strong>Examples:</strong>"
            + "<br/>"
            + "<table>"
            + "<tr><td>stripEnd(\"&nbsp;&nbsp;KNIME&nbsp;&nbsp;&nbsp;&nbsp;"
            + "\")</td>"
            + "<td>=&nbsp;\"&nbsp;&nbsp;KNIME\"</td></tr>"

            + "<tr><td>stripEnd(\"KNIME&nbsp;&nbsp;\", "
            + "\"&nbsp;&nbsp;KNIME\")</td>"
            + "<td>=&nbsp;[\"KNIME\", \"&nbsp;&nbsp;KNIME\"]</td></tr>"

            + "<tr><td>stripEnd(null, \"\", \"a&nbsp;&nbsp;\")</td>"
            + "<td>=&nbsp;[null, \"\", \"a\"]</td></tr>"

            + "</table>"
            + "* can be any character sequence.<br/>";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return String.class;
    }

}
