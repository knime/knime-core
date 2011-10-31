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
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationEditPolicy extends DirectEditPolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getDirectEditCommand(final DirectEditRequest edit) {
        StyledTextEditor ste = (StyledTextEditor)edit.getCellEditor();
        AnnotationData newAnnoData = (AnnotationData)ste.getValue();
        AnnotationEditPart annoPart = (AnnotationEditPart)getHost();
        Annotation oldAnno = annoPart.getModel();
        if (hasAnnotationDataChanged(oldAnno, newAnnoData)) {
            return new AnnotationEditCommand(annoPart, oldAnno, newAnnoData);
        }
        return null;
    }

    /** Compares the content of the new and the old annotation data. If they
     * are equal, no change command is executed.
     * @param oldAnno Old annotation (may be a default node annotation)
     * @param newAnnoData The new annotation data (not a default one)
     * @return If they are equally (a default with "Node x" equals a non-default
     * with "Node x")
     */
    private static boolean hasAnnotationDataChanged(
            final Annotation oldAnno, final AnnotationData newAnnoData) {
        AnnotationData oldAnnoRealData = new AnnotationData();
        // copy bounds, overwrite text later
        oldAnnoRealData.copyFrom(newAnnoData, true);
        oldAnnoRealData.copyFrom(oldAnno.getData(), false);
        // need to set text explicitely - default node annotations have
        // no text (it's determined during rendering)
        oldAnnoRealData.setText(AnnotationEditPart.getAnnotationText(oldAnno));
        return !oldAnnoRealData.equals(newAnnoData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showCurrentEditValue(final DirectEditRequest request) {
        // hack to prevent async layout from placing the cell editor twice.
        getHostFigure().getUpdateManager().performUpdate();
    }

}
