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
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getText();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof StringIconOption)) {
            return false;
        }
        StringIconOption other = (StringIconOption)obj;
        boolean iconEqal = false;
        if (getIcon() == other.getIcon()) {
            iconEqal = true;
        } else if (getIcon() != null && getIcon().equals(other.getIcon())) {
            iconEqal = true;
        } else {
            return false;
        }
        boolean textEqual = false;
        if (getText() == other.getText()) {
            textEqual = true;
        } else if (getText() != null && getText().equals(other.getText())) {
            textEqual = true;
        }
        return iconEqal && textEqual;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        if (getText() != null) {
            result = getText().hashCode(); 
        }
        if (getIcon() != null) {
            result ^= getIcon().hashCode();
        } 
        return result;
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
