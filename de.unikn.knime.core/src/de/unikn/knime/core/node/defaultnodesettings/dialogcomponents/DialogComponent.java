/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.04.2006 (mb): created
 */
package de.unikn.knime.core.node.defaultnodesettings.dialogcomponents;

import javax.swing.JPanel;

import de.unikn.knime.core.node.defaultnodesettings.settingsmodels.SettingsModel;

/**
 * Abstract (=empty) implementation of a component handling a standard type in a
 * NodeDialog. Actual implementation will make sure the label and editable
 * components are placed nicely in the underlying JPanel and handle save/load to
 * and from config objects via the underlying model.
 * Using the <code>DefaultNodeDialog</code> it is easy to add such Component to
 * quickly assemble a dialog dealing with typical parameters.
 * 
 * @see de.unikn.knime.core.node.defaultnodedialog.DefaultNodeDialogPane
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class DialogComponent extends JPanel {

    /**
     * We need to override the JPanel's method because we need to enable all
     * components contained in this dialog component. It's final because it
     * calls the abstract method <code>setEnabledComponents</code>.
     * 
     * @param enabled if true the contained components will be enabled.
     * @see #setEnabledComponents(boolean)
     * @see java.awt.Component#setEnabled(boolean)
     */
    @Override
    public final void setEnabled(final boolean enabled) {
        setEnabledComponents(enabled);
        super.setEnabled(enabled);
    }

    /**
     * This method is called by the above (final) <code>setEnabled</code>
     * method. Derived classes should disable all the contained components in
     * here.
     * 
     * @param enabled the new status of the component
     * @see #setEnabled(boolean)
     */
    protected abstract void setEnabledComponents(final boolean enabled);
    
    /** Return model that deals with the actual content and offers routines
     * to write/read the content to/from Settings Objects.
     * 
     * @return the underlying model
     */
    protected abstract SettingsModel getModel();

}
