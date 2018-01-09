/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   13.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Utility class for the InFieldsTable and the OutFieldsTable.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 */
final class FieldsTableUtil {

    /** prevent the creation of instances. */
    private FieldsTableUtil() {
        // do nothing.
    }

    /**
     * Creates a renderer for the java field name.
     *
     * @return the renderer
     */
    static TableCellRenderer createJavaFieldTableCellRenderer() {
        return new JavaFieldTableCellRenderer();
    }

    /**
     * Creates a renderer for the java type.
     *
     * @return the renderer
     */
    static TableCellRenderer createJavaTypeTableCellRenderer() {
        return new JavaTypeTableCellRenderer();
    }

    /**
     * Creates an editor for the java type.
     *
     * @return the editor
     */
    static TableCellEditor createJavaTypeTableCellEditor() {
        JavaTypeTableCellEditor editor = new JavaTypeTableCellEditor();
        editor.setClickCountToStart(2);
        return editor;
    }


    /**
     * Checks whether the given String is a valid java identifier.
     *
     * @param s the string to check.
     * @return true when s is a valid java identifier.
     */
    static boolean isValidJavaIdentifier(final String s) {
        // an empty or null string cannot be a valid identifier
        if (s == null || s.length() == 0) {
            return false;
        }

        char[] c = s.toCharArray();
        if (!Character.isJavaIdentifierStart(c[0])) {
            return false;
        }

        for (int i = 1; i < c.length; i++) {
            if (!Character.isJavaIdentifierPart(c[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a java variable identifier that is unique among the given list
     * of identifiers. The identifier is retrieved from the name of the given
     * column.
     * @param str the column
     * @param taken the list of already taken identifiers
     * @param prefix a prefix for the variable name
     * @return a java identifier
     */
    static String createUniqueJavaIdentifier(final String str,
            final Set<String> taken, final String prefix) {
        char[] c = str.toCharArray();
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        int i = 0;
        final int identifierLength = 100;
        while (builder.length() < identifierLength && i < c.length) {
            if (Character.isJavaIdentifierPart(c[i])) {
                builder.append(c[i]);
            }
            i++;
        }
        String baseName = builder.toString();
        String name = baseName;
        boolean isDuplicate = taken.contains(name);
        i = 1;
        while (isDuplicate) {
            name = baseName + "_" + i;
            isDuplicate = taken.contains(name);
            i++;
        }
        return name;
    }


     /** Renders the table cells defining the java field names.
     * Paints duplicated field names in red.
     */
    @SuppressWarnings("serial")
    private static class JavaFieldTableCellRenderer
            extends DefaultTableCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object value, final boolean isSelected,
                final boolean hasFocus, final int row,
                final int column) {
            // reset values which maybe changed by previous calls of this method
            setForeground(table.getForeground());
            setBackground(table.getBackground());

            if (value instanceof JavaToDataCellConverterFactory<?>) {
                final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                if (factory.getName().isEmpty()) {
                    setText(factory.getSourceType().getSimpleName());
                } else {
                    setText(factory.getSourceType().getSimpleName() + " (" + factory.getName() + ")");
                }
                setIcon(factory.getDestinationType().getIcon());
            } else if (value instanceof DataCellToJavaConverterFactory) {
                final DataCellToJavaConverterFactory<?, ?> factory = (DataCellToJavaConverterFactory<?, ?>)value;
                if (factory.getName().isEmpty()) {
                    setText(factory.getDestinationType().getSimpleName());
                } else {
                    setText(factory.getDestinationType().getSimpleName() + " (" + factory.getName() + ")");
                }
            } else {
                // let super class do the first step
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            FieldsTableModel model = (FieldsTableModel)table.getModel();
            if (model.isValidValue(row, column)) {
                setToolTipText(null);
            } else {
                setBackground(reddishBackground());
                setToolTipText(model.getErrorMessage(row, column));
            }
            return this;
        }

        private Color reddishBackground() {
            Color b = getBackground();
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2,
                    b.getBlue() / 2);
        }
    }

    /** Editor for the table cells defining the java type. */
    @SuppressWarnings("serial")
    private static class JavaTypeTableCellRenderer
            extends DefaultTableCellRenderer {

        public JavaTypeTableCellRenderer() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object v, final boolean isSelected,
                final boolean hasFocus, final int row,
                final int column) {
            // reset values which maybe changed by previous calls of this method
            setForeground(table.getForeground());
            setBackground(table.getBackground());

            Object value = v;
            if (value instanceof String) {
                // try to find a converter factory with id matching the given String
                Optional<DataCellToJavaConverterFactory<?, ?>> factory =
                    ConverterUtil.getDataCellToJavaConverterFactory((String)value);

                if (factory.isPresent()) {
                    value = factory.get();
                }
            }

            /* if this is still a string, no DataCellToJavaConverterFactory was found. Might still be JavaToDataCellConverterFactory */
            if (value instanceof String) {
                // try to find a converter factory with id matching the given String
                Optional<JavaToDataCellConverterFactory<?>> factory = ConverterUtil.getJavaToDataCellConverterFactory((String)value);

                if (factory.isPresent()) {
                    value = factory.get();
                }
            }

            String text = "null";
            if (value instanceof DataCellToJavaConverterFactory) {
                final DataCellToJavaConverterFactory<?, ?> factory = (DataCellToJavaConverterFactory<?, ?>)value;
                text = factory.getName();
            } else if (value instanceof JavaToDataCellConverterFactory) {
                final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                text = factory.getName();
            } else if (value instanceof Class) {
                Class javaType = (Class)value;
                text = javaType.getSimpleName();
            } else if (value != null) {
                text = value.toString();
            }

            // let super class do the first step
            super.getTableCellRendererComponent(table,
                    text,
                    isSelected, hasFocus,
                    row, column);

            FieldsTableModel model = (FieldsTableModel)table.getModel();
            if (model.isValidValue(row, column)) {
                setToolTipText(text);
            } else {
                setBackground(reddishBackground());
                setToolTipText(model.getErrorMessage(row, column));
            }

            return this;
        }

        private Color reddishBackground() {
            Color b = getBackground();
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2,
                    b.getBlue() / 2);
        }
    }

    /** Renders the table cells defining the java type. */
    @SuppressWarnings("serial")
    private static class JavaTypeTableCellEditor
            extends DefaultCellEditor {

        public JavaTypeTableCellEditor() {
            super(new JComboBox());
            JComboBox comboBox = (JComboBox)editorComponent;
            comboBox.setRenderer(new ConverterFactoryJavaTypeListCellRenderer());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(final JTable table,
                final Object value, final boolean isSelected,
                final int row, final int column) {
            JComboBox comboBox = (JComboBox)editorComponent;
            FieldsTableModel model = (FieldsTableModel)table.getModel();
            comboBox.setModel(new DefaultComboBoxModel(model.getAllowedJavaTypes(row)));

            return super.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
        }


    }

    /**
     * Test whether the given name is allowed for flow variables. Flow variables
     * are i.e. not allowed to start with "knime."
     *
     * @param name the name the name
     * @return true when give name is valid
     */
    static boolean verifyNameOfFlowVariable(final String name) {
        try {
            // test if a flow variable of this name might be
            // created. verifyName throws the package private
            // exception: IllegalFlowObjectStackException
            new FlowVariable(name, "").getScope().verifyName(name);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
