/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 9, 2007 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author ohl, University of Konstanz
 */
class CommentPanel extends JPanel {

    private static final Dimension TEXTFIELDDIM = new Dimension(100, 25);

    private final JTextField m_commentBegin = new JTextField();

    private final JTextField m_commentEnd = new JTextField();

    private final JTextField m_commentLine = new JTextField();

    private final JCheckBox m_addDate = new JCheckBox();

    private final JCheckBox m_addUser = new JCheckBox();

    private final JCheckBox m_addTableName = new JCheckBox();

    private final JCheckBox m_addCustom = new JCheckBox();

    /**
     * 
     */
    public CommentPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(createWhatPanel());
        add(createCommentPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
    }

    private JPanel createCommentPanel() {

        Box textBox = Box.createHorizontalBox();
        textBox.add(new JLabel(
                "Specify the patterns that start and end a comment (block)"));
        textBox.add(Box.createHorizontalGlue()); // make it left aligned.

        Box patternBox = Box.createHorizontalBox();
        patternBox.add(Box.createHorizontalStrut(50));
        patternBox.add(new JLabel("Comment begin:"));
        patternBox.add(Box.createHorizontalStrut(4));
        m_commentBegin.setPreferredSize(TEXTFIELDDIM);
        m_commentBegin.setMaximumSize(TEXTFIELDDIM);
        patternBox.add(m_commentBegin);
        patternBox.add(Box.createHorizontalStrut(25));
        patternBox.add(new JLabel("Comment end:"));
        patternBox.add(Box.createHorizontalStrut(4));
        m_commentEnd.setToolTipText("Leave empty for single line comments");
        m_commentEnd.setPreferredSize(TEXTFIELDDIM);
        m_commentEnd.setMaximumSize(TEXTFIELDDIM);
        patternBox.add(m_commentEnd);
        patternBox.add(Box.createHorizontalGlue());
        patternBox.add(Box.createHorizontalGlue());

        JPanel commentPanel = new JPanel();
        commentPanel.setLayout(new BoxLayout(commentPanel, BoxLayout.Y_AXIS));
        commentPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Comment Pattern"));
        commentPanel.add(textBox);
        commentPanel.add(patternBox);

        return commentPanel;
    }

    private JPanel createWhatPanel() {

        m_addDate.setText("the current creation time");
        m_addDate.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        m_addUser.setText("the user account name");
        m_addUser.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        m_addTableName.setText("the input table name");
        m_addTableName.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        m_addCustom.setText("this text:");
        m_addCustom.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        m_commentLine.setToolTipText("Use \\n or \\t for a new line or the "
                + "tab character");
        m_commentLine.setPreferredSize(new Dimension(150, 25));
        m_commentLine.setMaximumSize(new Dimension(150, 25));

        final int leftInset = 50;

        Box textBox = Box.createHorizontalBox();
        textBox.add(new JLabel(
                "Create a comment header with the following content:"));
        textBox.add(Box.createHorizontalGlue()); // make it left aligned.

        Box userBox = Box.createHorizontalBox();
        userBox.add(Box.createHorizontalStrut(leftInset));
        userBox.add(m_addUser);
        userBox.add(Box.createHorizontalGlue());

        Box dateBox = Box.createHorizontalBox();
        dateBox.add(Box.createHorizontalStrut(leftInset));
        dateBox.add(m_addDate);
        dateBox.add(Box.createHorizontalGlue());

        Box nameBox = Box.createHorizontalBox();
        nameBox.add(Box.createHorizontalStrut(leftInset));
        nameBox.add(m_addTableName);
        nameBox.add(Box.createHorizontalGlue());

        Box customBox = Box.createHorizontalBox();
        customBox.add(Box.createHorizontalStrut(leftInset));
        customBox.add(m_addCustom);
        customBox.add(Box.createHorizontalStrut(3));
        customBox.add(m_commentLine);
        customBox.add(Box.createHorizontalGlue());

        JPanel whatPanel = new JPanel();
        whatPanel.setLayout(new BoxLayout(whatPanel, BoxLayout.Y_AXIS));
        whatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Comment Header Content"));
        whatPanel.add(textBox);
        whatPanel.add(dateBox);
        whatPanel.add(userBox);
        whatPanel.add(nameBox);
        whatPanel.add(customBox);

        return whatPanel;
    }

    /**
     * Updates the values in the components from the passed settings object.
     * 
     * @param settings the object holding the values to load.
     */
    void loadValuesIntoPanel(final FileWriterNodeSettings settings) {

        m_commentBegin.setText(settings.getCommentBegin());
        m_commentEnd.setText(settings.getCommentEnd());

        // support \t and \n in the custom comment
        String commLine = settings.getCustomCommentLine();
        commLine = FileWriterSettings.escapeString(commLine);
        m_commentLine.setText(commLine);
        m_addCustom
                .setSelected(((commLine != null) && (commLine.length() > 0)));

        m_addUser.setSelected(settings.addCreationUser());
        m_addTableName.setSelected(settings.addTableName());
        m_addDate.setSelected(settings.addCreationTime());

        selectionChanged();

    }

    /**
     * Disables or Enables the comment pattern text boxes depending on the
     * selected comments to add.
     */
    private void selectionChanged() {
        
        boolean addComment = false;
        
        addComment |= m_addCustom.isSelected();
        addComment |= m_addDate.isSelected();
        addComment |= m_addUser.isSelected();
        addComment |= m_addTableName.isSelected();
        
        m_commentLine.setEnabled(m_addCustom.isSelected());

        m_commentBegin.setEnabled(addComment);
        m_commentEnd.setEnabled(addComment);
    }

    /**
     * Saves the current values from the panel into the passed object.
     * 
     * @param settings the object to write the values into
     */
    void saveValuesFromPanelInto(final FileWriterNodeSettings settings) {

        settings.setCommentBegin(m_commentBegin.getText());
        settings.setCommentEnd(m_commentEnd.getText());

        if (m_addCustom.isSelected()) {
            // support \t and \n in the custom comment line
            String commLine = m_commentLine.getText();
            commLine = FileWriterSettings.unescapeString(commLine);
            settings.setCustomCommentLine(commLine);
        } else {
            settings.setCustomCommentLine("");
        }

        settings.setAddCreationUser(m_addUser.isSelected());
        settings.setAddTableName(m_addTableName.isSelected());
        settings.setAddCreationTime(m_addDate.isSelected());
    }

}
