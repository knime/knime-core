/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
 */
package org.knime.workbench.ui.masterkey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.FlowVariable;

/**
 *
 * @author Thomas Gabriel, KNIME.com GmbH
 */
public class CredentialVariableTable implements Iterable<Credentials> {

    private static final String VAR_NAME = "Name";
    private static final String VAR_LOGIN = "Login";
    private static final String VAR_PASSWORD = "Password";

    private final List<Credentials> m_params
        = new ArrayList<Credentials>();

    private final TableViewer m_viewer;

    /**
     *
     * @param parent parent composite
     */
    public CredentialVariableTable(final Composite parent) {
        m_viewer = createViewer(parent);
        m_viewer.setInput(m_params);
    }

    /**
     *
     * @param cred workflow credentials
     * @return true if the variable was added, false if there was already a
     *  variable with same name and type
     *
     */
    public boolean add(final Credentials cred) {
        if (m_params.contains(cred)) {
            return false;
        }
        m_params.add(cred);
        return true;
    }

    /** Removes the given credential.
     * @param credential to be removed from the table
     */
    public void remove(final Credentials credential) {
        m_params.remove(credential);
    }

    /**
     *
     * @param idx index of the variable
     * @return the variable
     */
    public Credentials get(final int idx) {
        return m_params.get(idx);
    }


    /**
     *
     * @return an unmodifiable list of the represented {@link FlowVariable}s.
     */
    public List<Credentials> getCredentials() {
        return Collections.unmodifiableList(m_params);
    }

    /**
     * Replaces the variable at the given index with the new one.
     * @param index index of variable to be replaced (starts at 0)
     * @param credential which should be inserted at position index
     */
    public void replace(final int index, final Credentials credential) {
        m_params.set(index, credential);
    }

    /**
     *
     * @return the underlying {@link TableViewer}
     */
    public TableViewer getViewer() {
        return m_viewer;
    }

    private TableViewer createViewer(final Composite parent) {
        TableViewer viewer = new TableViewer(parent, SWT.SINGLE
                | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);

        TableViewerColumn nameCol = new TableViewerColumn(viewer, SWT.NONE);
        nameCol.getColumn().setText(VAR_NAME);
        nameCol.getColumn().setWidth(100);

        TableViewerColumn loginCol = new TableViewerColumn(viewer, SWT.NONE);
        loginCol.getColumn().setText(VAR_LOGIN);
        loginCol.getColumn().setWidth(100);
        
        TableViewerColumn typeCol = new TableViewerColumn(viewer, SWT.NONE);
        typeCol.getColumn().setText(VAR_PASSWORD);
        typeCol.getColumn().setWidth(100);

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        viewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(final Object arg) {
                return ((List<Credentials>) arg).toArray();
            }
            @Override
            public void dispose() {
                // do nothing -> images are static
            }
            @Override
            public void inputChanged(final Viewer arg0, final Object arg1,
                    final Object arg2) {
                // do nothing
            }
        });
        viewer.setLabelProvider(new WorkflowVariableLabelProvider());

        viewer.setInput(m_params);

        return viewer;
    }

    private static class WorkflowVariableLabelProvider
            extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(final Object arg0, final int arg1) {
            return null;
        }

        @Override
        public String getColumnText(final Object arg0, final int arg1) {
            Credentials cred = (Credentials) arg0;
            switch (arg1) {
            case 0: return cred.getName();
            case 1: return cred.getLogin();
            case 2: return cred.getPassword();
            default: throw new RuntimeException(
                    "Invalid number of columns defined: " + arg1);
            }
        }

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<Credentials> iterator() {
        return m_params.iterator();
    }
}
