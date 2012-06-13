/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
  * KNIME.com, Zurich, Switzerland
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
  * History
  *   Jun 7, 2011 (morent): created
  */

package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class LabeledFilterViewContributionItem extends
        FilterViewContributionItem {
    private Text m_label;

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     */
    public LabeledFilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter) {
        super(viewer, filter);
    }

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     * @param liveUpdate Set to true if the filter should be updated on every
     *      key pressed. If false, it is only updated on pressing enter.
     *
     */
    public LabeledFilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter, final boolean liveUpdate) {
        super(viewer, filter, liveUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout();
        layout.fill = true;
        layout.wrap = false;
        layout.center = true;
        comp.setLayout(layout);
        m_label = new Text(comp, SWT.NONE);
        m_label.setText("Filter");
        m_label.setEditable(false);
        m_label.setBackground(parent.getBackground());
        Combo combo = new Combo(comp, SWT.DROP_DOWN);
        combo.addKeyListener(this);
        combo.addSelectionListener(createSelectionAdaptor());
        RowData rowData = new RowData();
        rowData.width = 150;
        combo.setLayoutData(rowData);
        setCombo(combo);
        return comp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeWidth(final Control control) {
        return super.computeWidth(control)
                + m_label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    }

}
