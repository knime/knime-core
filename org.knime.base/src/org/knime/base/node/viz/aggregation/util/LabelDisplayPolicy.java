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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    23.12.2006 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation.util;

import org.knime.core.node.util.ButtonGroupEnumInterface;


/**
 * Enumerates all possible label display options and provides some utility
 * functions.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum LabelDisplayPolicy implements ButtonGroupEnumInterface {

    /**Display none labels at all.*/
    NONE("none", "None", "Display no labels", false),
    /**Display the label of all bars..*/
    ALL("all", "All elements", "Display all labels", false),
    /**Display the label of the selected bars.*/
    SELECTED("selected", "Selected elements",
            "Display labels only for selected elements", true);

    private final String m_id;
    private final String m_label;
    private final String m_toolTip;
    private final boolean m_default;

    private LabelDisplayPolicy(final String id, final String label,
            final String toolTip, final boolean isDefault) {
        m_id = id;
        m_label = label;
        m_toolTip = toolTip;
        m_default = isDefault;
    }

    /**
     * @return the id of the label option
     */
    public String getActionCommand() {
        return m_id;
    }

    /**
     * @return the name of the label option
     */
    public String getText() {
        return m_label;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTip() {
        return m_toolTip;
    }

    /**
     * @return the names of the label options
     */
    public static String[] getLabels() {
        final LabelDisplayPolicy[] options = values();
        final String[] names = new String[values().length];
        for (int i = 0, length = options.length; i < length; i++) {
            names[i] = options[i].getText();
        }
        return names;
    }
    /**
     * @return the default aggregation method
     */
    public static LabelDisplayPolicy getDefaultOption() {
        for (final LabelDisplayPolicy value : LabelDisplayPolicy.values()) {
            if (value.isDefault()) {
                return value;
            }
        }
        throw new IllegalStateException("No default layout defined");
    }
    /**
     * @param value the name of the option
     * @return the option itself
     */
    public static LabelDisplayPolicy getOption4ID(final String value) {
        for (final LabelDisplayPolicy type : values()) {
            if (type.getActionCommand().equals(value)) {
                return type;
            }
        }
        return getDefaultOption();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return m_default;
    }

}
