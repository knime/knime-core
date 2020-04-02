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
 * History
 *   Apr 4, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableTypeRegistry;

/**
 * Panel that displays a single line/element of a {@link ConfigEditJTree}.
 * It is composed of a label showing the property name, a combo box to select
 * the overwriting variable from and a textfield to enter a new variable name
 * in case the property should be exposed.
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
// TODO: consider making this class package-scope (and final)
public class ConfigEditTreeNodePanel extends JPanel {
    static final int MINIMUM_LABEL_WIDTH = 100;
    static final int COMBOBOX_WIDTH = 222;

    // The number of characters we allow in the label text before we start truncating via mid-excision
    private static final int MAXIMUM_LABEL_CHARACTER_COUNT = 30;

    private static final int MINIMUM_HEIGHT = 24;
    private static final Dimension LABEL_MINIMUM_SIZE = new Dimension(MINIMUM_LABEL_WIDTH, MINIMUM_HEIGHT);
    private static final Dimension VALUE_COMBOBOX_SIZE = new Dimension(COMBOBOX_WIDTH, MINIMUM_HEIGHT);

    private static final ComboBoxElement EMPTY_COMBOBOX_ELEMENT = new ComboBoxElement(null);
    private static final Icon ICON_UNKNOWN = DataValue.UTILITY.getIcon();

    /**
     * Note from loki: Marc Bux wrote to me that there is a method in the AP code base which does this already; i
     *  spent much more time searching the codebase (to only turn up many implementations, but that all did
     *  tail-excision truncation) than i would have spent writing this method. If someone knows of the method
     *  of which Marc writes, please replace this method.
     *
     * @param original
     * @return a truncated via mid-excision string if the original is longer than {@link #MAXIMUM_LABEL_CHARACTER_COUNT}
     */
    static String displayTextForString(final String original) {
        if (original.length() > MAXIMUM_LABEL_CHARACTER_COUNT) {
            final int exciseLength = (original.length() - MAXIMUM_LABEL_CHARACTER_COUNT) + 3;
            final int exciseStart = (original.length() - exciseLength) / 2;

            return (original.substring(0, exciseStart) + "..." + original.substring(original.length() - exciseStart));
        }

        return original;
    }


    private final JLabel m_keyLabel;
    private Icon m_keyIcon;
    private final DefaultComboBoxModel<ComboBoxElement> m_valueComboBoxModel;
    private final JComboBox<ComboBoxElement> m_valueComboBox;
    private FlowObjectStack m_flowObjectStack;
    private final JTextField m_exposeAsVariableField;
    private ConfigEditTreeNode m_treeNode;

    private final ConfigEditTreeRenderer m_parentRenderer;

    // If this panel will be used as a node editor in our tree, this should be true; otherwise false (for the case
    //      in which it is only used for painting.)
    private final boolean m_panelIntendedForEditor;
    // The depth of the node in the tree which this panel repesents
    private int m_treePathDepth;
    // The visible width for that this pane should use for its total size computations
    private int m_visibleWidth = -1;

    /**
     * Constructs new panel.
     *
     * @param isForConfig if true, the combo box and the textfield are not shown (configs can't be overwritten, nor be
     *            exported as variable).
     * @param owningRenderer the renderer for which we are part of the nodes' display; we'd rather have the tree, which
     *            we get from the renderer later, but the construction time sequence of events in which the cell editor
     *            which creates two instances of this class prevents us from having access to the tree, but indeed
     *            having reference to the renderer
     * @param isIntendedForEditor true if his panel will be used as a node editor in our tree; false for the case in
     *            which it is only used for painting.
     * @since 4.2
     */
    public ConfigEditTreeNodePanel(final boolean isForConfig, final ConfigEditTreeRenderer owningRenderer,
                                   final boolean isIntendedForEditor) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(true);
        m_keyLabel = new JLabel();
        m_keyLabel.setMinimumSize(LABEL_MINIMUM_SIZE);
        m_valueComboBoxModel = new DefaultComboBoxModel<>();
        m_valueComboBox = new JComboBox<>(m_valueComboBoxModel);
        m_valueComboBox.setMinimumSize(VALUE_COMBOBOX_SIZE);
        m_valueComboBox.setPreferredSize(VALUE_COMBOBOX_SIZE);
        m_valueComboBox.setSize(VALUE_COMBOBOX_SIZE);
        m_valueComboBox.setToolTipText(" "); // enable tooltip;
        m_valueComboBox.setRenderer(ComboBoxRenderer.INSTANCE);
        final FocusListener l = new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusLost(final FocusEvent e) {
                commit();
            }
        };
        m_valueComboBox.addFocusListener(l);
        m_valueComboBox.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                onSelectedItemChange(e.getItem());
            }
        });
        m_exposeAsVariableField = new JTextField(12);
        m_exposeAsVariableField.addFocusListener(l);

        add(m_keyLabel);
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            add(Box.createHorizontalGlue());
            add(Box.createVerticalStrut(MINIMUM_HEIGHT));
        }
        if (!ConfigEditTreeRenderer.PLATFORM_IS_MAC) {
            if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
                add(Box.createHorizontalGlue());
            }
            add(Box.createHorizontalStrut(6));
        }
        if (isForConfig) {
            if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
                add(Box.createHorizontalGlue());
            }
            add(m_valueComboBox);
            if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
                add(Box.createHorizontalGlue());
            }
            if (!ConfigEditTreeRenderer.PLATFORM_IS_MAC) {
                add(Box.createHorizontalStrut(6));
                if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
                    add(Box.createHorizontalGlue());
                }
            }
            add(m_exposeAsVariableField);
            if (!ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
                m_exposeAsVariableField.setMaximumSize(m_exposeAsVariableField.getPreferredSize());
            }
        }
        if (!ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            add(Box.createHorizontalGlue());
            add(Box.createVerticalStrut(MINIMUM_HEIGHT));
        }

        m_parentRenderer = owningRenderer;

        m_panelIntendedForEditor = isIntendedForEditor;
    }

    private Collection<FlowVariable> getAllVariablesOfTypes(final VariableType<?>... types) {
        return (m_flowObjectStack != null) ? m_flowObjectStack.getAvailableFlowVariables(types).values()
                                           : Collections.emptyList();
    }

    /**
     * This sets both the depth (which is used in overriding setBounds if we are for an editor) and sets the
     *  size of the key label based on the pre-computed maximum width per depth.
     *
     * @param depth depth of the node we're representing within the tree
     */
    void setTreePathDepth(final int depth) {
        m_treePathDepth = depth;

        final int maxLabelWidth = m_parentRenderer.getParentTree().labelWidthToEnforceForDepth(m_treePathDepth);
        m_keyLabel.setSize(maxLabelWidth, MINIMUM_HEIGHT);
        m_keyLabel.setPreferredSize(new Dimension(maxLabelWidth, MINIMUM_HEIGHT));
        m_keyLabel.setMaximumSize(m_keyLabel.getPreferredSize());
        m_keyLabel.invalidate();
    }

    int computeMinimumWidth() {
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            return getPreferredSize().width;
        } else {
            return m_keyLabel.getPreferredSize().width + m_valueComboBox.getPreferredSize().width
                                + m_exposeAsVariableField.getPreferredSize().width;
        }
    }

    void setVisibleWidth(final int w) {
        m_visibleWidth = w;
    }

    @Override
    public Dimension getPreferredSize() {
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            final int insets = m_parentRenderer.getTotalWidthInsets(m_keyIcon);

            return new Dimension((m_visibleWidth - insets), (MINIMUM_HEIGHT + 4));
        } else {
            return super.getPreferredSize();
        }
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
        int widthToUse = width;
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH && m_panelIntendedForEditor) {
            final int insets = m_parentRenderer.getTotalWidthInsets(m_keyIcon);

            widthToUse = (m_visibleWidth - insets);
        }

        super.setBounds(x, y, widthToUse, height);
    }

    /**
     * Set a new tree node to display.
     *
     * @param treeNode the new node to represent (may be null).
     */
    public void setTreeNode(final ConfigEditTreeNode treeNode) {
        m_treeNode = treeNode;
        final boolean isEditable = (m_treeNode != null) && m_treeNode.isLeaf();

        String usedVariable;
        m_valueComboBox.setEnabled(isEditable);

        VariableType<?> selType = null;
        final Collection<FlowVariable> suitableVariables = new ArrayList<>();
        if (m_treeNode != null) {
            final AbstractConfigEntry entry = m_treeNode.getConfigEntry();
            selType = fillSuitableVariablesNew(suitableVariables);

            if (selType == null) {
                selType = StringType.INSTANCE;
                m_keyIcon = ICON_UNKNOWN;
            } else {
                m_keyIcon = selType.getIcon();
            }
            m_keyLabel.setText(displayTextForString(entry.getKey()));
            m_keyLabel.setToolTipText(entry.getKey());
            usedVariable = m_treeNode.getUseVariableName();
            final String exposeVariable = m_treeNode.getExposeVariableName();
            m_exposeAsVariableField.setText(exposeVariable);
        } else {
            selType = StringType.INSTANCE;
            m_keyLabel.setText("");
            m_keyLabel.setToolTipText(null);
            m_keyIcon = ICON_UNKNOWN;
            m_exposeAsVariableField.setText("");
            usedVariable = null;
        }

        m_keyLabel.setMinimumSize(LABEL_MINIMUM_SIZE);
        m_valueComboBoxModel.removeAllElements();
        m_valueComboBoxModel.addElement(EMPTY_COMBOBOX_ELEMENT);
        ComboBoxElement match = null;
        for (final FlowVariable v : suitableVariables) {
            final ComboBoxElement cbe = new ComboBoxElement(v);
            m_valueComboBoxModel.addElement(cbe);
            if (v.getName().equals(usedVariable)) {
                match = cbe;
            }
        }
        m_valueComboBox.setSize(VALUE_COMBOBOX_SIZE);
        if (!ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            m_valueComboBox.setMaximumSize(m_valueComboBox.getPreferredSize());
        }

        if ((match == null) && (m_flowObjectStack != null)) {
            final Map<String, FlowVariable> allVars
                        = m_flowObjectStack.getAllAvailableFlowVariables();
            if (allVars.containsKey(usedVariable)) {
                final FlowVariable v = allVars.get(usedVariable);
                final String error = "Variable \"" + usedVariable + "\" has wrong type (" + v.getVariableType()
                                        + "), expected " + selType;
                final ComboBoxElement cbe = new ComboBoxElement(v, error);
                m_valueComboBoxModel.addElement(cbe);
                match = cbe;
            }
        }

        if (match != null) {
            m_valueComboBox.setSelectedItem(match);
        } else if (usedVariable != null) {
            addPlaceholderForInvalidVariable(usedVariable, selType);
        }
        m_valueComboBox.setEnabled(m_valueComboBoxModel.getSize() > 1);
    }

    private VariableType<?> fillSuitableVariablesNew(final Collection<FlowVariable> suitableVariables) {
        final AbstractConfigEntry entry = m_treeNode.getConfigEntry();
        final TreeNode parent = entry.getParent();
        if (parent instanceof Config) {
            final Config config = (Config)parent;
            final String configKey = entry.getKey();
            VariableType<?>[] suitableTypes =
                VariableTypeRegistry.getInstance().getOverwritingTypes(config, configKey);
            suitableVariables.addAll(getAllVariablesOfTypes(suitableTypes));

            return VariableTypeRegistry.getInstance().getCorrespondingVariableType(config, configKey).orElse(null);
        } else {
            return null;
        }
    }

    private void addPlaceholderForInvalidVariable(final String usedVariable, final VariableType<?> selType) {
        // show name in variable in arrows; makes also sure to
        // not violate the namespace of the variable (could be
        // node-local variable, which can't be created outside
        // the workflow package)
        final String errorName = "<" + usedVariable + ">";
        final FlowVariable virtualVar;
        virtualVar = createVirtualVariableNew(selType, errorName);
        final String error = "Invalid variable \"" + usedVariable + "\"";
        final ComboBoxElement cbe = new ComboBoxElement(virtualVar, error);
        m_valueComboBoxModel.addElement(cbe);
        m_valueComboBox.setSelectedItem(cbe);
    }

    private static FlowVariable createVirtualVariableNew(final VariableType<?> selType, final String name) {
        return new FlowVariable(name, selType);
    }

    /** Write the currently edited values to the underlying model. */
    public void commit() {
        if (m_treeNode == null) {
            return;
        }
        String v = null;
        Object selVar = m_valueComboBox.getSelectedItem();
        if (selVar instanceof ComboBoxElement) {
            final ComboBoxElement cbe = (ComboBoxElement)selVar;
            if (!EMPTY_COMBOBOX_ELEMENT.equals(cbe)) {
                if (cbe.m_errorString == null) {
                    v = cbe.m_variable.getName();
                }
            }
        }
        m_treeNode.setUseVariableName(StringUtils.isNotEmpty(v) ? v : null);
        v = m_exposeAsVariableField.getText();
        m_treeNode.setExposeVariableName(StringUtils.isNotEmpty(v) ? v : null);
    }

    /**
     * Get icon to this property (string, double, int, unknown).
     *
     * @return representative icon.
     */
    public Icon getIcon() {
        return m_keyIcon;
    }

    /**
     * @param flowObjectStack the variableStack to set
     */
    public void setFlowObjectStack(final FlowObjectStack flowObjectStack) {
        m_flowObjectStack = flowObjectStack;
    }

    /**
     * @return the variableStack
     */
    public FlowObjectStack getFlowObjectStack() {
        return m_flowObjectStack;
    }

    JLabel getKeyLabel() {
        return m_keyLabel;
    }

    private void onSelectedItemChange(final Object newItem) {
        final String newToolTip;
        if (newItem instanceof ComboBoxElement) {
            final ComboBoxElement cbe = (ComboBoxElement)newItem;
            if (cbe.m_errorString != null) {
                newToolTip = cbe.m_errorString;
            } else {
                if (m_treeNode != null) {
                    // Avoid truncated display text
                    newToolTip = m_treeNode.getConfigEntry().getKey();
                } else {
                    newToolTip = m_keyLabel.getText();
                }
            }
        } else {
            if (m_treeNode != null) {
                // Avoid truncated display text
                newToolTip = m_treeNode.getConfigEntry().getKey();
            } else {
                newToolTip = m_keyLabel.getText();
            }
        }
        final String oldToolTip = getToolTipText();
        if (!Objects.equals(oldToolTip, newToolTip)) {
            setToolTipText(newToolTip);
        }
        commit();
    }


    /**
     * Elements in the combo box. Used to also indicate errors with the current selection.
     */
    private static final class ComboBoxElement {
        private final FlowVariable m_variable;
        private final String m_errorString;

        /** Create ordinary element, without error. */
        private ComboBoxElement(final FlowVariable v) {
            this(v, null);
        }

        /** Creator error element. */
        private ComboBoxElement(final FlowVariable v, final String error) {
            m_variable = v;
            m_errorString = error;
        }
    }


    /** Renderer for the combo box. */
    private static final class ComboBoxRenderer extends DefaultListCellRenderer {
        /** Instance to be used. */
        static final ComboBoxRenderer INSTANCE = new ComboBoxRenderer();

        private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED);
        private static final Border OK_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);


        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JComponent)c).setBorder(OK_BORDER);
            if (value instanceof ComboBoxElement) {
                final ComboBoxElement cbe = (ComboBoxElement)value;
                if (EMPTY_COMBOBOX_ELEMENT.equals(cbe)) {
                    setIcon(null);
                    setText(" ");
                } else {
                    final FlowVariable v = cbe.m_variable;
                    setIcon(v.getVariableType().getIcon());
                    setText(v.getName());
                    setToolTipText(v.getName() + " ("
                                        + (v.getName().startsWith("knime.")
                                                ? "constant "
                                                : "currently ")
                                        + "\"" + v.getValueAsString() + "\")");
                    if (cbe.m_errorString != null) {
                        ((JComponent)c).setBorder(ERROR_BORDER);
                        setToolTipText(cbe.m_errorString);
                    }
                }
            } else {
                setToolTipText(null);
            }
            return c;
        }
    }
}
