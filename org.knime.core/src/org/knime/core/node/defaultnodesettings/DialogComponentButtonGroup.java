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
 *    27.04.2007 (Tobias Koetter): created
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;


/**
 * A standard component to display radio buttons. The given
 * {@link SettingsModelString} holds the value of the
 * <code>getActionCommand()</code> of the selected
 * {@link ButtonGroupEnumInterface}.
 * @author Tobias Koetter, University of Konstanz
 */
public class DialogComponentButtonGroup extends DialogComponent {

    private final ButtonGroup m_buttonGroup;

    /**Constructor for class DialogComponentButtonGroup.
     * @param stringModel the model that stores the action command of the
     * selected radio button
     * @param label the optional label of the group. Set to <code>null</code>
     * for none label. Set an empty <code>String</code> for a border.
     * @param vertical set to <code>true</code> to have the box in a vertical
     * orientation
     * @param elements the buttons of the group
     */
    public DialogComponentButtonGroup(final SettingsModelString stringModel,
            final String label, final boolean vertical,
            final ButtonGroupEnumInterface[] elements) {
        super(stringModel);
        if (elements == null || elements.length < 1) {
            throw new IllegalArgumentException("Elements must not be null");
        }
        final Set<String> uniqueness = new HashSet<String>(elements.length);
        for (final ButtonGroupEnumInterface element : elements) {
            if (!uniqueness.add(element.getActionCommand())) {
                throw new IllegalArgumentException("Duplicate action command "
                        + "found in elements");
            }
        }
        m_buttonGroup = createEnumButtonGroup(elements, new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateModel();
            }
        });

        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        final Box buttonBox =
            createButtonGroupBox(m_buttonGroup, label, vertical);
        getComponentPanel().add(buttonBox);

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**Constructor for class DialogComponentButtonGroup. The
     * <code>SettingsModel</code> holds the action command of the selected
     * radio button. The default value of the <code>SettingModel</code> is
     * selected per default.
     * @param stringModel the model that stores the action command of the
     * selected radio button
     * @param label the optional label of the group. Set to <code>null</code>
     * for none label. Set an empty <code>String</code> for a border.
     * @param vertical set to <code>true</code> to have the box in a vertical
     * orientation
     * @param buttonLabels the labels of the buttons
     * @param actionCommands the action command of the buttons in the same order
     * like the labels
     */
    public DialogComponentButtonGroup(final SettingsModelString stringModel,
            final String label, final boolean vertical,
            final String[] buttonLabels, final String[] actionCommands) {
        this(stringModel, label, vertical, buttonLabels, actionCommands,
                stringModel.getStringValue());
    }

    /**Constructor for class DialogComponentButtonGroup. The
     * <code>SettingsModel</code> holds the action command of the selected
     * radio button.
     * @param stringModel the model that stores the action command of the
     * selected radio button
     * @param label the optional label of the group. Set to <code>null</code>
     * for none label. Set an empty <code>String</code> for a border.
     * @param vertical set to <code>true</code> to have the box in a vertical
     * orientation
     * @param buttonLabels the labels of the buttons
     * @param actionCommands the action command of the buttons in the same order
     * like the labels
     * @param defaultAction the default action which should be selected
     * @deprecated use
     * {@link DialogComponentButtonGroup#DialogComponentButtonGroup(SettingsModelString, String, boolean, String[], String[])}
     * instead
     */
    @Deprecated
    public DialogComponentButtonGroup(final SettingsModelString stringModel,
            final String label, final boolean vertical,
            final String[] buttonLabels, final String[] actionCommands,
            final String defaultAction) {
        super(stringModel);
        if (buttonLabels == null || buttonLabels.length < 1) {
            throw new IllegalArgumentException("Labels must not be empty");
        }
        if (actionCommands == null || actionCommands.length < 1) {
            throw new IllegalArgumentException(
                    "ActionCommands must not be empty");
        }
        if (buttonLabels.length != actionCommands.length) {
            throw new IllegalArgumentException(
                    "Labels and actionCommands must be of equal size");
        }
        final Set<String> uniqueness =
            new HashSet<String>(actionCommands.length);
        for (final String element : actionCommands) {
            if (!uniqueness.add(element)) {
                throw new IllegalArgumentException(
                        "Duplicate action command found");
            }
        }
        m_buttonGroup =
            createEnumButtonGroup(buttonLabels, actionCommands, defaultAction,
                    new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateModel();
            }
        });

        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        final Box buttonBox =
            createButtonGroupBox(m_buttonGroup, label, vertical);
        getComponentPanel().add(buttonBox);
    }
    /**Constructor for class DialogComponentButtonGroup. The given
     * <code>SettingsModel</code> holds the selected element. The default
     * value of the <code>SettingModel</code> is selected per default.
     * @param stringModel the model that stores the action command of the
     * selected radio button
     * @param label the optional label of the group. Set to <code>null</code>
     * for none label. Set an empty <code>String</code> for a border.
     * @param vertical set to <code>true</code> to have the box in a vertical
     * orientation
     * @param elements the labels/action commands of the buttons
     */
    public DialogComponentButtonGroup(final SettingsModelString stringModel,
            final boolean vertical, final String label,
            final String... elements) {
        this(stringModel, label, vertical, stringModel.getStringValue(),
                elements);
    }
    /**Constructor for class DialogComponentButtonGroup. The given
     * <code>SettingsModel</code> holds the selected element.
     * @param stringModel the model that stores the action command of the
     * selected radio button
     * @param label the optional label of the group. Set to <code>null</code>
     * for none label. Set an empty <code>String</code> for a border.
     * @param vertical set to <code>true</code> to have the box in a vertical
     * orientation
     * @param defaultElement the default element which should be selected
     * @param elements the labels/action commands of the buttons
     * @deprecated use {@link DialogComponentButtonGroup
     *      #DialogComponentButtonGroup(SettingsModelString, boolean, String,
     *      String...)}
     * instead
     */
    @Deprecated
    public DialogComponentButtonGroup(final SettingsModelString stringModel,
            final String label, final boolean vertical,
            final String defaultElement,  final String... elements) {
        super(stringModel);
        if (elements == null || elements.length < 1) {
            throw new IllegalArgumentException("Elements must not be empty");
        }
        final String[] actionCommands = new String[elements.length];
        System.arraycopy(elements, 0, actionCommands, 0, elements.length);
        final Set<String> uniqueness =
            new HashSet<String>(actionCommands.length);
        for (final String element : actionCommands) {
            if (!uniqueness.add(element)) {
                throw new IllegalArgumentException(
                        "Duplicate action command found");
            }
        }
        m_buttonGroup =
            createEnumButtonGroup(elements, actionCommands, defaultElement,
                    new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateModel();
            }
        });

        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        final Box buttonBox =
            createButtonGroupBox(m_buttonGroup, label, vertical);
        getComponentPanel().add(buttonBox);
    }

    /**
     * Creates a <code>ButtonGroup</code> with the given elements as buttons.
     * @param elements the elements of the button group
     * @return the button group with all given elements as buttons
     */
    private static ButtonGroup createEnumButtonGroup(
            final ButtonGroupEnumInterface[] elements, final ActionListener l) {
        final ButtonGroup group = new ButtonGroup();
        boolean defaultFound = false;
        for (final ButtonGroupEnumInterface element : elements) {
            final JRadioButton button = new JRadioButton(element.getText());
            button.setActionCommand(element.getActionCommand());
            if (element.isDefault()) {
                button.setSelected(true);
                defaultFound = true;
            }
            if (element.getToolTip() != null) {
                button.setToolTipText(element.getToolTip());
            }
            if (l != null) {
                button.addActionListener(l);
            }
            group.add(button);
        }
        if (!defaultFound && group.getButtonCount() > 0) {
            //select the first button if none is by default selected
            group.getElements().nextElement().setSelected(true);
        }
        return group;
    }

    /**
     * Creates a <code>ButtonGroup</code> with the labels elements as button
     * labels and the actionCommands as the button action commands.
     * @param labels the labels of the buttons
     * @param actionCommands the action commands of the buttons in the same
     * order like the labels
     * @param defaultAction the default action which should be selected
     * @param l the action listener to add to each button
     * @return the button group
     */
    private static ButtonGroup createEnumButtonGroup(final String[] labels,
            final String[] actionCommands, final String defaultAction,
            final ActionListener l) {
        final ButtonGroup group = new ButtonGroup();
        boolean defaultFound = false;
        for (int i = 0, length = actionCommands.length; i < length; i++) {
            final JRadioButton button = new JRadioButton(labels[i]);
            button.setActionCommand(actionCommands[i]);
            if (defaultAction != null
                    && actionCommands[i].equals(defaultAction)) {
                button.setSelected(true);
                defaultFound = true;
            }
            if (l != null) {
                button.addActionListener(l);
            }
            group.add(button);
        }
        if (!defaultFound && group.getButtonCount() > 0) {
            //select the first button if none is by default selected
            group.getElements().nextElement().setSelected(true);
        }
        return group;
    }


    /**
     * Creates a <code>Box</code> with the buttons of the given
     * <code>ButtonGroup</code>. Surrounded by a border if the label is
     * not null.
     * @param group the <code>ButtonGroup</code> to create the box with
     * @param label the optional label of the group Set to <code>null</code>
     * for none label
     * @param set to <code>true</code> to have the box in a vertical
     * orientation
     * @return a <code>Box</code> with all buttons of the given
     * <code>ButtonGroup</code>
     */
    private static Box createButtonGroupBox(final ButtonGroup group,
              final String label, final boolean vertical) {
          Box buttonBox = null;
          if (vertical) {
              buttonBox = Box.createVerticalBox();
              buttonBox.add(Box.createVerticalGlue());
          } else {
              buttonBox = Box.createHorizontalBox();
              buttonBox.add(Box.createHorizontalGlue());
          }
          if (label != null) {
              buttonBox.setBorder(BorderFactory.createTitledBorder(
                      BorderFactory.createEtchedBorder(), label));
          }
          for (final Enumeration<AbstractButton> buttons = group.getElements();
              buttons.hasMoreElements();) {
              final AbstractButton button = buttons.nextElement();
              buttonBox.add(button);
              if (vertical) {
                  buttonBox.add(Box.createVerticalGlue());
              } else {
                  buttonBox.add(Box.createHorizontalGlue());
              }
          }
          return buttonBox;
      }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) {
        //nothing to check
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        final Enumeration<AbstractButton> buttons = m_buttonGroup.getElements();
        while (buttons.hasMoreElements()) {
            buttons.nextElement().setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        final Enumeration<AbstractButton> buttons = m_buttonGroup.getElements();
        while (buttons.hasMoreElements()) {
            buttons.nextElement().setToolTipText(text);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final String val = ((SettingsModelString)getModel()).getStringValue();
        final ButtonModel selectedButton = m_buttonGroup.getSelection();
        final String actionCommand;
        if (selectedButton != null) {
            actionCommand = selectedButton.getActionCommand();
        } else {
            actionCommand = null;
        }
        boolean update;
        if (val == null) {
            update = (actionCommand != null);
        } else {
            update = !val.equals(actionCommand);
        }
        if (update) {
            final Enumeration<AbstractButton> buttons =
                m_buttonGroup.getElements();
            while (buttons.hasMoreElements()) {
                final AbstractButton button = buttons.nextElement();
                if (button.getActionCommand().equals(val)) {
                    button.setSelected(true);
                }
            }
        }
        // also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() {
        // we transfer the value from the button group into the model
        final ButtonModel selectedButton = m_buttonGroup.getSelection();
        final String actionCommand;
        if (selectedButton != null) {
            actionCommand = selectedButton.getActionCommand();
        } else {
            actionCommand = null;
        }
        ((SettingsModelString)getModel()).setStringValue(actionCommand);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() {
        updateModel();
    }
}
