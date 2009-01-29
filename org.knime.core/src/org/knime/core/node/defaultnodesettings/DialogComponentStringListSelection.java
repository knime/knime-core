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
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.DefaultStringIconOption;
import org.knime.core.node.util.StringIconListCellRenderer;
import org.knime.core.node.util.StringIconOption;

/**
 * Provide a standard component for a dialog that allows to select one or more
 * strings from a list of strings.
 *
 * @author Tobias Koetter, University of Konstanz
 *
 */
public final class DialogComponentStringListSelection extends DialogComponent {

    private final JList m_selectBox;

    private final DefaultListModel m_listModel;

    private final boolean m_required;

    /**
     * Constructor that puts label and select box into panel. It expects the
     * user to make a selection, thus, at least one item in the list of
     * selectable items is required. When the settings are applied, the model
     * stores all selected strings of the provided list.
     *
     * @param stringModel the model that stores the values for this component.
     * @param label the optional label of the select box.
     * Set to <code>null</code> for none label. Set an empty
     * <code>String</code> for a border.
     * @param list list of items for the select box
     */
    public DialogComponentStringListSelection(
            final SettingsModelStringArray stringModel, final String label,
            final String... list) {
        this(stringModel, label, Arrays.asList(list),
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, -1);
    }

    /**
     * Constructor that puts label and select box into panel.
     * When the settings are applied, the model
     * stores all selected strings of the provided list.
     *
     * @param stringModel the model that stores the values for this component.
     * @param label the optional label of the select box.
     * Set to <code>null</code> for none label. Set an empty
     * <code>String</code> for a border.
     * @param list list of items for the select box
     * @param required if at least one item must be selected
     * @param visibleRowCount the number of visible rows
     */
    public DialogComponentStringListSelection(
            final SettingsModelStringArray stringModel, final String label,
            final Collection<String> list, final boolean required,
            final int visibleRowCount) {
        this(stringModel, label, list,
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, required,
                visibleRowCount);
    }

    /**
     * Constructor that puts label and select box into panel.
     * When the settings are applied, the model
     * stores all selected strings of the provided list.
     * The following <code>selectionMode</code> values are allowed:
     * <ul>
     * <li> <code>ListSelectionModel.SINGLE_SELECTION</code>
     *   Only one list index can be selected at a time.  In this
     *   mode the <code>setSelectionInterval</code> and
     *   <code>addSelectionInterval</code>
     *   methods are equivalent, and only the second index
     *   argument is used.
     * <li> <code>ListSelectionModel.SINGLE_INTERVAL_SELECTION</code>
     *   One contiguous index interval can be selected at a time.
     *   In this mode <code>setSelectionInterval</code> and
     *   <code>addSelectionInterval</code>
     *   are equivalent.
     * <li> <code>ListSelectionModel.MULTIPLE_INTERVAL_SELECTION</code>
     *   In this mode, there's no restriction on what can be selected.
     *   This is the default.
     * </ul>
     *
     * @param stringModel the model that stores all selected strings.
     * @param label the optional label of the select box.
     * Set to <code>null</code> for none label. Set an empty
     * <code>String</code> for a border.
     * @param list list (not empty) of strings (not null) for the select box.
     * @param selectionMode an integer specifying the type of selections
     *                         that are permissible
     * @param required if at least one item must be selected
     * @param visibleRowCount the number of visible rows
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringListSelection(
            final SettingsModelStringArray stringModel, final String label,
            final Collection<String> list, final int selectionMode,
            final boolean required, final int visibleRowCount) {
        this(stringModel, label,
                DefaultStringIconOption.createOptionArray(list), selectionMode,
                required, visibleRowCount);
    }
    /**
     * Constructor that puts label and select box into panel. When the settings
     * are applied, the model stores all selected strings of the provided list.
     * The following <code>selectionMode</code> values are allowed:
     * <ul>
     * <li> <code>ListSelectionModel.SINGLE_SELECTION</code> Only one list
     * index can be selected at a time. In this mode the
     * <code>setSelectionInterval</code> and <code>addSelectionInterval</code>
     * methods are equivalent, and only the second index argument is used.
     * <li> <code>ListSelectionModel.SINGLE_INTERVAL_SELECTION</code> One
     * contiguous index interval can be selected at a time. In this mode
     * <code>setSelectionInterval</code> and <code>addSelectionInterval</code>
     * are equivalent.
     * <li> <code>ListSelectionModel.MULTIPLE_INTERVAL_SELECTION</code> In
     * this mode, there's no restriction on what can be selected. This is the
     * default.
     * </ul>
     *
     * @param stringModel the model that stores all selected strings.
     * @param label the optional label of the select box. Set to
     *            <code>null</code> for none label. Set an empty
     *            <code>String</code> for a border.
     * @param list list (not empty) of StringIconOptions (not null) for the
     *            select box.
     * @param selectionMode an integer specifying the type of selections that
     *            are permissible
     * @param required if at least one item must be selected
     * @param visibleRowCount the number of visible rows
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringListSelection(
            final SettingsModelStringArray stringModel, final String label,
            final StringIconOption[] list, final int selectionMode,
            final boolean required, final int visibleRowCount) {
        super(stringModel);

        if (list == null) {
            throw new IllegalArgumentException("Selection list of strings "
                    + "shouldn't be null.");
        }
        m_required = required;
        if (m_required && list.length < 1) {
            throw new IllegalArgumentException("Selection list of strings "
                    + "shouldn't be empty.");
        }

        if (label != null) {
            getComponentPanel().add(new JLabel(label));
        }
        m_listModel = new DefaultListModel();


        for (final StringIconOption o : list) {
            if (o == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_listModel.addElement(o);
        }

        m_selectBox = new JList(m_listModel);
        m_selectBox.setCellRenderer(new StringIconListCellRenderer());
        m_selectBox.setSelectionMode(selectionMode);
        final int rowCount;
        if (visibleRowCount < 0) {
            //get the default visible row count or the number of available items
            //if they are less than the default row count
            rowCount = Math.max(3,
                    Math.min(m_selectBox.getVisibleRowCount(), list.length));
        } else {
            rowCount = visibleRowCount;
        }
        m_selectBox.setVisibleRowCount(rowCount);
        final JScrollPane scrollPane = new JScrollPane(m_selectBox);
        getComponentPanel().add(scrollPane);

        m_selectBox.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                try {
                    updateModel(false);
                } catch (final InvalidSettingsException ise) {
                    // ignore it here
                }
            }
        });
        // we need to update the selection, when the model changes.
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        //call this method to be in sync with the settings model
        updateComponent();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final String[] modelVals =
            ((SettingsModelStringArray)getModel()).getStringArrayValue();
        boolean update;
        final Object[] selectedValues = m_selectBox.getSelectedValues();
        if (modelVals == null) {
            update = (selectedValues != null && selectedValues.length > 0);
        } else {
            update = !deepEquals(modelVals, selectedValues);
        }
        if (update) {
            if (modelVals == null || modelVals.length < 1) {
                m_selectBox.setSelectedValue(null, true);
            } else {
                final List<Integer> selectedIndices =
                    new ArrayList<Integer>(modelVals.length);
                for (final String val : modelVals) {
                    for (int i = 0, size = m_listModel.getSize();
                        i < size; i++) {
                        if (((StringIconOption)m_listModel.getElementAt(i)).
                                getText().equals(val)) {
                            selectedIndices.add(new Integer(i));
                            break;
                        }
                    }
                }
                final int[] indices = new int[selectedIndices.size()];
                for (int i = 0, length = selectedIndices.size();
                    i < length; i++) {
                    indices[i] = selectedIndices.get(i).intValue();

                }
                m_selectBox.setSelectedIndices(indices);
            }
        }

        // also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    private boolean deepEquals(final String[] a1,
            final Object[] a2) {
        if (a1 == null && a2 == null) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        final int length = a1.length;
        if (a2.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            final String e1 = a1[i];
            final StringIconOption e2 = (StringIconOption)a2[i];
            if (e1 == null && e2 == null) {
                continue;
            }
            if (e2 == null && e1 != null) {
                return false;
            }
            if (!e2.getText().equals(e1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transfers the current value from the component into the model.
     * @param checkRequired if the method should check the required flag
     * @throws InvalidSettingsException if a selection is required and no item
     * is selected
     */
    void updateModel(final boolean checkRequired)
        throws InvalidSettingsException {
       final Object[] values = m_selectBox.getSelectedValues();
        if (checkRequired && m_required
                && (values == null || values.length < 1)) {
            m_selectBox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_selectBox.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(final ListSelectionEvent e) {
                    m_selectBox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException(
                    "Please select at least one item from the list.");
        }
        if (values == null || values.length < 1) {
            ((SettingsModelStringArray)getModel()).setStringArrayValue(
                    new String[0]);
        } else {
            final String[] selectedValues = new String[values.length];
            for (int i = 0, length = values.length; i < length; i++) {
                selectedValues[i] = ((StringIconOption)values[i]).getText();
            }
            // we transfer the value from the field into the model
            ((SettingsModelStringArray)getModel()).setStringArrayValue(
                    selectedValues);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(
            final PortObjectSpec[] specs) {
        // we are always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_selectBox.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_selectBox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * Sets the preferred number of rows in the list that can be displayed.
     *
     * <p>
     * The default value of this property is 8.
     * <p>
     *
     * @param visibleRowCount  an integer specifying the preferred number of
     *                         visible rows
     */
    public void setVisibleRowCount(final int visibleRowCount) {
        m_selectBox.setVisibleRowCount(visibleRowCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_selectBox.setToolTipText(text);
    }

    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new strings for the select box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final Collection<String> newItems,
            final String... select) {
        if (newItems == null || newItems.size() < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null");
        }
        final StringIconOption[] options =
            DefaultStringIconOption.createOptionArray(newItems);
        replaceListItems(options, select);
    }

    /**
     * Replaces the list of selectable options in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new {@link StringIconOption}s for the select box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final StringIconOption[] newItems,
            final String... select) {
        if (newItems == null) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null");
        }
        String[] sel = select;
        if (sel == null || sel.length < 1) {
            sel = ((SettingsModelStringArray)getModel()).getStringArrayValue();
        }

        m_listModel.removeAllElements();
        for (final StringIconOption s : newItems) {
            m_listModel.addElement(s);
        }

        boolean found = false;
        if (sel != null && sel.length > 0) {
            final List<Integer> selectedIndices =
                new ArrayList<Integer>(sel.length);
            for (final String val : sel) {
                for (int i = 0, size = m_listModel.getSize();
                    i < size; i++) {
                    if (((StringIconOption)m_listModel.getElementAt(i)).
                    getText().equals(val)) {
                        selectedIndices.add(new Integer(i));
                        found = true;
                        break;
                    }
                }
            }
            final int[] indices = new int[selectedIndices.size()];
            for (int i = 0, length = selectedIndices.size();
                i < length; i++) {
                indices[i] = selectedIndices.get(i).intValue();

            }
            m_selectBox.setSelectedIndices(indices);
        }
        if (!found && newItems.length > 0) {
            //if none of the preseleted items was found select the first
            m_selectBox.setSelectedIndex(0);
        }
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_selectBox.setSize(m_selectBox.getPreferredSize());
        getComponentPanel().validate();
    }
}
