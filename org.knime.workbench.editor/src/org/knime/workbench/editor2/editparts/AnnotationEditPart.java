/*
 * ------------------------------------------------------------------------
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
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
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
import org.eclipse.ui.PlatformUI;
import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.api.node.workflow.IAnnotation;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.IWorkflowAnnotation;
import org.knime.core.api.node.workflow.NodeUIInformationEvent;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.workbench.editor2.WorkflowMarqueeSelectionTool;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.WorkflowSelectionTool;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditManager;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditPolicy;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditorLocator;
import org.knime.workbench.editor2.figures.NodeAnnotationFigure;
import org.knime.workbench.editor2.figures.WorkflowAnnotationFigure;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationEditPart extends AbstractWorkflowEditPart implements
        NodeUIInformationListener, IPropertyChangeListener {

    private static final Color DEFAULT_FG = ColorConstants.black;

    /** White (since annotations have borders the default color is white) */
    public static final Color DEFAULT_BG_WORKFLOW = new Color(null, 255, 255, 255);

    /** default Color of the border for workflow annotations. Light yellow.*/
    private static final Color DEFAULT_BORDER_WORKFLOW = new Color(null, 255, 216, 0);

    /** White. */
    public static final Color DEFAULT_BG_NODE = new Color(null, 255, 255, 255);

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
     * @return the height of one line in default font (doesn't honor label size
     *         setting in preference page)
     */
    public static int workflowAnnotationDefaultOneLineHeight() {
        TextUtilities iNSTANCE = TextUtilities.INSTANCE;
        Font font = getWorkflowAnnotationDefaultFont();
        return iNSTANCE.getStringExtents("Agq|_ÊZ", font).height;
    }

    /**
     * @return the height of one line in default font for node annotations -
     *         uses the preference page setting to determine the font size
     */
    public static int nodeAnnotationDefaultOneLineHeight() {
        Font font = getNodeAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents("Agq|_ÊZ", font).height;
    }

    /**
     * @param text to test
     * @return the width of the specified text with the workflow annotation
     *         default font
     */
    public static int workflowAnnotationDefaultLineWidth(final String text) {
        Font font = getWorkflowAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents(text, font).width;
    }

    /**
     * @param text to test
     * @return the width of the specified text with the workflow annotation
     *         default font
     */
    public static int nodeAnnotationDefaultLineWidth(final String text) {
        Font font = getNodeAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents(text, font).width;
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
     * @return the default color of the workflow annotation border.
     */
    public static Color getAnnotationDefaultBorderColor() {
        return DEFAULT_BORDER_WORKFLOW;
    }

    /** @return the value set in the preference page for the default border width. */
    public static int getAnnotationDefaultBorderSizePrefValue() {
        // get workflow annotation border size from preferences
        IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return store.getInt(PreferenceConstants.P_ANNOTATION_BORDER_SIZE);
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
     * If no font is set, this one should be used for workflow annotations.
     *
     * @return the default font for workflow annotation
     */
    public static Font getWorkflowAnnotationDefaultFont() {
        // its the default font.
        return FontStore.INSTANCE.getDefaultFont(FontStore.getFontSizeFromKNIMEPrefPage());
    }

    /**
     * If no font is set, this one should be used for workflow annotations.
     * @param size size
     * @return the default font for workflow annotation
     */
    public static Font getWorkflowAnnotationDefaultFont(final int size) {
        // its the default font.
        return FontStore.INSTANCE.getDefaultFont(size);
    }


    /**
    * If no font is set, this one should be used for node annotations.
    * page for node labels.
    *
    * @return the default font for node annotation
    */
   public static Font getNodeAnnotationDefaultFont() {
       Font defFont = FontStore.INSTANCE.getDefaultFont(FontStore.getFontSizeFromKNIMEPrefPage());
       return defFont;
   }

    private AnnotationEditManager m_directEditManager;

    /** {@inheritDoc} */
    @Override
    public IAnnotation getModel() {
        return (IAnnotation)super.getModel();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        IAnnotation anno = getModel();
        NodeAnnotationFigure f = new WorkflowAnnotationFigure(anno);
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

        IAnnotation anno = getModel();
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
        IAnnotation anno = getModel();
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
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new AnnotationEditPolicy());
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        IAnnotation anno = getModel();
        NodeAnnotationFigure annoFig = (NodeAnnotationFigure)getFigure();
        annoFig.newContent(anno);
        WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
        parent.setLayoutConstraint(this, getFigure(), new Rectangle(
                anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight()));
        refreshVisuals();
        parent.refresh();
    }


    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent p) {
        if (p.getProperty().equals(PreferenceConstants.P_DEFAULT_NODE_LABEL)
                || p.getProperty().equals(PreferenceConstants.P_ANNOTATION_BORDER_SIZE)) {
            NodeAnnotationFigure fig = (NodeAnnotationFigure)getFigure();
            fig.newContent(getModel());
        }
    }

    /** Returns the text contained in the annotation or the default text if
     * the argument annotation is a default node annotation ("Node 1", "Node 2",
     * ...).
     * @param t The annotation, not null.
     * @return The above text. */
    public static String getAnnotationText(final IAnnotation t) {
        if (!isDefaultNodeAnnotation(t)) {
            return t.getText();
        }
        String text;
        if (((INodeAnnotation)t).getNodeContainer() == null) {
            return "";
        }
        int id = ((INodeAnnotation)t).getNodeContainer().getID().getIndex();
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
    public static boolean isDefaultNodeAnnotation(final IAnnotation t) {
        return t instanceof INodeAnnotation
        && (((INodeAnnotation)t).getData()).isDefault();
    }

    public static StyleRange[] toSWTStyleRanges(final AnnotationData t,
            final Font defaultFont) {
        AnnotationData.StyleRange[] knimeStyleRanges = t.getStyleRanges();
        ArrayList<StyleRange> swtStyleRange =
                new ArrayList<StyleRange>(knimeStyleRanges.length);
        for (AnnotationData.StyleRange knimeSR : knimeStyleRanges) {
            StyleRange swtStyle = new StyleRange();
            Font f = FontStore.INSTANCE.getAnnotationFont(knimeSR, defaultFont);
            swtStyle.font = f;
            if (knimeSR.getFgColor() >= 0) {
                int rgb = knimeSR.getFgColor();
                RGB rgbObj = RGBintToRGBObj(rgb);
                swtStyle.foreground = new Color(null, rgbObj);
            }
            swtStyle.start = knimeSR.getStart();
            swtStyle.length = knimeSR.getLength();
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
        AnnotationData.Builder result = AnnotationData.builder();
        result.setText(s.getText());
        result.setBgColor(colorToRGBint(s.getBackground()));
        result.setBorderColor(AnnotationEditPart.colorToRGBint(s.getMarginColor()));
        result.setBorderSize(s.getRightMargin()); // annotations have the same margin top/left/right/bottom.
        result.setDefaultFontSize(s.getFont().getFontData()[0].getHeight());
        AnnotationData.TextAlignment alignment;
        switch (s.getAlignment()) {
        case SWT.RIGHT:
            alignment = AnnotationData.TextAlignment.RIGHT;
            break;
        case SWT.CENTER:
            alignment = AnnotationData.TextAlignment.CENTER;
            break;
        default:
            alignment = AnnotationData.TextAlignment.LEFT;
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
            AnnotationData.StyleRange.Builder waSr = AnnotationData.StyleRange.builder();
            Color fg = sr.foreground;
            if (fg != null) {
                int rgb = colorToRGBint(fg);
                waSr.setFgColor(rgb);
            }
            FontStore.saveAnnotationFontToStyleRange(waSr, sr.font);
            waSr.setStart(sr.start);
            waSr.setLength(sr.length);
            wfStyleRanges.add(waSr.build());
        }
        result.setStyleRanges(wfStyleRanges
                .toArray(new AnnotationData.StyleRange[wfStyleRanges.size()]));
        return result.build();
    }

    @Override
    public void performRequest(final Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            // caused by a double click on this edit part
            performEdit();
            // we ignore REQ_DIRECT_EDIT as we want to allow editing only after a double-click
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
                                    (NodeAnnotationFigure)getFigure()));
        }

        m_directEditManager.show();
    }

    /** Extract the WorkflowAnnotation models from the argument list. It will
     * ignore NodeAnnotations (which have the same edit part).
     * @param annoParts The selected annotation parts
     * @return The workflow annotation models (possibly fewer than selected
     * edit parts!!!)
     */
    public static IWorkflowAnnotation[] extractWorkflowAnnotations(
            final AnnotationEditPart[] annoParts) {
        List<IWorkflowAnnotation> annoList = new ArrayList<IWorkflowAnnotation>();
        for (int i = 0; i < annoParts.length; i++) {
            IAnnotation model = annoParts[i].getModel();
            if (model instanceof IWorkflowAnnotation) {
                annoList.add((IWorkflowAnnotation)model);
            }
        }
        return annoList.toArray(new IWorkflowAnnotation[annoList.size()]);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelected(final int value) {
        super.setSelected(value);
        if (value != SELECTED_NONE) {
            // remove the icon if the annotation is selected (the erase target feedback removes it only when the
            // mouse is moved outside the figure - which is too late)
            IFigure f = getFigure();
            if (f instanceof WorkflowAnnotationFigure) {
                ((WorkflowAnnotationFigure)f).showEditIcon(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTargetFeedback(final Request request) {
        if (getSelected() == SELECTED_NONE && request.getType().equals(REQ_SELECTION)) {
            IFigure f = getFigure();
            if (f instanceof WorkflowAnnotationFigure) {
                ((WorkflowAnnotationFigure)f).showEditIcon(true);
            }
        }
        super.showTargetFeedback(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eraseTargetFeedback(final Request request) {
        if (request.getType().equals(REQ_SELECTION)) {
            IFigure f = getFigure();
            if (f instanceof WorkflowAnnotationFigure) {
                ((WorkflowAnnotationFigure)f).showEditIcon(false);
            }
        }
        super.eraseTargetFeedback(request);
    }

    /**
     * {@inheritDoc}
     * If dragging started on the "move" icon (top left corner) return the normal edit part dragger tool, otherwise
     * return the marquee selection tool.
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        Object object = request.getExtendedData().get(WorkflowSelectionTool.DRAG_START_LOCATION);
        IFigure f = getFigure();
        if (object instanceof Point && f instanceof WorkflowAnnotationFigure && getSelected() == SELECTED_NONE) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorSite();
            Rectangle iconBounds = ((WorkflowAnnotationFigure)f).getEditIconBounds().getCopy();
            if (!iconBounds.contains((Point)object)) {
                return new WorkflowMarqueeSelectionTool();
            }
        }
        // "normal" edit part dragging
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

}
