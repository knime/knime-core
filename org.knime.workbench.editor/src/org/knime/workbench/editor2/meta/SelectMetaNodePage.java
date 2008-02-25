/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   07.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class SelectMetaNodePage extends WizardPage {

    private static final String TITLE = "Select predefined MetaNode";
    private static final String DESCRIPTION = "If you want to create a MetaNode"
        + " with a usual number of data in and out ports, select one; \n"
        + "otherwise click next to define a custom MetaNode";

    private Combo m_comboBox;

    static final String ZERO_ONE = "0:1";
    static final String ONE_ONE = "1:1";
    static final String ONE_TWO = "1:2";
    static final String TWO_ONE = "2:1";
    static final String TWO_TWO = "2:2";
    static final String CUSTOM = "custom";

    private static final String[] metaNodes = new String[] {
        ZERO_ONE,
        ONE_ONE,
        ONE_TWO,
        TWO_ONE,
        TWO_TWO,
        CUSTOM
    };

    private String m_selectedMetaNode;


    /**
     *
     */
    public SelectMetaNodePage() {
        super(TITLE);
        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.FILL);
        composite.setLayout(new GridLayout(2, false));
        Label label = new Label(composite, SWT.READ_ONLY);
        label.setText("Select a predefined MetaNode");

        m_comboBox = new Combo(composite, SWT.RIGHT | SWT.READ_ONLY);
        m_comboBox.setItems(metaNodes);
        m_comboBox.select(5);
        m_comboBox.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                int selectedIdx = m_comboBox.getSelectionIndex();
                if (selectedIdx < 0) {
                    m_selectedMetaNode = null;
                    setPageComplete(false);
                } else {
                    m_selectedMetaNode = metaNodes[selectedIdx];
                    setPageComplete(true);
                }
            }
        });
        setControl(composite);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        return m_selectedMetaNode != null;
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return m_selectedMetaNode == null || m_selectedMetaNode.equals(CUSTOM);
    }

    String getSelectedMetaNode() {
        return m_selectedMetaNode;
    }



}
