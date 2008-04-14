/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;

import java.util.ArrayList;

import org.knime.core.data.DataTableSpec;


/**
 * The base class for all node dialogs. It provides a tabbed pane to which the
 * derived dialog can add its own components (method
 * <code>addTab(String,Component)</code>). Methods <code>#onOpen()</code>
 * and <code>#onClose()</code> are to be implemented, to setup and cleanup the
 * dialog. A method <code>#onApply()</code> must be provided by the derived
 * class to transfer the user choices into the node's model.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeDialogPane extends GenericNodeDialogPane {

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // get the data specs into a new (possibly smaller) array
        ArrayList<DataTableSpec> dataSpecList = new ArrayList<DataTableSpec>();
        for (PortObjectSpec s : specs) {
            if (s instanceof DataTableSpec) {
                dataSpecList.add((DataTableSpec)s);
            }
        }
        DataTableSpec[] dataSpecs = dataSpecList.toArray(
                new DataTableSpec[dataSpecList.size()]);
        loadSettingsFrom(settings, dataSpecs);
    }

    protected abstract void loadSettingsFrom(NodeSettingsRO settings,
            DataTableSpec[] specs) throws NotConfigurableException;


}
