/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
