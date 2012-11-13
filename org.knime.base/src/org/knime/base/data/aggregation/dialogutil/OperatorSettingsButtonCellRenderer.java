package org.knime.base.data.aggregation.dialogutil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;

/**
 * This class implements the aggregation operator settings button that is
 * displayed in the aggregation operator table. It opens a settings dialog
 * that allows the user to adjust operator specific settings.
 *
 * The code was partially taken from
 * <a href="http://tips4java.wordpress.com/2009/07/12/table-button-column/">
 * Rob Camick</a>.
 * @since 2.7
 */
public class OperatorSettingsButtonCellRenderer extends AbstractCellEditor
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
    public OperatorSettingsButtonCellRenderer(
      final AbstractAggregationPanel<?, ?, ?> rootPanel) {
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
    public Component getTableCellEditorComponent(final JTable table,
         final Object value, final boolean isSelected, final int row,
         final int column) {
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
    public Component getTableCellRendererComponent(final JTable table,
       final Object value, final boolean isSelected, final boolean hasFocus,
       final int row, final int column) {
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
        final int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();

        final AggregationMethod aggr = m_rootPanel.getTableModel().getRow(row);
        if (aggr.hasOptionalSettings()) {
            final JPanel settingsPanel = new JPanel();
            settingsPanel.add(
                      aggr.getSettingsPanel(m_rootPanel.getInputTableSpec()));
            String settingsTitle;
            if (aggr instanceof ColumnAggregator) {
                final ColumnAggregator op = (ColumnAggregator)aggr;
                settingsTitle = op.getOriginalColSpec().getName() + ": " + aggr.getLabel();
            } else if (aggr instanceof NamedAggregationOperator) {
                final NamedAggregationOperator op = (NamedAggregationOperator)aggr;
                settingsTitle = op.getName() + ": " + aggr.getLabel();
            } else {
                settingsTitle = aggr.getLabel() + " parameter";
            }
            final Border settingsBorder =
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                 " " + settingsTitle + " ");
            settingsPanel.setBorder(settingsBorder);
            //save the initial settings to restore them on cancel
            final NodeSettings initialSettings = new NodeSettings("tmp");
            aggr.saveSettingsTo(initialSettings);

            // figure out the parent to be able to make the dialog modal
            Frame f = null;
            Container c = m_rootPanel.getComponentPanel().getParent();
            while (c != null) {
                if (c instanceof Frame) {
                    f = (Frame)c;
                    break;
                }
                c = c.getParent();
            }

            final JDialog dialog = new JDialog(f, true);
            dialog.setTitle(" Parameter ");
            if (KNIMEConstants.KNIME16X16 != null) {
                dialog.setIconImage(KNIMEConstants.KNIME16X16.getImage());
            }
            //center the dialog
            dialog.setLocationRelativeTo(c);

            final JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new GridBagLayout());
            final GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.gridwidth = 2;
            gc.insets = new Insets(10, 10, 10, 10);
            rootPanel.add(settingsPanel, gc);

            //buttons
            gc.anchor = GridBagConstraints.LINE_END;
            gc.weightx = 1;
            gc.ipadx = 20;
            gc.gridwidth = 1;
            gc.gridx = 0;
            gc.gridy = 1;
            gc.insets = new Insets(0, 10, 10, 0);
            final JButton okButton = new JButton("OK");
            final ActionListener okActionListener = new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (validateSettings(dialog, aggr)) {
                        closeDialog(dialog);
                    }
                }
            };
            okButton.addActionListener(okActionListener);
            rootPanel.add(okButton, gc);

            gc.anchor = GridBagConstraints.LINE_START;
            gc.weightx = 0;
            gc.ipadx = 10;
            gc.gridx = 1;
            gc.insets = new Insets(0, 5, 10, 10);
            final JButton cancelButton = new JButton("Cancel");
            final ActionListener cancelActionListener = new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onCancel(dialog, aggr, initialSettings);
                }
            };
            cancelButton.addActionListener(cancelActionListener);
            rootPanel.add(cancelButton, gc);
            dialog.setContentPane(rootPanel);

            dialog.setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent we) {
                    //handle all window closing events triggered by none of
                    //the given buttons
                    onCancel(dialog, aggr, initialSettings);
                }
            });
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    /**
     * @param dialog the {@link JDialog} to close
     * @param aggr the corresponding {@link AggregationMethod}
     * @param initialSettings the initial settings
     */
    void onCancel(final JDialog dialog, final AggregationMethod aggr,
                  final NodeSettingsRO initialSettings) {
        //reset the settings
        try {
            aggr.loadValidatedSettings(initialSettings);
        } catch (InvalidSettingsException e) {
            //this should not happen
        }
        closeDialog(dialog);
    }

    private boolean validateSettings(final JDialog dialog,
                                     final AggregationMethod aggr) {
        NodeSettings tmpSettings = new NodeSettings("tmp");
        aggr.saveSettingsTo(tmpSettings);
        try {
            aggr.validateSettings(tmpSettings);
            return true;
        } catch (InvalidSettingsException e) {
            //show the error message
            JOptionPane.showMessageDialog(dialog, e.getMessage(),
                              "Invalid Settings", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * @param dialog the dialog to close
     */
    private void closeDialog(final JDialog dialog) {
        dialog.setVisible(false);
        dialog.dispose();
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
