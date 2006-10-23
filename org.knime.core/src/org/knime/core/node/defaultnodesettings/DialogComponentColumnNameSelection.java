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
 *   21.09.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link org.knime.core.data.DataTableSpec}. Provides label and list
 * (possibly filtered by a given {@link org.knime.core.data.DataCell} type) as
 * well as functionality to load/store into config object.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnNameSelection extends DialogComponent {
    /** Contains all column names matching the given given filter class. */
    private final ColumnSelectionPanel m_chooser;

    private final int m_specIndex;

    private final List<Class<? extends DataValue>> m_typeList;

    /**
     * Constructor that puts label and checkbox into panel.
     * 
     * @param model the model holding the value of this component
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex,
            final Class<? extends DataValue>... classFilter) {
        this(model, label, specIndex, true, classFilter);
    }

    /**
     * Constructor that puts label and checkbox into panel.
     * 
     * @param model the model holding the value of this component
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired True, if the component should throw an exception in
     *            case of no available compatible type, false otherwise.
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final Class<? extends DataValue>... classFilter) {
        super(model);
        this.add(new JLabel(label));
        m_chooser = new ColumnSelectionPanel((Border)null, classFilter);
        m_chooser.setRequired(isRequired);
        this.add(m_chooser);

        m_specIndex = specIndex;
        m_typeList = Arrays.asList(classFilter);

        // we are not listening to the selection panel and not updating the
        // model on a selection change. We set the value in the model right
        // before save

        // update the selection panel, when the model was changed
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                String classCol = ((SettingsModelString)getModel())
                        .getStringValue();
                if ((classCol == null) || (classCol.length() == 0)) {
                    classCol = "** Unknown column **";
                }
                try {
                    m_chooser.update(getLastTableSpec(m_specIndex), classCol);
                } catch (NotConfigurableException e1) {
                    // we check the correctness of the table spec before, so
                    // this exception shouldn't fly.
                    assert false;
                }
            }
        });
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        /*
         * this is a bit of code duplication: if the selection panel is set to
         * require at least one selectable column in the specs, it will fail
         * during update if no such column is present. We check this here, to
         * avoid loading if no column is selectable, so that the update with a
         * new value (following this method call) will not fail.
         */
        if ((specs == null) || (specs.length < m_specIndex)) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        DataTableSpec spec = specs[m_specIndex];
        if (spec == null) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        // now check if at least one column is compatible to at least
        // one of the types we accept
        for (DataColumnSpec col : spec) {
            for (Class<? extends DataValue> t : m_typeList) {
                if (col.getType().isCompatible(t)) {
                    // we found one acceptable type we are compatible to - cool!
                    return;
                }
            }
        }

        // here none of the columns are compatible to the acceptable types.
        throw new NotConfigurableException("The input table doesn't contain"
                + " a column with the expected type.");
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        // we transfer the selected value in the model here
        ((SettingsModelString)getModel()).setStringValue(m_chooser
                .getSelectedColumn());
    }

    /**
     * @see DialogComponent#setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }
}
