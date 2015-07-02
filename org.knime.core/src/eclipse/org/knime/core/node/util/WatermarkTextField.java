/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   7 Febr 2015 (Gabor): created
 */
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 *
 * @author Gabor Bakos
 * @since 2.12
 */
@SuppressWarnings("serial")
public class WatermarkTextField extends JTextField {
    private boolean m_empty = true;

    private String m_watermark;

    private static final Color WATERMARK_COLOR = Color.LIGHT_GRAY;

    private final Color m_origForeground;

    /**
     *
     */
    public WatermarkTextField() {
        this(null, null, 0);
    }

    /**
     * @param text
     */
    public WatermarkTextField(final String text) {
        this(null, text, 0);
    }

    /**
     * @param columns
     */
    public WatermarkTextField(final int columns) {
        this(null, null, columns);
    }

    /**
     * @param text
     * @param columns
     */
    public WatermarkTextField(final String text, final int columns) {
        this(null, text, columns);
    }

    /**
     * @param doc
     * @param text
     * @param columns
     */
    public WatermarkTextField(final Document doc, final String text, final int columns) {
        super(doc, text, columns);
        m_origForeground = getForeground();
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
//                changed();
                if ((getText() == null || getText().isEmpty())) {
                    m_empty = false;
                    setForeground(m_origForeground);
                }
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                changed();
            }

            private void changed() {
                if (isEnabled()) {
                    if (getText() == null || getText().isEmpty()) {
                        m_empty = true;
//                        ViewUtils.invokeLaterInEDT(
                        SwingUtilities.invokeLater(
                            new Runnable() {
                            @Override
                            public void run() {
                                if (m_empty) {
                                    setForeground(WATERMARK_COLOR);
                                    WatermarkTextField.super.setText(m_watermark);
                                    setCaretPosition(0);
                                }
                            }});
                    } else if (!Objects.equals(WatermarkTextField.super.getText(), m_watermark)) {
                        setForeground(m_origForeground);
                    }
                }
            }
        });
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent e) {
            }

            @Override
            public void focusGained(final FocusEvent e) {
                if (m_empty) {
                    setSelectionStart(0);
                    setSelectionEnd(WatermarkTextField.super.getText().length());
                }
            }
        });
        addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                if (m_empty && e.getDot() != e.getMark() && getText() != null && Math.abs(e.getMark() - e.getDot()) != getText().length()) {
                    m_empty = false;
                    WatermarkTextField.super.setText(WatermarkTextField.super.getText());
                    setForeground(m_origForeground);
                }
            }
        });
    }

    /**
     * Sets the watermark for the control.
     *
     * Cannot be called from a {@link DocumentListener}, should be called on EDT.
     *
     * @param watermark The watermark to use.
     */
    public void setWatermark(final String watermark) {
        m_watermark = watermark;
        if (m_empty) {
            WatermarkTextField.super.setText(m_watermark);
            setForeground(WATERMARK_COLOR);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        if (m_empty) {
            return null;
        }
        return super.getText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setText(final String t) {
        m_empty = t == null || t.isEmpty();
        setForeground(m_empty ? WATERMARK_COLOR : m_origForeground);
        super.setText(t);
    }

    /**
     * main
     * @param args
     */
    @Deprecated
    public static void main(final String[] args) {
        JFrame frame = new JFrame("Test");
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new FlowLayout(FlowLayout.LEADING, 4, 4));
        contentPane.add(new JTextField(11));
        contentPane.add(new JLabel("Hi"));
        WatermarkTextField textField = new WatermarkTextField(22);
        textField.setWatermark("Hello world");
        contentPane.add(textField);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }
}
