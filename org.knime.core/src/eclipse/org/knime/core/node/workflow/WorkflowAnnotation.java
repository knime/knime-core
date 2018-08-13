/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 21, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow;

/** Workflow annotation (not associated with a node).
 * Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class WorkflowAnnotation extends Annotation {

    private WorkflowAnnotationID m_id = null;

    /** New empty annotation. */
    public WorkflowAnnotation() {
        this(new AnnotationData());
    }

    /** Restore annotation.
     * @param data Data */
    public WorkflowAnnotation(final AnnotationData data) {
        super(data);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowAnnotation clone() {
        WorkflowAnnotation anno = (WorkflowAnnotation)super.clone();
        anno.unsetID();
        return anno;
    }

    /**
     * Sets the annotation id. Can only be called once in order to make sure that the very same annotation is not part
     * of two or more workflows. If the id has been set already, an exception will be thrown.
     *
     * @param id the id
     * @throws IllegalStateException if the id has been set already
     */
    void setID(final WorkflowAnnotationID id) throws IllegalStateException {
        if (m_id != null) {
            throw new IllegalStateException("Workflow annotation id has been set already");
        }
        m_id = id;
    }

    /**
     * Sets the associated id to <code>null</code> such that {@link #setID(WorkflowAnnotationID)} can be called again.
     * Is called when a workflow annotation is removed from its workflow manager
     * ({@link WorkflowManager#removeAnnotation(WorkflowAnnotation)}).
     */
    void unsetID() {
        m_id = null;
    }

    /**
     * Gives access to the workflow annotation id. Id is only available if the workflow annotation is part of a
     * workflow manager. I.e. when the annotation is added to a workflow manager the annotation id will be set by the
     * workflow manager (see {@link WorkflowManager#addWorkflowAnnotation(WorkflowAnnotation)}).
     *
     * @return the id or <code>null</code> if the workflow annotation is not part of a workflow, yet
     * @since 3.7
     */
    public WorkflowAnnotationID getID() {
        return m_id;
    }

}
