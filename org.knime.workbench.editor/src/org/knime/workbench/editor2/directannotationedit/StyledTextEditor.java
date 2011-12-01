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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
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

    private StyledText m_styledText;

    private List<MenuItem> m_styleMenuItems;

    private Composite m_panel;

    private Color m_backgroundColor = null;

    private final AtomicBoolean m_allowFocusLost = new AtomicBoolean(true);

    private MenuItem m_rightAlignMenuItem;

    private MenuItem m_centerAlignMenuItem;

    private MenuItem m_leftAlignMenuItem;

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
        m_styledText = new StyledText(parent, SWT.MULTI | SWT.WRAP
                | SWT.FULL_SELECTION);
        m_styledText.setFont(parent.getFont());
        m_styledText.setAlignment(SWT.LEFT);
        m_styledText.setText("");
        // somehow that matches the tab indent of the figure...
        m_styledText.setTabs(16);
        m_styledText.addVerifyKeyListener(new VerifyKeyListener() {
            @Override
            public void verifyKey(final VerifyEvent event) {
                // pressing DEL at the end of the text closes the editor!
                if (event.keyCode == SWT.DEL
                        && m_styledText.getCaretOffset() == m_styledText
                                .getText().length()) {
                    // ignore the DEL at the end of the text
                    event.doit = false;
                }
            }
        });
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
                // close the editor only if called directly (not as a side
                // effect of an opening font editor, for instance)
                if (m_allowFocusLost.get()) {
                    lostFocus();
                }
            }
        });
        m_styledText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                // super marks it dirty (otherwise no commit at the end)
                fireEditorValueChanged(true, true);
            }
        });
        m_styledText.addExtendedModifyListener(new ExtendedModifyListener() {
            @Override
            public void modifyText(final ExtendedModifyEvent event) {
                if (event.length > 0) {
                    textInserted(event.start, event.length);
                }
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
        m_styledText.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true));
        addMenu(m_styledText);
        // toolbar gets created first - enable its style buttons!
        selectionChanged();
        return m_styledText;
    }

    /**
     * Sets the style range for the new text. Copies it from the left neighbor
     * (or from the right neighbor, if there is no left neighbor).
     *
     * @param startIdx
     * @param length
     */
    private void textInserted(final int startIdx, final int length) {
        if (m_styledText.getCharCount() <= length) {
            // no left nor right neighbor
            return;
        }
        StyleRange[] newStyles = m_styledText.getStyleRanges(startIdx, length);
        if (newStyles != null && newStyles.length > 0 && newStyles[0] != null) {
            // inserted text already has a style (shouldn't really happen)
            return;
        }
        StyleRange[] extStyles;
        if (startIdx == 0) {
            extStyles = m_styledText.getStyleRanges(length, 1);
        } else {
            extStyles = m_styledText.getStyleRanges(startIdx - 1, 1);
        }
        if (extStyles == null || extStyles.length != 1 || extStyles[0] == null) {
            // no style to extend over inserted text
            return;
        }
        if (startIdx == 0) {
            extStyles[0].start = 0;
        }
        extStyles[0].length += length;
        m_styledText.setStyleRange(extStyles[0]);
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
        if (m_styleMenuItems != null) {
            for (MenuItem action : m_styleMenuItems) {
                action.setEnabled(enableThem);
            }
        }
    }

    private void addMenu(final Composite parent) {
        Menu menu = new Menu(parent);
        Image img;

        // background color
        img = ImageRepository.getImage("icons/annotations/bgcolor_10.png");
        addMenuItem(menu, "bg", SWT.PUSH, "Background", img);

        // alignment
        img = ImageRepository.getImage("icons/annotations/alignment_10.png");

        MenuItem alignmentMenuItem = addMenuItem(menu, "alignment",
                SWT.CASCADE, "Alignment", img);

        final Menu alignMenu = new Menu(alignmentMenuItem);
        alignmentMenuItem.setMenu(alignMenu);

        m_leftAlignMenuItem = addMenuItem(alignMenu, "alignment_left",
                SWT.RADIO, "Left", null);

        m_centerAlignMenuItem = addMenuItem(alignMenu, "alignment_center",
                SWT.RADIO, "Center", null);

        m_rightAlignMenuItem = addMenuItem(alignMenu, "alignment_right",
                SWT.RADIO, "Right", null);

        new MenuItem(menu, SWT.SEPARATOR);
        // contains buttons being en/disabled with selection
        m_styleMenuItems = new ArrayList<MenuItem>();
        MenuItem action;

        // font/style button
        img = ImageRepository.getImage("icons/annotations/font_10.png");
        action = addMenuItem(menu, "style", SWT.PUSH, "Font", img);
        m_styleMenuItems.add(action);

        // foreground color button
        img = ImageRepository.getImage("icons/annotations/color_10.png");
        action = addMenuItem(menu, "color", SWT.PUSH, "Color", img);
        m_styleMenuItems.add(action);

        // bold button
        img = ImageRepository.getImage("icons/annotations/bold_10.png");
        action = addMenuItem(menu, "bold", SWT.PUSH, "Bold", img);
        m_styleMenuItems.add(action);

        // italic button
        img = ImageRepository.getImage("icons/annotations/italic_10.png");
        action = addMenuItem(menu, "italic", SWT.PUSH, "Italic", img);
        m_styleMenuItems.add(action);

        new MenuItem(menu, SWT.SEPARATOR);

        // ok button
        img = ImageRepository.getImage("icons/annotations/ok_10.png");
        addMenuItem(menu, "ok", SWT.PUSH, "OK (commit)", img);

        // cancel button
        img = ImageRepository.getImage("icons/annotations/cancel_10.png");
        addMenuItem(menu, "cancel", SWT.PUSH, "Cancel (discard)", img);

        parent.setMenu(menu);
    }

    private MenuItem addMenuItem(final Menu menuMgr, final String id,
            final int style, final String text, final Image img) {
        MenuItem menuItem = new MenuItem(menuMgr, style);
        SelectionAdapter selListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_allowFocusLost.set(false);
                try {
                    buttonClick(id);
                } finally {
                    m_allowFocusLost.set(true);
                }
            }
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                super.widgetSelected(e);
            }
        };
        menuItem.addSelectionListener(selListener);
        menuItem.setText(text);
        menuItem.setImage(img);
        return menuItem;
    }

    private void buttonClick(final String src) {
        if (src.equals("style")) {
            font();
            fireEditorValueChanged(true, true);
        } else if (src.equals("color")) {
            fontColor();
            fireEditorValueChanged(true, true);
        } else if (src.equals("bold")) {
            bold();
            fireEditorValueChanged(true, true);
        } else if (src.equals("italic")) {
            italic();
            fireEditorValueChanged(true, true);
        } else if (src.equals("bg")) {
            bgColor();
            fireEditorValueChanged(true, true);
        } else if (src.equals("alignment_left")) {
            alignment(SWT.LEFT);
            fireEditorValueChanged(true, true);
        } else if (src.equals("alignment_center")) {
            alignment(SWT.CENTER);
            fireEditorValueChanged(true, true);
        } else if (src.equals("alignment_right")) {
            alignment(SWT.RIGHT);
            fireEditorValueChanged(true, true);
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
     * @return a {@link AnnotationData} with the new text and style ranges -
     *         and with the same ID as the original annotation (the one the
     *         editor was initialized with) - but in a new object.
     */
    @Override
    protected Object doGetValue() {
        assert m_styledText != null : "Control not created!";
        return AnnotationEditPart.toAnnotationData(m_styledText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetFocus() {
        assert m_styledText != null : "Control not created!";
        String text = m_styledText.getText();
        if (text.equals(AddAnnotationCommand.INITIAL_FLOWANNO_TEXT)) {
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
        assert value instanceof Annotation : "Wrong value object!";
        Annotation wa = (Annotation)value;
        int alignment;
        switch (wa.getAlignment()) {
        case CENTER:
            alignment = SWT.CENTER;
            break;
        case RIGHT:
            alignment = SWT.RIGHT;
            break;
        default:
            alignment = SWT.LEFT;
        }
        checkSelectionOfAlignmentMenuItems(alignment);
        String text;
        if (AnnotationEditPart.isDefaultNodeAnnotation(wa)) {
            text = AnnotationEditPart.getAnnotationText(wa);
        } else {
            text = wa.getText();
        }
        m_styledText.setAlignment(alignment);
        m_styledText.setText(text);
        m_styledText.setStyleRanges(AnnotationEditPart.toSWTStyleRanges(
                wa.getData()));
        setBackgroundColor(AnnotationEditPart.RGBintToColor(wa
                .getBgColor()));
    }

    private void bold() {
        setSWTStyle(SWT.BOLD);
    }

    /** Update selection state of alignment buttons in menu.
     * @param swtAlignment SWT.LEFT, CENTER, or RIGHT
     */
    private void checkSelectionOfAlignmentMenuItems(final int swtAlignment) {
        MenuItem[] alignmentMenuItems = new MenuItem[] {
                m_leftAlignMenuItem, m_centerAlignMenuItem, m_rightAlignMenuItem};
        MenuItem activeMenuItem;
        switch (swtAlignment) {
        case SWT.LEFT:
            activeMenuItem = m_leftAlignMenuItem;
            break;
        case SWT.CENTER:
            activeMenuItem = m_centerAlignMenuItem;
            break;
        case SWT.RIGHT:
            activeMenuItem = m_rightAlignMenuItem;
            break;
        default:
            LOGGER.coding("Invalid alignment (ignored): " + swtAlignment);
            return;
        }
        for (MenuItem m : alignmentMenuItems) {
            m.setSelection(m == activeMenuItem);
        }
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

    /** Change alignment.
     * @param alignment SWT.LEFT|CENTER|RIGHT.
     */
    private void alignment(final int alignment) {
        int newAlignment;
        switch (alignment) {
        case SWT.CENTER:
            newAlignment = alignment;
            break;
        case SWT.RIGHT:
            newAlignment = alignment;
            break;
        default:
            newAlignment = SWT.LEFT;
        }
        checkSelectionOfAlignmentMenuItems(newAlignment);
        m_styledText.setAlignment(newAlignment);
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
        m_allowFocusLost.set(false);
        FontData newFontData;
        try {
            newFontData = fd.open();
        } finally {
            m_allowFocusLost.set(true);
        }
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

    /**
     * @return the bounds needed to display the current text
     */
    Rectangle getTextBounds() {
        int charCount = m_styledText.getCharCount();
        if (charCount < 1) {
            Rectangle b = m_styledText.getBounds();
            return new Rectangle(b.x, b.y, 0, 0);
        } else {
            Rectangle r = m_styledText.getTextBounds(0, charCount - 1);
            if (m_styledText.getText(charCount - 1, charCount - 1).charAt(0) == '\n') {
                r.height += m_styledText.getLineHeight();
            }
            return r;
        }
    }

}
