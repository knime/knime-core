/*
 * -------------------------------------------------------------------
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
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
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
        for (ButtonGroupEnumInterface element : elements) {
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
    }
    
    /**
     * Creates a <code>ButtonGroup</code> with the given elements as buttons.
     * @param elements the elements of the button group
     * @return the button group with all given elements as buttons
     */
    private static ButtonGroup createEnumButtonGroup(
            final ButtonGroupEnumInterface[] elements, final ActionListener l) {
        final ButtonGroup group = new ButtonGroup();
        for (final ButtonGroupEnumInterface element : elements) {
            final JRadioButton button = new JRadioButton(element.getText());
            button.setActionCommand(element.getActionCommand());
            button.setSelected(element.isDefault());
            if (element.getToolTip() != null) {
                button.setToolTipText(element.getToolTip());
            }
            if (l != null) {
                button.addActionListener(l);
            }
            group.add(button);
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
    protected void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs) {
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
        final String actionCommand = 
            m_buttonGroup.getSelection().getActionCommand();
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
        ((SettingsModelString)getModel()).setStringValue(
                m_buttonGroup.getSelection().getActionCommand());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateStettingsBeforeSave() {
        updateModel();
    }
}
