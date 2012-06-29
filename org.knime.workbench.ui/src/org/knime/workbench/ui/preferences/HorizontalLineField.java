/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: 29.06.2012
 * Author: Peter Ohl
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Preference page field creating a horizontal separator.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class HorizontalLineField extends FieldEditor {

    private Label m_line;

    /**
     * @param parent
     */
    HorizontalLineField(final Composite parent) {
        super("HOR_LINE", "", parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createControl(final Composite parent) {
        m_line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        super.createControl(parent); // calls doFillIntoGrid!
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void adjustForNumColumns(final int numColumns) {
        Object o = m_line.getLayoutData();
        if (o instanceof GridData) {
            ((GridData)o).horizontalSpan = numColumns;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFillIntoGrid(final Composite parent, final int numColumns) {
        m_line.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, numColumns, 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doLoad() {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doLoadDefault() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStore() {
        // nothing to store
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfControls() {
        return 1;
    }

}
