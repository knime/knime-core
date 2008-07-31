/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Abstract (=empty) implementation of a component handling a standard type in a
 * NodeDialog. Actual implementations will make sure the label and editable
 * components are placed nicely in the underlying JPanel and handle save/load to
 * and from config objects. Using the
 * {@link org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane} it
 * is easy to add such Component to quickly assemble a dialog dealing with
 * typical parameters.
 * 
 * @see DefaultNodeDialogPane
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public abstract class DialogComponent extends JPanel {

    /**
     * Read value(s) of this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws InvalidSettingsException if load fails.
     * @throws NotConfigurableException If there is no chance for the dialog 
     * component to be valid (i.e. the settings are valid), e.g. if the given
     * specs lack some important columns or column types. 
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) 
        throws InvalidSettingsException, NotConfigurableException;

    /**
     * Write value(s) of this dialog component to configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    public abstract void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException;

    /**
     * We need to override the JPanel's method because we need to enable all
     * components contained in this dialog component. It's final because it
     * calls the abstract method {@link #setEnabledComponents(boolean)}.
     * 
     * @param enabled if <code>true</code> the contained components will be
     * enabled
     * @see #setEnabledComponents(boolean)
     * @see java.awt.Component#setEnabled(boolean)
     */
    @Override
    public final void setEnabled(final boolean enabled) {
        setEnabledComponents(enabled);
        super.setEnabled(enabled);
    }

    /**
     * This method is called by the above (final) {@link #setEnabled(boolean)}
     * method. Derived classes should disable all the contained components in
     * here.
     * 
     * @param enabled the new status of the component
     * @see #setEnabled(boolean)
     */
    protected abstract void setEnabledComponents(final boolean enabled);
}
