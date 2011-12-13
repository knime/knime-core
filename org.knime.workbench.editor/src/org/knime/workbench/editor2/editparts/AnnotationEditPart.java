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
 *   2010 10 11 (ohl): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.TextUtilities;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditManager;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditPolicy;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditorLocator;
import org.knime.workbench.editor2.figures.AnnotationFigure3;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationEditPart extends AbstractWorkflowEditPart implements
        NodeUIInformationListener, IPropertyChangeListener {

    private static final Color DEFAULT_FG = ColorConstants.black;

    /** Light yellow. */
    public static final Color DEFAULT_BG_WORKFLOW =
        new Color(null, 255, 255, 225);

    /** White. */
    public static final Color DEFAULT_BG_NODE = new Color(null, 255, 255, 255);

    private static final Font DEFAULT_FONT = Display.getCurrent()
            .getSystemFont();

    /**
     * Fonts used in the figure or the style editor must go through this.
     */
    public static final FontStore FONT_STORE = new FontStore(DEFAULT_FONT);

    /**
     * If no foreground color is set, this one should be used.
     *
     * @return the default font color of annotation figures
     */
    public static Color getAnnotationDefaultForegroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_FG;
    }

    /**
     * @return the height of one line in default font
     */
    public static int defaultOneLineHeight() {
        TextUtilities iNSTANCE = TextUtilities.INSTANCE;
        Font font = getAnnotationDefaultFont();
        return iNSTANCE.getStringExtents("Agq|_ÊZ", font).height;
    }

    /**
     * @param text to test
     * @return the width of the specified text with the annotation default font
     */
    public static int defaultLineWidth(final String text) {
        TextUtilities INSTANCE = TextUtilities.INSTANCE;
        Font font = getAnnotationDefaultFont();
        return INSTANCE.getStringExtents(text, font).width;
    }

    /**
     * If no background color is set, this one should be used for workflow
     * annotations.
     *
     * @return the default background color of workflow annotation figures
     */
    public static Color getWorkflowAnnotationDefaultBackgroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_BG_WORKFLOW;
    }
    /**
     * If no background color is set, this one should be used for node
     * annotations.
     *
     * @return the default background color of node annotation figures
     */
    public static Color getNodeAnnotationDefaultBackgroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_BG_NODE;
    }

    /**
     * Returns an integer with 24bit color info. Bits 23 to 16 contain the red
     * value, 15 to 8 contain the green value, and 7 to 0 contain the blue.
     *
     * @param c the color to translate
     * @return an integer with 24bit color info
     */
    public static int colorToRGBint(final Color c) {
        return ((c.getRed() & 0x0FF) << 16) | ((c.getGreen() & 0x0FF) << 8)
                | ((c.getBlue() & 0x0FF));
    }

    /**
     * Returns the red, green, blue value of the specified 24bit int as separate
     * values in a new object.
     *
     * @param rgb the 24bit color integer to convert
     * @return a new RGB object with separated rgb values
     */
    public static RGB RGBintToRGBObj(final int rgb) {
        return new RGB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Returns a new Color for the passed rgb values.
     *
     * @param rgb
     * @return a new Color for the passed RGB object
     */
    public static Color RGBintToColor(final int rgb) {
        return RGBtoColor(RGBintToRGBObj(rgb));
    }

    /**
     * Returns a new Color for the passed RGB object.
     *
     * @param rgb
     * @return a new Color for the passed RGB object
     */
    public static Color RGBtoColor(final RGB rgb) {
        return new Color(null, rgb);
    }

    /**
     *
     * If no font is set, this one should be used.
     *
     * @return the default font for annotation figures
     */
    public static Font getAnnotationDefaultFont() {
        // TODO: read it from a pref page...
        return DEFAULT_FONT;
    }

    private AnnotationEditManager m_directEditManager;

    /** {@inheritDoc} */
    @Override
    public Annotation getModel() {
        return (Annotation)super.getModel();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        Annotation anno = getModel();
        AnnotationFigure3 f = new AnnotationFigure3(anno);
        if (anno instanceof WorkflowAnnotation) {
            f.setBounds(new Rectangle(anno.getX(), anno.getY(), anno.getWidth(),
                    anno.getHeight()));
        }
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        store.addPropertyChangeListener(this);

        Annotation anno = getModel();
        anno.addUIInformationListener(this);
        // update the ui info now
        nodeUIInformationChanged(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        store.removePropertyChangeListener(this);
        Annotation anno = getModel();
        anno.removeUIInformationListener(this);
        super.deactivate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {
        // Installs the edit policy to directly edit the annotation in its
        // editpart (through the StyledTextEditor) after clicking it twice.
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE,
                new AnnotationEditPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        Annotation anno = getModel();
        AnnotationFigure3 annoFig = (AnnotationFigure3)getFigure();
        annoFig.newContent(anno);
        WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
        parent.setLayoutConstraint(this, getFigure(), new Rectangle(
                anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight()));
        // annoFig.revalidate();
        refreshVisuals();
        parent.refresh();
    }


    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent p) {
        if (p.getProperty().equals(PreferenceConstants.P_DEFAULT_NODE_LABEL)) {
            AnnotationFigure3 fig = (AnnotationFigure3)getFigure();
            fig.newContent(getModel());
        }
    }

    /**
     *
     * @param t
     * @param parent
     * @return
     */
    public static StyledText toStyledText(final Annotation t,
            final Composite parent) {
        StyledText stext = new StyledText(parent, SWT.NONE);
        String text;
        StyleRange[] styles;
        boolean isDefaultNodeAnnotation = isDefaultNodeAnnotation(t);
        if (isDefaultNodeAnnotation) {
            text = getAnnotationText(t);
            styles = new StyleRange[0];
        } else {
            text = t.getText();
            styles = toSWTStyleRanges(t.getData());
        }
        stext.setText(text);
        stext.setStyleRanges(styles);
        return stext;
    }

    /** Returns the text contained in the annotation or the default text if
     * the argument annotation is a default node annotation ("Node 1", "Node 2",
     * ...).
     * @param t The annotation, not null.
     * @return The above text. */
    public static String getAnnotationText(final Annotation t) {
        if (!isDefaultNodeAnnotation(t)) {
            return t.getText();
        }
        String text;
        int id = ((NodeAnnotation)t).getNodeContainer().getID().getIndex();
        String prefix = KNIMEUIPlugin.getDefault().getPreferenceStore().
            getString(PreferenceConstants.P_DEFAULT_NODE_LABEL);
        if (prefix == null || prefix.isEmpty()) {
            text = "";
        } else {
            text = prefix + " " + id;
        }
        return text;
    }

    /**
     * @param t
     * @return */
    public static boolean isDefaultNodeAnnotation(final Annotation t) {
        return t instanceof NodeAnnotation
        && (((NodeAnnotation)t).getData()).isDefault();
    }

    public static StyleRange[] toSWTStyleRanges(final AnnotationData t) {
        AnnotationData.StyleRange[] waStyleRange = t.getStyleRanges();
        ArrayList<StyleRange> swtStyleRange =
                new ArrayList<StyleRange>(waStyleRange.length);
        for (AnnotationData.StyleRange waSr : waStyleRange) {
            StyleRange swtStyle = new StyleRange();
            Font f =
                    FONT_STORE.getFont(waSr.getFontName(), waSr.getFontSize(),
                            waSr.getFontStyle());
            if (!FONT_STORE.isDefaultFont(f)) {

                swtStyle.font = f;
            }
            if (waSr.getFgColor() >= 0) {
                int rgb = waSr.getFgColor();
                RGB rgbObj = RGBintToRGBObj(rgb);
                swtStyle.foreground = new Color(null, rgbObj);
            }
            swtStyle.start = waSr.getStart();
            swtStyle.length = waSr.getLength();
            swtStyleRange.add(swtStyle);
        }
        return swtStyleRange.toArray(new StyleRange[swtStyleRange.size()]);
    }

    /**
     *
     * @param s the component with the styled text to convert.
     * @return
     */
    public static AnnotationData toAnnotationData(final StyledText s) {
        AnnotationData result = new AnnotationData();
        result.setText(s.getText());
        result.setBgColor(colorToRGBint(s.getBackground()));
        TextAlignment alignment;
        switch (s.getAlignment()) {
        case SWT.RIGHT:
            alignment = TextAlignment.RIGHT;
            break;
        case SWT.CENTER:
            alignment = TextAlignment.CENTER;
            break;
        default:
            alignment = TextAlignment.LEFT;
        }
        result.setAlignment(alignment);

        StyleRange[] swtStyleRange = s.getStyleRanges();
        ArrayList<AnnotationData.StyleRange> wfStyleRanges =
                new ArrayList<AnnotationData.StyleRange>(
                        swtStyleRange.length);
        for (StyleRange sr : swtStyleRange) {
            if (sr.isUnstyled()) {
                continue;
            }
            AnnotationData.StyleRange waSr =
                    new AnnotationData.StyleRange();
            Color fg = sr.foreground;
            if (fg != null) {
                int rgb = colorToRGBint(fg);
                waSr.setFgColor(rgb);
            }
            Font f = sr.font;
            if (f != null) {
                waSr.setFontName(f.getFontData()[0].getName());
                waSr.setFontSize((f.getFontData()[0].getHeight()));
                waSr.setFontStyle(f.getFontData()[0].getStyle());
            }
            waSr.setStart(sr.start);
            waSr.setLength(sr.length);
            wfStyleRanges.add(waSr);
        }
        result.setStyleRanges(wfStyleRanges
                .toArray(new AnnotationData.StyleRange[wfStyleRanges.size()]));
        return result;
    }

    @Override
    public void performRequest(final Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
            // enter edit mode only after a double-click
            performEdit();
        } else if (request.getType() == RequestConstants.REQ_OPEN) {
            // caused by a double click on this edit part
            performEdit();
        } else {
            super.performRequest(request);
        }
    }

    /**
     * Opens the editor to directoy edit the annotation in place.
     */
    public void performEdit() {
        final EditPart parent = getParent();
        if (parent instanceof WorkflowRootEditPart) {
            WorkflowRootEditPart wkfRootEdit = (WorkflowRootEditPart)parent;
            if (wkfRootEdit.getWorkflowManager().isWriteProtected()) {
                return;
            }
        }
        if (m_directEditManager == null) {
            m_directEditManager =
                    new AnnotationEditManager(this,
                            new StyledTextEditorLocator(
                                    (AnnotationFigure3)getFigure()));
        }

        m_directEditManager.show();
    }

    /** Extract the WorkflowAnnotation models from the argument list. It will
     * ignore NodeAnnotations (which have the same edit part).
     * @param annoParts The selected annotation parts
     * @return The workflow annotation models (possibly fewer than selected
     * edit parts!!!)
     */
    public static WorkflowAnnotation[] extractWorkflowAnnotations(
            final AnnotationEditPart[] annoParts) {
        List<WorkflowAnnotation> annoList = new ArrayList<WorkflowAnnotation>();
        for (int i = 0; i < annoParts.length; i++) {
            Annotation model = annoParts[i].getModel();
            if (model instanceof WorkflowAnnotation) {
                annoList.add((WorkflowAnnotation)model);
            }
        }
        return annoList.toArray(new WorkflowAnnotation[annoList.size()]);
    }


}
