/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provide a standard component for a dialog that allows to select a string from
 * a list of strings.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class DialogComponentStringSelection extends DialogComponent {

    private JComboBox m_combobox;

    /**
     * Constructor that puts label and combobox into panel.
     * 
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list of items for the combobox
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final List<String> list) {
        super(stringModel);

        this.add(new JLabel(label));
        m_combobox = new JComboBox();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (String s : list) {
                    m_combobox.addItem(s);
                }
            }
        });
        this.add(m_combobox);

        // we do not change the model when the selection changes. We update
        // the settings model right before save.

        // we need to update the selection, when the model changes.
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String val = ((SettingsModelString)getModel())
                                .getStringValue();
                        m_combobox.setSelectedItem(val);
                    }
                });
            }
        });

    }

    /**
     * Constructor that puts label and combobox into panel.
     * 
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list of items for the combobox
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final String... list) {
        this(stringModel, label, Arrays.asList(list));
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        // we transfer the value from the field into the model
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    ((SettingsModelString)getModel())
                            .setStringValue((String)m_combobox
                                    .getSelectedItem());
                }

            });
        } catch (final InterruptedException e) {
            throw new InvalidSettingsException(e);
        } catch (final InvocationTargetException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * @see DialogComponent #setEnabledComponents(boolean)
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_combobox.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     * 
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_combobox.setPreferredSize(new Dimension(width, height));
    }

}
