/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
