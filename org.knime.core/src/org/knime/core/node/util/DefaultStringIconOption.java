/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *    27.11.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import java.util.Collection;

import javax.swing.Icon;


/**
 * Default implementation of the {@link StringIconOption} interface which is
 * used in the default dialog components itself.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DefaultStringIconOption implements StringIconOption {

    private final String m_text;

    private final Icon m_icon;


    /**Constructor for class DialogComponentStringSelection.StringOption
     * without icon.
     * @param text the text to display
     */
    public DefaultStringIconOption(final String text) {
        this(text, null);
    }


    /**Constructor for class DialogComponentStringSelection.StringOption.
     * @param text the text to display
     * @param icon the optional icon to display
     *
     */
    public DefaultStringIconOption(final String text, final Icon icon) {
        if (text == null) {
            throw new NullPointerException("Text must not be null");
        }
        m_text = text;
        m_icon = icon;
    }
    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return m_icon;
    }

    /**
     * {@inheritDoc}
     */
    public String getText() {
        return m_text;
    }

    /**
     * Helper method to create a {@link StringIconOption} array from a
     * <code>String</code> <code>Collection</code>.
     * @param items the <code>String</code> <code>Collection</code> to create
     * the StringIconOption array of
     * @return the StringIconOption array
     */
    public static StringIconOption[] createOptionArray(
            final Collection<String> items) {
        if (items == null || items.size() < 1) {
            return new StringIconOption[0];
        }
        final StringIconOption[] options =
            new StringIconOption[items.size()];
        int idx = 0;
        for (final String s : items) {
            final DefaultStringIconOption option =
                new DefaultStringIconOption(s);
            options[idx++] = option;
        }
        return options;
    }
}
