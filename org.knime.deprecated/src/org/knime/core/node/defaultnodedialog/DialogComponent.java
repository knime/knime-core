/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
