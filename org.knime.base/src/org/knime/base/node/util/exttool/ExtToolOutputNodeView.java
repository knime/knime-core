/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 * History
 *   28.08.2007 (ohl): created
 */
package org.knime.base.node.util.exttool;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.knime.core.node.NodeView;

/**
 * Base class for both output views. Provides a text field and the ability to
 * add a line to this field, or to update the entire field.
 *
 * @author ohl, University of Konstanz
 * @param <T> the actual implementation of the abstract node model
 */
public abstract class ExtToolOutputNodeView<T extends ExtToolOutputNodeModel>
        extends NodeView<T> {

    private final JTextArea m_output;

    private int m_numOfLines;

    private final Color m_colorGray;

    private final Color m_colorDefault;

    /**
     * The constructor.
     *
     * @param nodeModel the model associated with this view.
     */
    public ExtToolOutputNodeView(final T nodeModel) {
        super(nodeModel);
        m_output = new JTextArea();
        m_numOfLines = 0;
        m_colorGray = Color.GRAY;
        m_colorDefault = m_output.getForeground();
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    init();
                }
            });
        } else {
            init();
        }

    }

    /**
     * initializes the components.
     */
    private void init() {
        m_output.setEditable(false);
        m_output.setColumns(80);
        m_output.setRows(50);
        // show the output even if the node is not executed (failed executing)
        setShowNODATALabel(false);
        setComponent(new JScrollPane(m_output));

    }

    /**
     * This method is called whenever the entire text has changed (like after a
     * reset, load, execute, etc.). It removes the current content and retrieves
     * the new output to display from the node model.
     *
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        Collection<String> output = getFullOutput();

        // if the current output is empty, see if we have output from any
        // previously failing execution
        Color col = m_colorDefault;
        if (output.isEmpty()) {
            Collection<String> failedErr = getFullFailureOutput();
            if ((failedErr != null) && !failedErr.isEmpty()) {
                output = failedErr;
                col = m_colorGray;
            }
        }

        if ((output == null) || (output.size() == 0)) {
            // get the "empty output" text
            output = getNoOutputText();
        }

        setTextInSwingThreadLater(output, col);
    }

    private void setTextInSwingThreadLater(final Collection<String> lines,
            final Color fgColor) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setText(lines, fgColor);
                }
            });
        } else {
            setText(lines, fgColor);
        }
    }

    private void setText(final Collection<String> lines, final Color fgCol) {

        // clear any previous output
        m_output.setText("");
        m_numOfLines = 0;

        m_output.setForeground(fgCol);
        for (String line : lines) {
            addLine(line);
        }
    }

    /**
     * adds the specified line (plus \n) to the end of the Textfield. If the
     * field contains more than 500 lines, it removes the first line.<br>
     * Call this method if you are not sure in which thread you are.
     *
     * @param s the line to add
     */
    protected void addLineInSwingThreadLater(final String s) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    addLine(s);
                }
            });
        } else {
            addLine(s);
        }
    }

    /**
     * removes all output from the view.
     */
    protected void clearText() {
        setTextInSwingThreadLater(
                Arrays.asList(new String[]{}), m_colorDefault);
    }

    /**
     * adds the specified line (plus \n) to the end of the Textfield. If the
     * field contains more than 500 lines, it removes the first line.<br>
     * Call it in the Swing Thread only.
     *
     * @see #addLineInSwingThreadLater(String)
     * @param s the line to add
     */
    private void addLine(final String s) {

        PlainDocument doc = (PlainDocument)m_output.getDocument();

        try {
            // insertString is thread safe
            doc.insertString(doc.getEndPosition().getOffset(), s, null);
            doc.insertString(doc.getEndPosition().getOffset(), "\n", null);
            m_numOfLines++;
            if (m_numOfLines > CommandExecution.MAX_OUTLINES_STORED) {
                removefirstLine();
            }
        } catch (BadLocationException e) {
            m_output.setText(m_output.getText() + "\n[...and more]");
            m_numOfLines++;
            return;
        }
    }

    private void removefirstLine() {
        PlainDocument doc = (PlainDocument)m_output.getDocument();

        int maxPos = doc.getEndPosition().getOffset();
        boolean done = false;
        int lfPos = 0;
        try {
            while (!done) {
                // run through chunks of 80 chars until we find the LF
                int length = Math.min(80, maxPos - lfPos);

                String s = doc.getText(lfPos, length);
                for (int c = 0; c < s.length(); c++) {
                    if (s.charAt(c) == '\n') {
                        doc.remove(0, lfPos + 1);
                        m_numOfLines--;
                        done = true;
                        break;
                    }

                    lfPos++;
                }

                if (lfPos == maxPos) {
                    // then there is no linefeed in the output
                    m_output.setText("");
                    m_numOfLines = 0;
                }

            }
        } catch (BadLocationException e) {
            // then we don't delete the line...
            System.err.println("Bad Location: " + e.getMessage());
            return;
        }

    }

    /**
     * @return the entire list of strings to display in this view.
     */
    protected abstract Collection<String> getFullOutput();

    /**
     * @return the entire list of strings of a previous failed execution.
     */
    protected abstract Collection<String> getFullFailureOutput();

    /**
     * @return the text that should be displayed if none of the above methods
     *         returned any content.
     */
    protected abstract Collection<String> getNoOutputText();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty
    }

}
