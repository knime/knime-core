/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

import org.knime.core.node.GenericNodeView;

/**
 * Base class for both output views. Provides a text field and the ability to
 * add a line to this field, or to update the entire field.
 *
 * @author ohl, University of Konstanz
 * @param <T> the actual implementation of the abstract node model
 */
public abstract class ExtToolOutputNodeView<T extends ExtToolOutputNodeModel>
        extends GenericNodeView<T> {

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
        setViewTitle("Output to StdErr");
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
