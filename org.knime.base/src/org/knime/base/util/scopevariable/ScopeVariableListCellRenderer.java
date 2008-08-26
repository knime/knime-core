/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 26, 2008 (wiswedel): created
 */
package org.knime.base.util.scopevariable;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.workflow.ScopeVariable;

/**
 * List cell renderer for lists whose elements are of type 
 * {@link ScopeVariable}. It will show the name of the variable along with an
 * icon representing the type.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ScopeVariableListCellRenderer extends DefaultListCellRenderer {
    
    /** {@inheritDoc} */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        Component c =
                super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
        if (value instanceof ScopeVariable) {
            ScopeVariable v = (ScopeVariable)value;
            Icon icon;
            setText(v.getName());
            String curValue;
            switch (v.getType()) {
            case DOUBLE:
                icon = DoubleValue.UTILITY.getIcon();
                curValue = Double.toString(v.getDoubleValue());
                break;
            case INTEGER:
                icon = IntValue.UTILITY.getIcon();
                curValue = Integer.toString(v.getIntValue());
                break;
            case STRING:
                icon = StringValue.UTILITY.getIcon();
                curValue = v.getStringValue();
                break;
            default:
                icon = DataValue.UTILITY.getIcon();
                curValue = v.toString();
            }
            setIcon(icon);
            setToolTipText(v.getName() + " (current value: " + curValue + ")");
        } else {
            setToolTipText(null);
        }
        return c;
    }

}
