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
