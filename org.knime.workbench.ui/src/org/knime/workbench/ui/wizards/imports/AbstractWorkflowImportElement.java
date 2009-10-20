/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
