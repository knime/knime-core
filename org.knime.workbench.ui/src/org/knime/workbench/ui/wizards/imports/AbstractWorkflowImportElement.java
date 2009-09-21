/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
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
 *   13.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Abstract implementation which handles all parent child relationships.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractWorkflowImportElement implements
        IWorkflowImportElement {

    private final Collection<IWorkflowImportElement>m_children
        = new ArrayList<IWorkflowImportElement>();

    private IWorkflowImportElement m_parent;

    /**
     * Invalid if an element with the same name already exists in the target
     * location.
     */
    private boolean m_isInvalid = false;

    /**
     * The name of this element
     */
    private final String m_name;

    /**
     * In case of name clashes this is the renamed name of this import element
     */
    private String m_newName;


    /**
     *
     * @param name the original name of this import element
     */
    public AbstractWorkflowImportElement(final String name) {
        m_name = name;
        m_newName = name;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void addChild(final IWorkflowImportElement child) {
        child.setParent(this);
        m_children.add(child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IWorkflowImportElement> getChildren() {
        return m_children;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract InputStream getContents();


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_newName;
    }

    /**
     * {@inheritDoc}
     */
    public String getOriginalName() {
        return m_name;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setName(final String newName) {
        m_newName = newName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowImportElement getParent() {
        return m_parent;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setParent(final IWorkflowImportElement parent) {
        m_parent = parent;
    }

    /**
     *
     * {@inheritDoc}
     */
    public IPath getRenamedPath() {
        List<String> segments = new ArrayList<String>();
        IWorkflowImportElement element = this;
        while (element != null) {
            segments.add(element.getName());
            element = element.getParent();
        }
        Collections.reverse(segments);
        // remove last element (which is the workspace or zip file)
        segments.remove(0);
        String path = "";
        for (int i = 0; i < segments.size(); i++) {
                path += segments.get(i);
                if (i < segments.size() - 1 && !path.equals("/")) {
                    path += "/";
                }
        }
        return new Path(path);
    }

    /**
     *
     * {@inheritDoc}
     */
    public IPath getOriginalPath() {
        List<String> segments = new ArrayList<String>();
        IWorkflowImportElement element = this;
        while (element != null) {
            segments.add(element.getOriginalName());
            element = element.getParent();
        }
        Collections.reverse(segments);
        // remove last element (which is the workspace or zip file)
        segments.remove(0);
        String path = "";
        for (int i = 0; i < segments.size(); i++) {
                path += segments.get(i);
                if (i < segments.size() - 1 && !path.equals("/")) {
                    path += "/";
                }
        }
        return new Path(path);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setInvalid(final boolean invalid) {
        m_isInvalid = invalid;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isInvalid() {
        return m_isInvalid;
    }

}
