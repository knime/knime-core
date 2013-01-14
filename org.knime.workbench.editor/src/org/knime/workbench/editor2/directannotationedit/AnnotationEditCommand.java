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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 25 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationEditCommand extends Command {

    private final AnnotationData m_newAnnotation;

    private AnnotationData m_oldAnnotation;

    private final Annotation m_theAnnotation;

    /**
     * Creates a new command to change the user node name.
     * @param annoPart unused
     * @param theAnnotation the annotation to change
     * @param newAnnotation an annotation containing the new values.
     */
    public AnnotationEditCommand(final AnnotationEditPart annoPart,
            final Annotation theAnnotation,
            final AnnotationData newAnnotation) {
        m_newAnnotation = newAnnotation;
        m_theAnnotation = theAnnotation;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        // store the old value for undo
        m_oldAnnotation = m_theAnnotation.getData().clone();
        m_theAnnotation.copyFrom(m_newAnnotation, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_oldAnnotation != null;
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        if (m_oldAnnotation != null) {
            m_theAnnotation.copyFrom(m_oldAnnotation, true);
        }
    }
}
