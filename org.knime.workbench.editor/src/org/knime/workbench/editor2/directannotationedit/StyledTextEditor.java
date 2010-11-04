/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.commands.AddAnnotationCommand;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class StyledTextEditor extends CellEditor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(StyledTextEditor.class);

    /** height of the font style toolbar in the editor control */
    public static final int TOOLBAR_HEIGHT = 16;

    private StyledText m_styledText;

    private Composite m_toolbar;

    private Composite m_styleButtons;

    private Composite m_panel;

    private Color m_backgroundColor = null;

    private double m_zoomFactor = 1.0;

    private final AtomicBoolean m_commitOnFocusLost = new AtomicBoolean(false);

    /**
     *
     */
    public StyledTextEditor() {
        super();
    }

    /**
     * @param parent
     */
    public StyledTextEditor(final Composite parent) {
        super(parent);
    }

    /**
     * @param parent
     * @param style
     */
    public StyledTextEditor(final Composite parent, final int style) {
        super(parent, style);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_panel = new Composite(parent, SWT.NONE);
        m_panel.setLayout(createPanelGridLayout(1, true));

        // create toolbar first!
        createToolbar(m_panel);
        createStyledText(m_panel);
        applyBackgroundColor();
        return m_panel;
    }

    private GridLayout createPanelGridLayout(final int columns,
            final boolean equalWith) {
        GridLayout layout = new GridLayout(columns, equalWith);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginBottom = 0;
        layout.marginHeight = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginWidth = 0;
        layout.marginBottom = 0;
        return layout;
    }

    private Control createStyledText(final Composite parent) {
        m_styledText =
                new StyledText(parent, SWT.MULTI | SWT.WRAP
                        | SWT.FULL_SELECTION);
        m_styledText.setFont(parent.getFont());
        m_styledText.setAlignment(SWT.LEFT);
        m_styledText.setText("");
        // forward some events to the cell editor
        m_styledText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                keyReleaseOccured(e);
            }
        });
        m_styledText.addFocusListener(new FocusAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void focusLost(final org.eclipse.swt.events.FocusEvent e) {
                // close the editor and commit changes if focus is lost.
                // BUT only if the focus was not set on any other component
                // in the panel (these components clear the flag in their
                // focusGained).
                m_commitOnFocusLost.set(true);

                Display.getCurrent().asyncExec(new Runnable() {
                    // execute this only after the focus event chain is done
                    @Override
                    public void run() {
                        if (m_commitOnFocusLost.get()) {
                            lostFocus();
                        }
                    }
                });
            }
        });
        m_styledText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                // super marks it dirty (otherwise no commit at the end)
                fireEditorValueChanged(true, true);
            }
        });
        m_styledText.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectionChanged();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                selectionChanged();
            }
        });
        m_styledText
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        // toolbar gets created first - enable its style buttons!
        selectionChanged();
        return m_styledText;
    }

    private void selectionChanged() {
        boolean enabled = true;
        int[] sel = m_styledText.getSelectionRanges();
        if (sel == null || sel.length != 2) {
            enabled = false;
        } else {
            int length = sel[1];
            enabled = (length > 0);
        }
        enableStyleButtons(enabled);
    }

    private void enableStyleButtons(final boolean enableThem) {
        for (Control b : m_styleButtons.getChildren()) {
            b.setEnabled(enableThem);
        }
    }

    private Control createToolbar(final Composite parent) {
        ImageDescriptor imgDescr;
        Image img;

        // mother of all buttons
        m_toolbar = new Composite(parent, SWT.NONE);
        m_toolbar.setLayout(createPanelGridLayout(4, false));

        // contains buttons being en/disabled with selection
        m_styleButtons = new Composite(m_toolbar, SWT.NONE);
        m_styleButtons.setLayout(createPanelGridLayout(4, true));

        // font/style button
        createButton(m_styleButtons, "style", "F", null,
                "Font - change selection font");
        // foreground color button
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/color_10.png");
        img = imgDescr.createImage();
        createButton(m_styleButtons, "color", null, img,
                "Color - change selection font color");
        // bold button
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/bold_10.png");
        img = imgDescr.createImage();
        createButton(m_styleButtons, "bold", null, img,
                "Bold - change selection font style");
        // italic button
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/italic_10.png");
        img = imgDescr.createImage();
        createButton(m_styleButtons, "italic", null, img,
                "Italic - change selection font style");
        // background color
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/bgcolor_10.png");
        img = imgDescr.createImage();
        createButton(m_toolbar, "bg", null, img,
                "Background - change the background color");
        // ok button
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/ok_10.png");
        img = imgDescr.createImage();
        createButton(m_toolbar, "ok", null, img, "OK - commit changes");
        // cancel button
        imgDescr =
                ImageRepository
                        .getImageDescriptor("icons/annotations/cancel_10.png");
        img = imgDescr.createImage();
        createButton(m_toolbar, "cancel", null, img, "Cancel - discard changes");

        return m_toolbar;
    }

    private Button createButton(final Composite parent, final String id,
            final String text, final Image icon, final String tooltip) {
        final Button b = new Button(parent, SWT.PUSH);
        b.setData("id", id);

        GridData buttonData = new GridData();
        buttonData.grabExcessHorizontalSpace = false;
        buttonData.grabExcessVerticalSpace = false;
        buttonData.heightHint = TOOLBAR_HEIGHT;
        buttonData.widthHint = TOOLBAR_HEIGHT;
        b.setLayoutData(buttonData);
        b.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                buttonClick((String)b.getData("id"));
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                buttonClick((String)b.getData("id"));
            }
        });
        if (text != null) {
            b.setText(text);
        }
        b.setToolTipText(tooltip);
        b.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                // focus stays in the editor panel - don't close it
                m_commitOnFocusLost.set(false);
            }
        });
        if (icon != null) {
            b.setImage(icon);
        }
        return b;
    }

    private void buttonClick(final String src) {
        if (src.equals("style")) {
            font();
        } else if (src.equals("color")) {
            fontColor();
        } else if (src.equals("bold")) {
            bold();
        } else if (src.equals("italic")) {
            italic();
        } else if (src.equals("bg")) {
            bgColor();
        } else if (src.equals("ok")) {
            ok();
        } else if (src.equals("cancel")) {
            cancel();
        } else {
            LOGGER.coding("IMPLEMENTATION ERROR: Wrong button ID");
        }

        // set the focus back to the editor after the buttons finish
        if (!src.equals("ok") && !src.equals("cancel")) {
            m_styledText.setFocus();
        }

    }

    private void applyBackgroundColor() {
        if (m_backgroundColor != null && m_panel != null) {
            LinkedList<Composite> comps = new LinkedList<Composite>();
            comps.add(m_panel);
            while (!comps.isEmpty()) {
                // set the composite's bg
                Composite c = comps.pollFirst();
                c.setBackgroundMode(SWT.INHERIT_NONE);
                c.setBackground(m_backgroundColor);
                // and the bg all of its children
                Control[] children = c.getChildren();
                for (Control child : children) {
                    if (child instanceof Composite) {
                        comps.add((Composite)child);
                    } else {
                        child.setBackground(m_backgroundColor);
                    }
                }
            }
        }
    }

    public void setBackgroundColor(final Color bg) {
        m_backgroundColor = bg;
        applyBackgroundColor();
    }

    /**
     * Font size is multiplied by the zoom factor. MUST be set before setValue!
     * Does not apply to already set contents!
     *
     * @param factor defaults to 1.0 (=100%)
     */
    public void setZoomFactor(final double factor) {
        m_zoomFactor = factor;
    }

    /**
     * {@inheritDoc}
     */
    protected void lostFocus() {
        super.focusLost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void keyReleaseOccured(final KeyEvent keyEvent) {
        if (keyEvent.character == SWT.CR) { // Return key
            // don't let super close the editor on CR
            if ((keyEvent.stateMask & SWT.CTRL) != 0) {

                // closing the editor with Ctrl-CR.
                keyEvent.doit = false;
                fireApplyEditorValue();
                deactivate();
                return;
            }
        } else {
            super.keyReleaseOccured(keyEvent);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link WorkflowAnnotation} with the new text and style ranges -
     *         and with the same ID as the original annotation (the one the
     *         editor was initialized with) - but in a new object.
     */
    @Override
    protected Object doGetValue() {
        assert m_styledText != null : "Control not created!";
        return AnnotationEditPart.toAnnotation(m_styledText, m_zoomFactor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetFocus() {
        assert m_styledText != null : "Control not created!";
        String text = m_styledText.getText();
        if (text.equals(AddAnnotationCommand.INITIAL_TEXT)) {
            m_styledText.setSelection(0, text.length());
            selectionChanged();
        }
        m_styledText.setFocus();
        m_styledText.setCaretOffset(text.length());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetValue(final Object value) {
        assert value instanceof WorkflowAnnotation : "Wrong value object!";
        WorkflowAnnotation wa = (WorkflowAnnotation)value;
        m_styledText.setText(wa.getText());
        m_styledText.setStyleRanges(AnnotationEditPart.toSWTStyleRanges(wa,
                m_zoomFactor));
        m_styledText.setBackground(AnnotationEditPart.RGBintToColor(wa
                .getBgColor()));
    }

    private void bold() {
        setSWTStyle(SWT.BOLD);
    }

    private void setSWTStyle(final int SWTstyle) {
        List<StyleRange> styles = getStylesInSelection();
        boolean setAttr = true;
        for (StyleRange s : styles) {
            if (s.font != null
                    && (s.font.getFontData()[0].getStyle() & SWTstyle) != 0) {
                setAttr = false;
                break;
            }
        }
        for (StyleRange s : styles) {
            if (setAttr) {
                s.font =
                        AnnotationEditPart.FONT_STORE.addStyleToFont(s.font,
                                SWTstyle);
            } else {
                s.font =
                        AnnotationEditPart.FONT_STORE.removeStyleFromFont(
                                s.font, SWTstyle);
            }
            m_styledText.setStyleRange(s);
        }
    }

    /**
     * Returns a list of ordered styles in the selected range. For regions in
     * the selection that do not have a style yet, it inserts a new (empty)
     * style. The styles are ordered and not overlapping. If there is no
     * selection in the control, an empty list is returned, never null.
     * Contained styles should be applied individually (after possible
     * modification) with setStyleRange().
     *
     * @return styles for the entire selected range, ordered and not
     *         overlapping. Empty list, if no selection exists, never null.
     */
    private List<StyleRange> getStylesInSelection() {
        int[] sel = m_styledText.getSelectionRanges();
        if (sel == null || sel.length != 2) {
            return Collections.emptyList();
        }
        int start = sel[0];
        int length = sel[1];
        StyleRange[] styles = m_styledText.getStyleRanges(start, length);
        if (styles == null || styles.length == 0) {
            // no existing styles in selection
            StyleRange newStyle = new StyleRange();
            newStyle.start = start;
            newStyle.length = length;
            return Collections.singletonList(newStyle);
        } else {
            LinkedList<StyleRange> result = new LinkedList<StyleRange>();
            int lastEnd = start; // not yet covered index
            for (StyleRange s : styles) {
                if (s.start < lastEnd) {
                    LOGGER.error("StyleRanges not ordered! "
                            + "Style might be messed up");
                }
                if (lastEnd < s.start) {
                    // create style for range not covered by next exiting style
                    StyleRange newRange = new StyleRange();
                    newRange.start = lastEnd;
                    newRange.length = s.start - lastEnd;
                    lastEnd = s.start;
                    result.add(newRange);
                }
                result.add(s);
                lastEnd = s.start + s.length;
            }
            if (lastEnd < start + length) {
                // create new style for the part at the end, not covered
                StyleRange newRange = new StyleRange();
                newRange.start = lastEnd;
                newRange.length = start + length - lastEnd;
                result.add(newRange);
            }
            return result;
        }
    }

    private void italic() {
        setSWTStyle(SWT.ITALIC);
    }

    private void bgColor() {
        ColorDialog colDlg = new ColorDialog(m_styledText.getShell());
        colDlg.setText("Change the Background Color");
        if (m_backgroundColor != null) {
            colDlg.setRGB(m_backgroundColor.getRGB());
        }
        RGB newBGCol = colDlg.open();
        if (newBGCol == null) {
            // user canceled
            return;
        }
        m_backgroundColor = new Color(null, newBGCol);
        applyBackgroundColor();
    }

    private void fontColor() {
        Color col = AnnotationEditPart.getAnnotationDefaultForegroundColor();
        List<StyleRange> sel = getStylesInSelection();
        // set the color of the first selection style
        for (StyleRange style : sel) {
            if (style.foreground != null) {
                col = style.foreground;
                break;
            }
        }
        ColorDialog colDlg = new ColorDialog(m_styledText.getShell());
        colDlg.setText("Change Font Color in Selection");
        colDlg.setRGB(col.getRGB());
        RGB newRGB = colDlg.open();
        if (newRGB == null) {
            // user canceled
            return;
        }
        Color newCol = AnnotationEditPart.RGBtoColor(newRGB);
        for (StyleRange style : sel) {
            style.foreground = newCol;
            m_styledText.setStyleRange(style);
        }
    }

    private void font() {
        List<StyleRange> sel = getStylesInSelection();
        Font f = AnnotationEditPart.getAnnotationDefaultFont();
        // set the first font in the selection
        for (StyleRange style : sel) {
            if (style.font != null) {
                f = style.font;
                break;
            }
        }
        FontDialog fd = new FontDialog(m_styledText.getShell());
        fd.setText("Change Font in Selection");
        FontData[] dlgFontData = f.getFontData();
        fd.setFontList(dlgFontData);
        FontData newFontData = fd.open();
        if (newFontData == null) {
            // user canceled
            return;
        }
        RGB newRGB = fd.getRGB();
        Color newCol =
                newRGB == null ? null : AnnotationEditPart.RGBtoColor(newRGB);
        for (StyleRange style : sel) {
            style.font =
                    AnnotationEditPart.FONT_STORE.getFont(
                            newFontData.getName(), newFontData.getHeight(),
                            newFontData.getStyle());
            if (newCol != null) {
                style.foreground = newCol;
            }
            m_styledText.setStyleRange(style);
        }
    }

    private void ok() {
        fireApplyEditorValue();
        deactivate();
        return;
    }

    private void cancel() {
        fireCancelEditor();
        deactivate();
        return;
    }
}
