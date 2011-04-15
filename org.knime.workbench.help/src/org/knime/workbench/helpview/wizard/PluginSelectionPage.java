/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.12.2007 (Fabian Dill): created
 */
package org.knime.workbench.helpview.wizard;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 *
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PluginSelectionPage extends WizardPage {

    private CheckboxTableViewer m_table;


    /**
     *
     */
    public PluginSelectionPage() {
        super("KNIME Help Files Converter");
        setTitle("KNIME Help Files Converter");
        setDescription("Creates help files from the node descriptions "
                + "for the Eclipse build-in online help");
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {

        Composite container = new Composite(parent, SWT.NULL);
        container.setBounds(0, 0, 600, 200);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        container.setLayout(layout);

        m_table = CheckboxTableViewer.newCheckList(container,
                SWT.BORDER | SWT.SHADOW_IN);
        m_table.setLabelProvider(new LabelProvider());
        m_table.setContentProvider(new ArrayContentProvider());
        m_table.setSorter(new ViewerSorter() {

            /**
             *
             * {@inheritDoc}
             */
            @Override
            public int compare(final Viewer viewer,
                    final Object e1, final Object e2) {
                String s1 = (String)e1;
                String s2 = (String)e2;
                return s1.compareTo(s2);
            }

        });
        initTable();
        m_table.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                String selection = (String)((IStructuredSelection)event
                        .getSelection()).getFirstElement();
                m_table.setChecked(selection,
                        !m_table.getChecked(selection));
                updatePageComplete();
            }
        });
        /*
        m_table.addSelectionChangedListener(new ISelectionChangedListener() {

            /**
             *
             * {@inheritDoc}
             *
            public void selectionChanged(final SelectionChangedEvent event) {
                updatePageComplete();
            }

        });
        */
        GridData gridData = new GridData();
        gridData.widthHint = 400;
        m_table.getControl().setLayoutData(gridData);

        Composite buttonCont = new Composite(container, SWT.TOP);

        GridLayout layout2 = new GridLayout();
        layout2.numColumns = 1;
        buttonCont.setLayout(layout2);

        gridData = new GridData();
        buttonCont.setLayoutData(gridData);

        Button selectAll = new Button(buttonCont, SWT.PUSH);
        selectAll.setText("Select all");
        gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        gridData.horizontalAlignment = SWT.RIGHT;
        gridData.widthHint = 80;
        selectAll.setLayoutData(gridData);
        selectAll.addSelectionListener(new SelectionListener() {

            /**
             *
             * {@inheritDoc}
             */
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                m_table.setAllChecked(true);
                updatePageComplete();
            }

            /**
             *
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_table.setAllChecked(true);
                updatePageComplete();
            }

        });
        Button deselectAll = new Button(buttonCont, SWT.PUSH);
        deselectAll.setText("Deselect all");
        gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        gridData.horizontalAlignment = SWT.RIGHT;
        gridData.widthHint = 80;
        deselectAll.setLayoutData(gridData);
        deselectAll.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                m_table.setAllChecked(false);
                updatePageComplete();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_table.setAllChecked(false);
                updatePageComplete();
            }

        });
        setControl(container);
        updatePageComplete();
        setMessage(null);
        setErrorMessage(null);
    }

    private void initTable() {
        IConfigurationElement[] configs = NodeDescriptionConverter
            .getConfigurationElements();
        Set<String>grayed = new HashSet<String>();
        Set<String>checked = new HashSet<String>();
        Set<String>all = new HashSet<String>();
        for (IConfigurationElement e : configs) {
            String pluginId = e.getNamespaceIdentifier();
            all.add(pluginId);
            URL path = FileLocator.find(
                    Platform.getBundle(pluginId),
                    new Path("/tocs"), null);
            if (path == null) {
                checked.add(pluginId);
            } else {
                path = FileLocator.find(
                        Platform.getBundle(pluginId),
                        new Path("/tocs"), null);
                File pluginDir = new File(new File(path.getFile())
                .getAbsolutePath());
                if (!pluginDir.canWrite()) {
                    checked.remove(pluginId);
                    grayed.add(pluginId);
                }
            }
        }
        m_table.setInput(all.toArray());
        m_table.setCheckedElements(checked.toArray());
        m_table.setGrayedElements(grayed.toArray());
    }



    private void updatePageComplete() {
        setPageComplete(false);
        if (getSelectedItems() != null && getSelectedItems().length > 0) {
            StringWriter errorMessage = new StringWriter();
            for (String s : getSelectedItems()) {
                URL devWorkSpace;
                try {
                    devWorkSpace = FileLocator.toFileURL(FileLocator.find(
                            Platform.getBundle(s), new Path("/"), null));
                    File loc = new File(devWorkSpace.getFile().toString());
                    if (!loc.canWrite()) {
                        errorMessage.append("Plug-in " + s
                                + " has no write permissions.");
                    }
                } catch (IOException e) {
                    errorMessage.append("Plug-in " + s
                            + "cannot be processes. Cause: " + e.getCause()
                            + "\n");

                }
            }
            setPageComplete(true);
            if (errorMessage.getBuffer().length() > 0) {
                setErrorMessage(errorMessage.toString());
            } else {
                setErrorMessage(null);
            }
        } else {
            setErrorMessage("Please select the plug-ins for which the "
                    + "help files should be generated");
        }
    }

    /*
     *
     *
     *
    @Override
    public void setVisible(final boolean visible) {
        if (!visible && getSelectedItems() != null
                && getSelectedItems().length > 0) {
            try {
                final String[] selection = getSelectedItems();
                getContainer().run(true, false, new IRunnableWithProgress() {
                    /**
                     *
                     *
                     *
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException,
                            InterruptedException {
                        buildDocumentation(monitor,
                                selection);
                        setPageComplete(true);
                    }
                });
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // should never happen, since task can not be cancelled
                e.printStackTrace();
            }
        }
        super.setVisible(visible);
    }
    */


    /**
     *
     * @return the selected items
     */
    public String[] getSelectedItems() {
        String[] selection = null;
        if (m_table.getCheckedElements() != null
                && m_table.getCheckedElements().length > 0) {
            Object[] checked = m_table.getCheckedElements();
            selection = new String[checked.length];
            for (int i = 0; i < checked.length; i++) {
                selection[i] = (String)checked[i];
            }
        }
        return selection;
    }


}
