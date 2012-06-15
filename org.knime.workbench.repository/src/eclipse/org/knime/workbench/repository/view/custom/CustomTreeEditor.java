/*
 * This piece of code was copied from http://www.eclipse.org/swt/snippets/#treeeditor
 * and slightly adapted.
 *
 *******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
 *
 * History
 *   01.06.2012 (meinl): created
 */
package org.knime.workbench.repository.view.custom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.CustomRepositoryManager;

/**
 * Editor for a Tree that lets the user change the text of items.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public class CustomTreeEditor implements Listener {
    private final Tree m_tree;

    private final TreeEditor m_editor;

    private TreeItem m_lastItem;

    /**
     * Creates a new editor. The editor is functional as soon as it is created.
     *
     * @param tree the tree for which this editor should be used.
     */
    public CustomTreeEditor(final Tree tree) {
        m_tree = tree;
        m_editor = new TreeEditor(tree);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(final Event event) {
        final TreeItem item = (TreeItem)event.item;
        if ((item != null)
                && (item == m_lastItem)
                && (item.getData() instanceof Category)
                && CustomRepositoryManager.isCustomCategory((Category)item
                        .getData())) {
            final Category cat = (Category)item.getData();

            boolean showBorder = true;
            final Composite composite = new Composite(m_tree, SWT.NONE);
            if (showBorder) {
                composite.setBackground(Display.getDefault().getSystemColor(
                        SWT.COLOR_BLACK));
            }
            final Text text = new Text(composite, SWT.NONE);
            final int inset = showBorder ? 1 : 0;
            composite.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(final Event e) {
                    Rectangle rect = composite.getClientArea();
                    text.setBounds(rect.x + inset, rect.y + inset, rect.width
                            - inset * 2, rect.height - inset * 2);
                }
            });
            Listener textListener = new Listener() {
                @Override
                public void handleEvent(final Event e) {
                    switch (e.type) {
                        case SWT.FocusOut:
                            cat.setName(text.getText());
                            item.setText(text.getText());
                            composite.dispose();
                            break;
                        case SWT.Verify:
                            String newText = text.getText();
                            String leftText = newText.substring(0, e.start);
                            String rightText =
                                    newText.substring(e.end, newText.length());
                            GC gc = new GC(text);
                            Point size =
                                    gc.textExtent(leftText + e.text + rightText);
                            gc.dispose();
                            size = text.computeSize(size.x, SWT.DEFAULT);
                            m_editor.horizontalAlignment = SWT.LEFT;
                            Rectangle itemRect = item.getBounds(),
                            rect = m_tree.getClientArea();
                            m_editor.minimumWidth =
                                    Math.max(size.x, itemRect.width) + inset
                                            * 2;
                            int left = itemRect.x,
                            right = rect.x + rect.width;
                            m_editor.minimumWidth =
                                    Math.min(m_editor.minimumWidth, right
                                            - left);
                            m_editor.minimumHeight = size.y + inset * 2;
                            m_editor.layout();
                            break;
                        case SWT.Traverse:
                            switch (e.detail) {
                                case SWT.TRAVERSE_RETURN:
                                    cat.setName(text.getText());
                                    item.setText(text.getText());
                                    // FALL THROUGH
                                case SWT.TRAVERSE_ESCAPE:
                                    composite.dispose();
                                    e.doit = false;
                            }
                            break;
                    }
                }
            };
            text.addListener(SWT.FocusOut, textListener);
            text.addListener(SWT.Traverse, textListener);
            text.addListener(SWT.Verify, textListener);
            m_editor.setEditor(composite, item);
            text.setText(item.getText());
            text.selectAll();
            text.setFocus();
        }
        m_lastItem = item;
    }

}
