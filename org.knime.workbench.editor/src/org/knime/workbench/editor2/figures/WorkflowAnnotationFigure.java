/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   17.09.2015 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.knime.core.def.node.workflow.IAnnotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl
 */
public class WorkflowAnnotationFigure extends NodeAnnotationFigure {

    private final Label m_editIcon;

    private static final Image MOVE_ICON = ImageRepository.getImage(SharedImages.AnnotationMoveHover);

    public WorkflowAnnotationFigure(final IAnnotation anno) {
        super(anno);
        ImageData d = MOVE_ICON.getImageData();
        m_editIcon = new Label(MOVE_ICON);
        m_editIcon.setBounds(new Rectangle(0, 0, d.width, d.height));
        m_editIcon.setVisible(false); // visible only when mouse enters
        add(m_editIcon);
    }

    public void showEditIcon(final boolean showit) {
        m_editIcon.setVisible(showit);
    }

    public Rectangle getEditIconBounds() {
        return m_editIcon.getBounds();
    }

    @Override
    public void newContent(final IAnnotation annotation) {
        super.newContent(annotation);

        AnnotationData data = annotation.getData();

        Color bg = AnnotationEditPart.RGBintToColor(data.getBgColor());
        setBackgroundColor(bg);
        m_page.setBackgroundColor(bg);

        // set border with specified annotation color
        if (data.getBorderSize() > 0) {
            Color col = AnnotationEditPart.RGBintToColor(data.getBorderColor());
            m_page.setBorder(new LineBorder(col, data.getBorderSize()));
        } else {
            m_page.setBorder(null);
        }
    }
}
