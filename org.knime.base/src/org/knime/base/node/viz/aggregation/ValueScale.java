/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    23.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import org.knime.core.node.util.ButtonGroupEnumInterface;


/**
 * Enumeration of different value scales.
 * @author Tobias Koetter, University of Konstanz
 */
public enum ValueScale implements ButtonGroupEnumInterface {
    /**The original scaling.*/
    ORIGINAL("original", "Original", "Original scale", null),
//    /**Logarithm scaling.*/
//    LOG("Log", "Log", "Logarithm scale", null),
    /**Percentage scaling.*/
    PERCENT("Percent", "Percent", "Percentage scale", "%");

    private final String m_actionCommand;

    private final String m_text;

    private final String m_toolTip;

    private final String m_extension;

    private ValueScale(final String id, final String label,
            final String toolTip, final String extension) {
        m_actionCommand = id;
        m_text = label;
        m_toolTip = toolTip;
        m_extension = extension;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionCommand() {
        return m_actionCommand;
    }

    /**
     * {@inheritDoc}
     */
    public String getText() {
        return m_text;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTip() {
        return m_toolTip;
    }


    /**
     * @return the optional value scale extension
     */
    public String getExtension() {
        return m_extension;
    }

    /**
     * @return the default scale
     */
    public static ValueScale getDefaultMethod() {
        return ValueScale.ORIGINAL;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this.equals(ValueScale.getDefaultMethod());
    }

    /**
     * @param value the value to scale
     * @param totalValue the total value used to calculate percentage
     * @return the scaled value
     */
    public double scale(final double value, final double totalValue) {
        switch (this) {
        case PERCENT:
            if (totalValue == 0) {
                return 0;
            }
            return 100.0 / totalValue * Math.abs(value);
        default:
            return value;
        }
    }

    /**
     * Returns the scale for the given action command. If the command is
     * <code>null</code> or has length zero the method returns the default
     * scale.
     *
     * @param action the action command to check
     * @return the aggregation method with the given name
     */
    public static ValueScale getScale4Command(final String action) {
        if (action == null || action.length() < 1) {
            return getDefaultMethod();
        }
        for (final ValueScale value : ValueScale.values()) {
            if (value.getActionCommand().equals(action)) {
                return value;
            }
        }
        return getDefaultMethod();
    }

}
