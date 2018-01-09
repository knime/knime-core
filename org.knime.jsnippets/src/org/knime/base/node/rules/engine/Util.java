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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.04.23. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.awt.Component;
import java.text.ParseException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import org.jdesktop.xswingx.PromptSupport;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;

/**
 * Some helper methods somewhat related to Rule Engine.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public final class Util {
    private Util() {
        super();
    }

    /**
     * Selects the corresponding {@link DataType} that belongs to the flow variable {@link Type}.
     *
     * @param type A flow variable {@link Type}.
     * @return The corresponding {@link DataType}.
     */
    public static DataType toDataType(final Type type) {
        switch (type) {
            case DOUBLE:
                return DoubleCell.TYPE;
            case INTEGER:
                return IntCell.TYPE;
            case STRING:
                return StringCell.TYPE;
            default:
                throw new UnsupportedOperationException("Unknown flow variable type: " + type);
        }
    }

    private static final Map<? extends Character, ? extends Type> flowVarTypePrefixChars;

    private static final Map<? extends Type, ? extends Character> typeToPrefices;
    static {
        final Map<Character, Type> flowVarMap = new LinkedHashMap<Character, FlowVariable.Type>();
        final EnumMap<Type, Character> reverseMap = new EnumMap<Type, Character>(Type.class);
        for (final Type type : FlowVariable.Type.values()) {
            assert type != null;
            final char key;
            final Type val;
            switch (type) {
                case DOUBLE:
                    key = 'D';
                    val = Type.DOUBLE;
                    break;
                case INTEGER:
                    key = 'I';
                    val = Type.INTEGER;
                    break;
                case STRING:
                    key = 'S';
                    val = Type.STRING;
                    break;
                case CREDENTIALS:
                    continue;
                default:
                    throw new IllegalStateException("Unhandled workflow variable type: " + type);
            }
            flowVarMap.put(key, val);
            reverseMap.put(val, key);
        }
        flowVarTypePrefixChars = Collections.unmodifiableMap(flowVarMap);
        typeToPrefices = Collections.unmodifiableMap(reverseMap);
    }

    /**
     * @return The flow variable type describing prefix characters.
     */
    public static Set<? extends Character> getFlowVarTypePrefixChars() {
        return flowVarTypePrefixChars.keySet();
    }

    /**
     * @return The map from flow variable {@link Type}s to their representing {@link Character}s.
     */
    public static Map<? extends Type, ? extends Character> getTypeToPrefices() {
        return typeToPrefices;
    }

    /**
     * @return the flow variable type-prefix chars
     */
    public static Map<? extends Character, ? extends Type> getFlowVarTypePrefixCharMap() {
        return flowVarTypePrefixChars;
    }

    /**
     * Clones a map recursively. Each included maps in the keys or values will also be cloned.
     *
     * @param input A {@link Map}.
     * @return The cloned value.
     * @param <K> Key type.
     * @param <V> Value type.
     */
    static <K, V> Map<K, V> clone(final Map<K, V> input) {
        if (input.isEmpty()) {
            return new HashMap<K, V>();
        }
        Map<K, V> ret = new HashMap<K, V>();
        for (Entry<K, V> entry : input.entrySet()) {
            K k = entry.getKey();
            V v = entry.getValue();
            if (k instanceof Map<?, ?>) {
                Map<?, ?> keyMap = (Map<?, ?>)k;
                @SuppressWarnings("unchecked")
                final K cloneAsK = (K)clone(keyMap);
                k = cloneAsK;
            }
            if (v instanceof Map<?, ?>) {
                Map<?, ?> valueMap = (Map<?, ?>)v;
                @SuppressWarnings("unchecked")
                final V cloneAsV = (V)clone(valueMap);
                v = cloneAsV;
            }
            ret.put(k, v);
        }
        return ret;
    }

    /**
     * Merges two matched object maps together. The objects from the second argument can overwrite the objects in the
     * result coming from the first. The input maps are not affected, this method will create new references to the
     * contained objects.
     *
     * @param first Some matched objects.
     * @param second Other matched objects.
     * @return A new matched objects {@link Map}.
     */
    public static Map<String, Map<String, String>> mergeObjects(final Map<String, Map<String, String>> first,
                                                                final Map<String, Map<String, String>> second) {
        if (first.isEmpty() && second.isEmpty()) {
            return new HashMap<String, Map<String,String>>();
        }
        Map<String, Map<String, String>> ret = clone(first);
        for (Entry<String, Map<String, String>> entry : second.entrySet()) {
            if (ret.containsKey(entry.getKey())) {
                ret.get(entry.getKey()).putAll(entry.getValue());
            } else {
                ret.put(entry.getKey(), clone(entry.getValue()));
            }
        }
        return ret;
    }

    /**
     * A {@link Math#signum(double)} function with int arguments and results.
     *
     * @param value An int value.
     * @return {@code 0} if the are the same, {@code -1} if it is below {@code 0}, else {@code 1}.
     */
    public static int signum(final int value) {
        if (value == 0) {
            return 0;
        }
        if (value < 0) {
            return -1;
        }
        return 1;
    }

    /**
     * Finds a component with the type/class {@code cls} within the array of {@link Component}s.
     *
     * @param components Some {@link Component}s.
     * @param cls A class, belonging to a subclass of {@link Component}.
     * @return The first element of {@code components} which is instance of {@code cls}, else {@code null}.
     * @param <T> {@link Component}'s type.
     */
    public static <T extends Component> T findComponent(final Component[] components, final Class<? extends T> cls) {
        for (int i = 0; i < components.length; i++) {
            if (cls.isInstance(components[i])) {
                return cls.cast(components[i]);
            }
        }
        return null;
    }

    /**
     * Creates a new text field with prompt ({@link PromptSupport}, aka watermark). It also adds copy/cut/paste actions
     * to the context menu.
     *
     * @param watermark The text to be shown when nothing entered.
     * @param colWidth The number of columns that is visible in preferred size of the {@link JTextField}.
     * @param label The tooltip. Can be {@code null}.
     * @return A new {@link JTextField}.
     */
    public static JTextField
            createTextFieldWithWatermark(final String watermark, final int colWidth, final String label) {
        JTextField comp = new JTextField(/*watermark,*/colWidth);
        PromptSupport.init(watermark, null, null, comp);
        if (label != null) {
            comp.setToolTipText(label);
        } else if (watermark != null && !watermark.isEmpty()) {
            comp.setToolTipText(watermark);
        }
        addCopyCutPaste(comp);
        return comp;
    }

    /**
     * Adds the Copy/Cut/Paste actions to the {@code component}'s context menu.
     *
     * @param component A {@link JTextComponent}.
     */
    public static void addCopyCutPaste(final JTextComponent component) {
        JPopupMenu popup = component.getComponentPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu("Test");
            component.setComponentPopupMenu(popup);
        }
        boolean pasteFound = false, cutFound = false, copyFound = false;
        for (MenuElement menu : popup.getSubElements()) {
            if (menu.getComponent() instanceof JMenuItem) {
                JMenuItem item = (JMenuItem)menu.getComponent();
                final String name = item.getName();
                pasteFound |= "Paste".equals(name);
                cutFound |= "Cut".equals(name);
                copyFound |= "Copy".equals(name);
            }
        }
        if (!copyFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.CopyAction()));
        }
        if (!cutFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.CutAction()));
        }
        if (!pasteFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.PasteAction()));
        }
    }

    /**
     * @param action An {@link Action}.
     * @return A new {@link JMenuItem} with the specified {@link Action}.
     */
    private static JMenuItem createNamedMenuItem(final Action action) {
        final JMenuItem menuItem = new JMenuItem(action);
        return menuItem;
    }

    /**
     * A method to escape the flow variable name.
     *
     * @param fv A {@link FlowVariable}.
     * @return The escaped {@link FlowVariable#getName()}.
     * @see #getFlowVarTypePrefixChars()
     */
    public static String escapeFlowVariableName(final FlowVariable fv) {
        return "$${" + getTypeToPrefices().get(fv.getType()) + fv.getName() + "}$$";
    }

    /**
     * Creates an {@link ExpressionValue} based on the {@code var} {@link FlowVariable}.
     *
     * @param var A {@link FlowVariable}.
     * @return The {@link ExpressionValue} representing {@code var}.
     */
    static ExpressionValue readFlowVarToExpressionValue(final FlowVariable var) {
        Map<String, Map<String, String>> emptyMap = Collections.<String, Map<String, String>> emptyMap();
        switch (var.getType()) {
            case DOUBLE:
                return new ExpressionValue(new DoubleCell(var.getDoubleValue()), emptyMap);
            case INTEGER:
                return new ExpressionValue(new IntCell(var.getIntValue()), emptyMap);
            case STRING:
                return new ExpressionValue(new StringCell(var.getStringValue()), emptyMap);
            default:
                return null;
        }

    }

    /**
     * @param type A flow variable {@link Type}.
     * @return The class that can be used by {@link FlowVariableProvider} implementations for the actual {@code type}.
     */
    public static Class<?> flowVarTypeToClass(final Type type) {
        switch (type) {
            case DOUBLE:
                return Double.class;
            case INTEGER:
                return Integer.class;
            case STRING:
                return String.class;
            default:
                throw new UnsupportedOperationException("Unknown flow variable type: " + type);
        }
    }

    /**
     * @param n An int.
     * @return A {@link String} of {@code max(0, n)} spaces.
     */
    public static String nSpaces(final int n) {
        StringBuilder ret = new StringBuilder();
        for (int i = n; i-- > 0;) {
            ret.append(' ');
        }
        return ret.toString();
    }

    /**
     * Adds some details/context to the error message of the exception.
     *
     * @param pe The original {@link ParseException}.
     * @param line The line with problem.
     * @param lineNo The line number.
     * @return A new {@link ParseException} with a bit more context coded into the message.
     */
    public static ParseException addContext(final ParseException pe, final String line, final int lineNo) {
        final ParseException ret =
            new ParseException("Line: " + lineNo + ": " + pe.getMessage() + "\n" + line + "\n"
                + nSpaces(pe.getErrorOffset()) + "^", pe.getErrorOffset());
        ret.setStackTrace(pe.getStackTrace());
        return ret;
    }
}
