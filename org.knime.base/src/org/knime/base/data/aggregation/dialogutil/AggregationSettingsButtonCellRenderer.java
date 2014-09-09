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
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.core.node.NotConfigurableException;

/**
 * This class implements the aggregation operator settings button that is
 * displayed in the aggregation operator table. It opens a settings dialog
 * that allows the user to adjust operator specific settings.
 *
 * The code was partially taken from
 * <a href="http://tips4java.wordpress.com/2009/07/12/table-button-column/">
 * Rob Camick</a>.
 * @since 2.11
 */
public class AggregationSettingsButtonCellRenderer extends AbstractCellEditor
implements TableCellRenderer, TableCellEditor, MouseListener {

    private static final JLabel NO_SETTINGS_COMPONENT = new JLabel("");

    private static final long serialVersionUID = 1L;

    private Border m_originalBorder;

    private Border m_focusBorder;

    private JButton m_renderButton;

    private JButton m_editButton;

    private Object m_editorValue;

    private boolean m_isButtonColumnEditor;

    private final AbstractAggregationPanel<?, ?, ?> m_rootPanel;

    /**
     * Create the ButtonColumn to be used as a renderer and editor.
     * The renderer and editor will automatically be installed on the
     * TableColumn of the specified column.
     * @param rootPanel the AbstractAggregationPanel this button is added
     */
    public AggregationSettingsButtonCellRenderer(final AbstractAggregationPanel<?, ?, ?> rootPanel) {
        if (rootPanel == null) {
            throw new NullPointerException("rootPanel must not be null");
        }
        m_rootPanel = rootPanel;
        m_renderButton = new JButton();
        m_editButton = new JButton();
        m_editButton.setFocusPainted(false);
        m_originalBorder = m_editButton.getBorder();
        setFocusBorder(new LineBorder(Color.BLUE));
        m_rootPanel.getTable().addMouseListener(this);
    }


    /**
     * Get foreground color of the button when the cell has focus.
     *
     * @return the foreground color
     */
    public Border getFocusBorder() {
        return m_focusBorder;
    }

    /**
     * The foreground color of the button when the cell has focus.
     *
     * @param focusBorder the foreground color
     */
    public void setFocusBorder(final Border focusBorder) {
        m_focusBorder = focusBorder;
        m_editButton.setBorder(focusBorder);
    }

    @Override
    public Object getCellEditorValue() {
        return m_editorValue;
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        if (value == null) {
            return NO_SETTINGS_COMPONENT;
        } else {
            m_editButton.setEnabled(false);
            m_editButton.setText("Edit");
            m_editButton.setIcon(null);
        }
        m_editorValue = value;
        return m_editButton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        if (Boolean.FALSE.equals(value)) {
            return NO_SETTINGS_COMPONENT;
        }
        if (isSelected) {
            m_renderButton.setForeground(table.getSelectionForeground());
            m_renderButton.setBackground(table.getSelectionBackground());
        } else {
            m_renderButton.setForeground(table.getForeground());
            m_renderButton.setBackground(
                                 UIManager.getColor("Button.background"));
        }
        if (hasFocus) {
            m_renderButton.setBorder(m_focusBorder);
        } else {
            m_renderButton.setBorder(m_originalBorder);
        }

        m_renderButton.setText("Edit");
        m_renderButton.setIcon(null);

        return m_renderButton;
    }

    private void openSettingsDialog() {
        final JTable table = m_rootPanel.getTable();
        final int rowIdx = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();

        final AggregationFunctionRow<?> row = m_rootPanel.getTableModel().getRow(rowIdx);
        if (!row.getFunction().hasOptionalSettings()) {
            //the operator has no additional settings
            return;
        }

        // figure out the parent to be able to make the dialog modal
        Frame f = null;
        Container c = m_rootPanel.getComponentPanel().getParent();
        final Component root = SwingUtilities.getRoot(c);
        if (root instanceof Frame) {
            f = (Frame)root;
        }
        while (f == null && c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        try {
            final AggregationSettingsDialog dialog =
                    new AggregationSettingsDialog(f, row.getFunction(), m_rootPanel.getInputTableSpec());
            //center the dialog
            dialog.setLocationRelativeTo(c);
            dialog.pack();
            //show it
            dialog.setVisible(true);
        } catch (NotConfigurableException e) {
            //show the error message
              JOptionPane.showMessageDialog(m_rootPanel.getComponentPanel(), e.getMessage(),
                                "Unable to open dialog", JOptionPane.ERROR_MESSAGE);
              return;
          }
    }

    //
    //  Implement MouseListener interface
    //
    /*
     *  When the mouse is pressed the editor is invoked. If you then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        JTable table = m_rootPanel.getTable();
        if (table.isEditing() && table.getCellEditor() == this) {
            m_isButtonColumnEditor = true;
            openSettingsDialog();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
        JTable table = m_rootPanel.getTable();
        if (m_isButtonColumnEditor && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        m_isButtonColumnEditor = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(final MouseEvent e) {
    }

}
