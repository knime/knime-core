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
 * ---------------------------------------------------------------------
 *
 * Created on 08.11.2012 by hofer
 */
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.border.LineBorder;

/**
 * A {@link DataColumnSpecListCellRenderer} intended to be used for
 * {@link JComboBox}. A default value can be set which is displayed when no
 * item is selected.
 *
 * @author Heiko Hofer
 * @since 2.7
 */
public class ColumnComboBoxRenderer extends DataColumnSpecListCellRenderer {
    private static final long serialVersionUID = 6360911508907991064L;

    private JComboBox m_comboBox;
    private String m_defaultValue;

    /**
     * Create a ListCellRenderer intended to be used for a JComboBox.
     *
     */
    public ColumnComboBoxRenderer() {
        super();
        m_defaultValue = "Choose ...";
    }

    /**
     * Attach this renderer to the given JComboBox.
     * @param comboBox the JComboBox this renderer will be attached.
     */
    public void attachTo(final JComboBox comboBox) {
        m_comboBox = comboBox;
        m_comboBox.setRenderer(this);
    }

    /**
     * Get the default value. The default value is displayed when nothing is
     * selected.
     */
    public String getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * Set the default value. The default value is displayed when nothing is
     * selected.
     * @param defaultValue the default value, displayed when nothing is selected
     */
    public void setDefaultValue(final String defaultValue) {
        m_defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
              final Object value, final int index, final boolean isSelected,
              final boolean cellHasFocus) {

        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (m_comboBox != null) {
            if (m_comboBox.getSelectedIndex() < 0 && value == null) {
                m_comboBox.setBorder(new LineBorder(Color.red));
                setText(m_defaultValue);
            } else {
                m_comboBox.setBorder(null);
            }
        }
        return this;
    }
}
