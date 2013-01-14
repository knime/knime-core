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
 *   Feb 2, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.UIManager;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValue.UtilityFactory;


/**
 * Renderer used in the combo box where the user can pick different
 * {@link org.knime.core.data.DataValue} class. This renderer will show the
 * name of the {@link org.knime.core.data.DataValue} class along with the
 * icon assigned to the value class as defined by the
 * {@link DataType#getUtilityFor(Class)} method.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataTypeNameRenderer extends DefaultListCellRenderer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        /* Almost all has been copied from the super implementation */
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof Class
                && DataValue.class.isAssignableFrom((Class)value)) {
            Class<? extends DataValue> type = (Class<? extends DataValue>)value;
            String s = type.getName();
            int dot = s.lastIndexOf('.');
            if (dot >= 0 && dot < s.length() - 1) {
                s = s.substring(dot + 1);
            }
            s = s.trim();
            UtilityFactory fac = DataType.getUtilityFor(type);
            Icon icon = fac.getIcon();
            setIcon(icon);
            setText(s);
        } else {
            if (value instanceof Icon) {
                setIcon((Icon)value);
                setText("");
            } else {
                setIcon(null);
                setText((value == null) ? "" : value.toString());
            }
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus) ? UIManager
                .getBorder("List.focusCellHighlightBorder") : noFocusBorder);
        return this;
    }
}
