/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.util.Collection;

import org.eclipse.core.runtime.IPath;

/**
 * Represents either a file or a zip entry of a workflow or workflow group to be
 * imported.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public interface IWorkflowImportElement {

    /**
     *
     * @return the children of this workflow group
     */
    public Collection<IWorkflowImportElement> getChildren();

    /**
     *
     * @return the parent workflow group
     */
    public IWorkflowImportElement getParent();

    /**
     *
     * @return name of this workflow group or workflow
     */
    public String getName();

    /**
    *
    * @return original name of this workflow group or workflow
    */
    public String getOriginalName();

    /**
     *
     * @param newName the new name (e.g. entered in rename page)
     */
    public void setName(String newName);


    /**
     *
     * @return stream of the contents
     */
    public InputStream getContents();

    /**
     *
     * @param child add a workflow group or workflow to this workflow group
     */
    public void addChild(IWorkflowImportElement child);

    /**
     *
     * @param parent set the parent of this workflow or workflow group
     */
    public void setParent(IWorkflowImportElement parent);

    /**
     *
     * @return a relative path this import element if it is imported into
     *  the workspace root before it was renamed
     */
    public IPath getOriginalPath();

    /**
     *
     * @return a relative path this import element if it is imported into
     *  the workspace root before it was renamed
     */
    public IPath getRenamedPath();

    /**
     *
     * @return true if this path is valid
     */
    public boolean isInvalid();

    /**
     *
     * @param invalid true if the path would be invalid (element already
     * exists in destination location)
     */
    public void setInvalid(boolean invalid);

    /**
     *
     * @return true if the element is a workflow
     */
    public boolean isWorkflow();

    /**
     *
     * @return true if the element is a workflow group
     */
    public boolean isWorkflowGroup();



}
