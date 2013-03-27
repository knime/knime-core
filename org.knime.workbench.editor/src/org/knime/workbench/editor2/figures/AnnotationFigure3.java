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
 *   2010 10 14 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.BlockFlow;
import org.eclipse.draw2d.text.BlockFlowLayout;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.PageFlowLayout;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationFigure3 extends Figure {

    private final FlowPage m_page;

    /**
     * @param annotation the annotation to display
     */
    public AnnotationFigure3(final Annotation annotation) {
        setLayoutManager(new BorderLayout());
        Color bg = AnnotationEditPart.getWorkflowAnnotationDefaultBackgroundColor();
        m_page = new FlowPage();
        m_page.setLayoutManager(new PageFlowLayout(m_page));
        m_page.setBackgroundColor(bg);
        add(m_page);
        setConstraint(m_page, BorderLayout.CENTER);
        setBackgroundColor(bg);
        newContent(annotation);
    }

    public void newContent(final Annotation annotation) {
        boolean isNodeAnnotation = (annotation instanceof NodeAnnotation);
        String text;
        AnnotationData.StyleRange[] sr;
        if (AnnotationEditPart.isDefaultNodeAnnotation(annotation)) {
            text = AnnotationEditPart.getAnnotationText(annotation);
            sr = new AnnotationData.StyleRange[0];
        } else {
            text = annotation.getText();
            if (annotation.getStyleRanges() != null) {
                sr =
                        Arrays.copyOf(annotation.getStyleRanges(),
                                annotation.getStyleRanges().length);
            } else {
                sr = new AnnotationData.StyleRange[0];
            }
        }
        Arrays.sort(sr, new Comparator<AnnotationData.StyleRange>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(final AnnotationData.StyleRange o1,
                    final AnnotationData.StyleRange o2) {
                if (o1.getStart() == o2.getStart()) {
                    NodeLogger.getLogger(AnnotationEditPart.class).error(
                            "Ranges overlap");
                    return 0;
                } else {
                    return o1.getStart() < o2.getStart() ? -1 : 1;
                }
            }
        });
        Color bg = AnnotationEditPart.RGBintToColor(annotation.getBgColor());
        setBackgroundColor(bg);
        m_page.setBackgroundColor(bg);
        if (isNodeAnnotation && AnnotationEditPart.DEFAULT_BG_NODE.equals(bg)) {
            // node annotation are white if
            setOpaque(false);
        } else {
            setOpaque(true);
        }
        int i = 0;
        List<TextFlow> segments = new ArrayList<TextFlow>(sr.length);
        final Font defaultFont = isNodeAnnotation
                ? AnnotationEditPart.getNodeAnnotationDefaultFont()
                : AnnotationEditPart.getWorkflowAnnotationDefaultFont();
        for (AnnotationData.StyleRange r : sr) {
            // create text from last range to beginning of this range
            if (i < r.getStart()) {
                String noStyle = text.substring(i, r.getStart());
                if (isNodeAnnotation) {
                    segments.add(getNodeAnnotationDefaultStyled(noStyle, bg));
                } else {
                    segments.add(getWorkflowAnnotationDefaultStyled(noStyle, bg));
                }
                i = r.getStart();
            }
            String styled = text.substring(i, r.getStart() + r.getLength());
            segments.add(getStyled(styled, r, bg, defaultFont));
            i = r.getStart() + r.getLength();
        }
        if (i < text.length()) {
            String noStyle = text.substring(i, text.length());
            if (isNodeAnnotation) {
                segments.add(getNodeAnnotationDefaultStyled(noStyle, bg));
            } else {
                segments.add(getWorkflowAnnotationDefaultStyled(noStyle, bg));
            }
        }
        BlockFlow bf = new BlockFlow();
        // bf.setBorder(new MarginBorder(4, 2, 4, 0));
        BlockFlowLayout bfl = new BlockFlowLayout(bf);
        bfl.setContinueOnSameLine(true);
        bf.setLayoutManager(bfl);
        int position;
        switch (annotation.getAlignment()) {
        case CENTER:
            position = PositionConstants.CENTER;
            break;
        case RIGHT:
            position = PositionConstants.RIGHT;
            break;
        default:
            position = PositionConstants.LEFT;
        }
        bf.setHorizontalAligment(position);
        bf.setOrientation(SWT.LEFT_TO_RIGHT);
        bf.setBackgroundColor(bg);
        for (TextFlow tf : segments) {
            // tf.setLayoutManager(new SimpleFlowLayout(tf));
            bf.add(tf);
        }

        m_page.removeAll();
        m_page.add(bf);
        m_page.setVisible(true);
        revalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBounds(final Rectangle rect) {
        super.setBounds(rect);
        m_page.invalidate();
    }

    private TextFlow getNodeAnnotationDefaultStyled(final String text, final Color bg) {
        Font normalFont = AnnotationEditPart.getNodeAnnotationDefaultFont();
        TextFlow unstyledText = new TextFlow();
        unstyledText.setForegroundColor(AnnotationEditPart
                .getAnnotationDefaultForegroundColor());
        unstyledText.setBackgroundColor(bg);
        unstyledText.setFont(normalFont);
        unstyledText.setText(text);
        return unstyledText;
    }

    private TextFlow getWorkflowAnnotationDefaultStyled(final String text, final Color bg) {
        Font normalFont = AnnotationEditPart.getWorkflowAnnotationDefaultFont();
        TextFlow unstyledText = new TextFlow();
        unstyledText.setForegroundColor(AnnotationEditPart
                .getAnnotationDefaultForegroundColor());
        unstyledText.setBackgroundColor(bg);
        unstyledText.setFont(normalFont);
        unstyledText.setText(text);
        return unstyledText;
    }

    private TextFlow getStyled(final String text,
            final AnnotationData.StyleRange style, final Color bg,
            final Font defaultFont) {
        Font styledFont = AnnotationEditPart.FONT_STORE.getAnnotationFont(
                style, defaultFont);
        TextFlow styledText = new TextFlow(text);
        styledText.setFont(styledFont);
        styledText.setForegroundColor(new Color(null, AnnotationEditPart
                .RGBintToRGBObj(style.getFgColor())));
        styledText.setBackgroundColor(bg);
        return styledText;
    }
}
