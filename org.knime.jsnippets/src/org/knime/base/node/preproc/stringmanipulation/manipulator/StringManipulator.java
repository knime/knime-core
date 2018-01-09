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
 * Converts input to string.
 *
 * @author Heiko Hofer
 */
public class StringManipulator implements Manipulator {
    /**
     * Converts input to string.
     *
     * @param x the value to convert
     * @return the string
     */
    public static String string(final String x) {
        return x;
    }

    /**
     * Converts input to string.
     *
     * @param x the value to convert
     * @return the string
     */
    public static String string(final Integer x) {
        if (null == x) {
            return null;
        }
        return x.toString();
    }

    /**
     * Converts input to string.
     *
     * @param x the value to convert
     * @return the string
     */
    public static String string(final Long x) {
        if (null == x) {
            return null;
        }
        return x.toString();
    }

    /**
     * Converts input to string.
     *
     * @param x the value to convert
     * @return the string
     */
    public static String string(final Double x) {
        if (null == x) {
            return null;
        }
        return x.toString();
    }

    /**
     * Converts input to string.
     *
     * @param o the value to convert
     * @return the string
     */
    public static String string(final Object o) {
        if (null == o) {
            return null;
        }
        if (o instanceof String) {
            return string((String)o);
        } else if (o instanceof Integer) {
            return string((Integer)o);
        } else if (o instanceof Long) {
            return string((Long)o);
        } else if (o instanceof Double) {
            return string((Double)o);
        } else {
            return o.toString();
        }
    }

    /**
     * Converts input to string.
     *
     * @param o the value to convert
     * @return the string
     */
    public static String[] string(final Object... o) {
        if (null == o) {
            return null;
        }
        String[] s = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            s[i] = string(o[i]);
        }
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "string";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(x)";
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
        return "Convert Type";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Converts input to string. "
            + ""
            + "<br/>"
            + "<br/>"
            + "<strong>Examples:</strong>"
            + "<br/>"
            + "<table>"
            + "<tr><td>string((String)null)</td>"
            + "<td>=&nbsp;null</td></tr>"

            + "<tr><td>string(\"KNIME\")</td>"
            + "<td>=&nbsp;\"KNIME\"</td></tr>"

            + "<tr><td>string(1, 2.1, null, \"3\")</td>"
            + "<td>=&nbsp;[\"1\", \"2.1\", null, \"3\"]</td></tr>"

            + "</table>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return String.class;
    }

}
