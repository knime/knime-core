/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 26, 2008 (wiswedel): created
 */
package org.knime.core.node.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;

import org.knime.core.data.DataValue;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;

/**
 * List cell renderer for lists whose elements are of type 
 * {@link FlowVariable}. It will show the name of the variable along with an
 * icon representing the type.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class FlowVariableListCellRenderer extends DefaultListCellRenderer {
    
    /** Icon representing double flow variables. */
    public static final Icon FLOW_VAR_DOUBLE_ICON;
    /** Icon representing integer flow variables. */
    public static final Icon FLOW_VAR_INT_ICON;
    /** Icon representing string flow variables. */
    public static final Icon FLOW_VAR_STRING_ICON;
    
    static {
        FLOW_VAR_DOUBLE_ICON = loadIcon(
                Node.class, "/icon/flowvar_double.png");
        FLOW_VAR_INT_ICON = loadIcon(
                Node.class, "/icon/flowvar_integer.png");
        FLOW_VAR_STRING_ICON = loadIcon(
                Node.class, "/icon/flowvar_string.png");
    }
    
    private static Icon loadIcon(
            final Class<?> className, final String path) {
        ImageIcon icon;
        try {
            ClassLoader loader = className.getClassLoader(); 
            String packagePath = 
                className.getPackage().getName().replace('.', '/');
            String correctedPath = path;
            if (!path.startsWith("/")) {
                correctedPath = "/" + path;
            }
            icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
        } catch (Exception e) {
            NodeLogger.getLogger(FlowVariableListCellRenderer.class).debug(
                    "Unable to load icon at path " + path, e);
            icon = null;
        }
        return icon;
    }        
    
    /** {@inheritDoc} */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        Component c =
                super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
        if (value instanceof FlowVariable) {
            FlowVariable v = (FlowVariable)value;
            Icon icon;
            setText(v.getName());
            String curValue;
            switch (v.getType()) {
            case DOUBLE:
                icon = FLOW_VAR_DOUBLE_ICON;
                curValue = Double.toString(v.getDoubleValue());
                break;
            case INTEGER:
                icon = FLOW_VAR_INT_ICON;
                curValue = Integer.toString(v.getIntValue());
                break;
            case STRING:
                icon = FLOW_VAR_STRING_ICON;
                curValue = v.getStringValue();
                break;
            default:
                icon = DataValue.UTILITY.getIcon();
                curValue = v.toString();
            }
            setIcon(icon);
            StringBuilder b = new StringBuilder(v.getName());
            b.append(" (");
            if (v.getName().startsWith("knime.")) { // constant
                b.append("constant: ");
            } else {
                b.append("current value: ");
            }
            b.append(curValue);
            b.append(")");
            setToolTipText(b.toString());
        } else {
            setToolTipText(null);
        }
        return c;
    }

}
