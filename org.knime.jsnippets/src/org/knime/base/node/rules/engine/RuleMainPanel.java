/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 2013.04.25. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconRowHeader;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.rules.engine.manipulator.ConstantManipulator;
import org.knime.base.node.rules.engine.manipulator.InfixManipulator;
import org.knime.base.node.rules.engine.manipulator.PrefixUnaryManipulator;
import org.knime.base.node.rules.engine.manipulator.RuleManipulatorProvider;
import org.knime.base.node.util.JSnippetPanel;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.rsyntaxtextarea.KnimeSyntaxTextArea;

/**
 * The main panel (manipulators, columns, flow variables and the editor) of the rule engine node dialogs.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
@SuppressWarnings("serial")
public class RuleMainPanel extends JSnippetPanel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RuleMainPanel.class);

    /** Error icon to signal problems in rows */
    static final ImageIcon ERROR_ICON;
    static {
        ImageIcon icon;
        try {
            icon = new ImageIcon(RuleMainPanel.class.getResource("error_obj.png"));
        } catch (RuntimeException e) {
            icon = KNIMEConstants.KNIME16X16;
        }
        ERROR_ICON = icon;
    }

    private KnimeSyntaxTextArea m_textEditor;

    private Gutter m_gutter;

    private final String m_syntaxStyle;

    private static class ToggleRuleAction extends AbstractAction {
        private static final long serialVersionUID = 5930758516767278299L;

        private static class LinePosition extends ActionEvent {
            private static final long serialVersionUID = -7627500390929718724L;

            private final int m_lineNumber;

            /**
             * @param source
             * @param id
             * @param command
             * @param modifiers
             * @param lineNumber Line number starting from {@code 0}.
             * @see ActionEvent#ActionEvent(Object, int, String, int)
             */
            @SuppressWarnings("hiding")
            public LinePosition(final Object source, final int id, final String command, final int modifiers,
                final int lineNumber) {
                super(source, id, command, modifiers);
                this.m_lineNumber = lineNumber;
            }

            /**
             * @return the lineNumber
             */
            public int getLineNumber() {
                return m_lineNumber;
            }
        }

        private final RTextArea m_textArea;

        /**
         * @param textArea The {@link RTextArea} where the toggle action is applied.
         */
        public ToggleRuleAction(final RTextArea textArea) {
            this("", textArea);
        }

        public ToggleRuleAction(final String name, final RTextArea textArea) {
            this(name, null, textArea);
        }

        public ToggleRuleAction(final String name, final Icon icon, final RTextArea textArea) {
            super(name, icon);
            m_textArea = textArea;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            Object source = e.getSource();
            RTextArea textArea;
            if (source instanceof RTextArea) {
                textArea = (RTextArea)source;
            } else {
                textArea = m_textArea;
            }
            try {
                if (e instanceof LinePosition) {
                    LinePosition linePosition = (LinePosition)e;
                    int line = linePosition.getLineNumber();
                    toggle(textArea, line);
                } else {
                    int line = textArea.getLineOfOffset(textArea.getLineStartOffsetOfCurrentLine());
                    toggle(textArea, line);
                }
            } catch (BadLocationException ex) {
                LOGGER.debug(ex.getMessage(), ex);
            }
        }

        /**
         * Comments/uncomments the rule in line {@code line}. Invalid line numbers have no effect besides a debug log.
         *
         * @param textArea The {@link RTextArea}.
         * @param line A valid line in {@code textArea}.
         */
        private void toggle(final RTextArea textArea, final int line) {
            int lineStart;
            try {
                lineStart = textArea.getLineStartOffset(line);
                String ruleText = textArea.getText().substring(lineStart, textArea.getLineEndOffset(line));
                if (RuleSupport.isComment(ruleText)) {
                    int l = 0;
                    while (ruleText.charAt(l) == '/') {
                        ++l;
                    }
                    textArea.replaceRange("", lineStart, lineStart + l);
                } else {
                    textArea.insert("//", lineStart);
                }
            } catch (BadLocationException e1) {
                LOGGER.debug(e1.getMessage(), e1);
            }
        }
    }

    /**
     * @param nodeType
     */
    public RuleMainPanel(final RuleNodeSettings nodeType) {
        super(RuleManipulatorProvider.getProvider(nodeType), nodeType.completionProvider(), nodeType.allowColumns(), nodeType.allowFlowVariables());
        m_syntaxStyle = nodeType.syntaxKey();
        m_textEditor.setSyntaxEditingStyle(m_syntaxStyle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent createEditorComponent() {
        m_textEditor = new KnimeSyntaxTextArea(20, 60);
        final RSyntaxTextArea textArea = m_textEditor;
        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        AutoCompletion ac = new AutoCompletion(getCompletionProvider());
        ac.setShowDescWindow(true);

        ac.install(textArea);
        setExpEdit(textArea);
        textArea.setSyntaxEditingStyle(m_syntaxStyle);

        textArea.getPopupMenu().add(new ToggleRuleAction("Toggle comment", textArea));
        RTextScrollPane textScrollPane = new RTextScrollPane(textArea);
        textScrollPane.setLineNumbersEnabled(true);
        textScrollPane.setIconRowHeaderEnabled(true);
        m_gutter = textScrollPane.getGutter();
        addRowHeaderMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    try {
                        new ToggleRuleAction(textArea).actionPerformed(new ToggleRuleAction.LinePosition(textArea,
                            (int)(new Date().getTime() & 0x7fffffff), "toggle comment", e.getModifiers(), textArea
                                .getLineOfOffset(textArea.viewToModel(e.getPoint()))));
                    } catch (BadLocationException e1) {
                        LOGGER.debug(e1.getMessage(), e1);
                    }
                }
            }
        });
        return textScrollPane;
    }

    /**
     * Adds a {@link MouseListener} to the row header.
     *
     * @param listener A {@link MouseListener} to handle clicks on the row header.
     */
    public void addRowHeaderMouseListener(final MouseListener listener) {
        final IconRowHeader rowHeader = Util.findComponent(m_gutter.getComponents(), IconRowHeader.class);
        if (rowHeader != null) {
            rowHeader.addMouseListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionInManipulatorList(final Object selected) {
        if (selected instanceof InfixManipulator) {
            InfixManipulator infix = (InfixManipulator)selected;
            String textToInsert = infix.getName() + " ";
            try {
                if (m_textEditor.getCaretPosition() == 0 || m_textEditor.getText().isEmpty()
                    || m_textEditor.getText(m_textEditor.getCaretPosition(), 1).charAt(0) != ' ') {
                    textToInsert = " " + textToInsert;
                }
            } catch (BadLocationException e) {
                LOGGER.coding("Not fatal error, but should not happen, requires no action.", e);
            }
            m_textEditor.insert(textToInsert, m_textEditor.getCaretPosition());
            m_textEditor.requestFocus();
        } else if (selected instanceof PrefixUnaryManipulator || selected instanceof ConstantManipulator) {
            Manipulator prefix = (Manipulator)selected;
            m_textEditor.replaceSelection(prefix.getName() + " ");

            m_textEditor.requestFocus();
        } else {
            super.onSelectionInManipulatorList(selected);
        }
    }

    /**
     * @return the textEditor
     */
    public KnimeSyntaxTextArea getTextEditor() {
        return m_textEditor;
    }

    /**
     * @return the gutter
     */
    public Gutter getGutter() {
        return m_gutter;
    }
}
