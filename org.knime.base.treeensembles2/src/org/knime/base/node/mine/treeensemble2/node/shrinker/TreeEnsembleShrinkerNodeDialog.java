package org.knime.base.node.mine.treeensemble2.node.shrinker;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * @author Patrick Winter, University of Konstanz
 */
class TreeEnsembleShrinkerNodeDialog extends NodeDialogPane {

    ButtonGroup m_resultSizeType = new ButtonGroup();

    JSpinner m_resultSizeRelative = new JSpinner();

    JSpinner m_resultSizeAbsolute = new JSpinner();

    @SuppressWarnings("unchecked")
    ColumnSelectionComboxBox m_targetColumn = new ColumnSelectionComboxBox((Border)null, StringValue.class);

    public TreeEnsembleShrinkerNodeDialog() {
        m_resultSizeAbsolute.setEnabled(false);
        final JRadioButton resultSizeTypeRelative = new JRadioButton("Relative (%)");
        final JRadioButton resultSizeTypeAbsolute = new JRadioButton("Absolute");
        final JRadioButton resultSizeTypeAutomatic = new JRadioButton("Automatic");
        resultSizeTypeRelative.setActionCommand(TreeEnsembleShrinkerNodeConfig.SIZE_TYPE_RELATIVE);
        resultSizeTypeAbsolute.setActionCommand(TreeEnsembleShrinkerNodeConfig.SIZE_TYPE_ABSOLUTE);
        resultSizeTypeAutomatic.setActionCommand(TreeEnsembleShrinkerNodeConfig.SIZE_TYPE_AUTOMATIC);
        m_resultSizeType.add(resultSizeTypeRelative);
        m_resultSizeType.add(resultSizeTypeAbsolute);
        m_resultSizeType.add(resultSizeTypeAutomatic);
        final JLabel resultSizeLabel = new JLabel("Size of result ensemble:");
        final JLabel targetColumnLabel = new JLabel("Target column:");
        ChangeListener resultSizeTypeListener = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_resultSizeRelative.setEnabled(resultSizeTypeRelative.isSelected());
                m_resultSizeAbsolute.setEnabled(resultSizeTypeAbsolute.isSelected());
            }
        };
        resultSizeTypeRelative.addChangeListener(resultSizeTypeListener);
        resultSizeTypeAbsolute.addChangeListener(resultSizeTypeListener);
        resultSizeTypeAutomatic.addChangeListener(resultSizeTypeListener);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;
        panel.add(resultSizeLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(resultSizeTypeRelative, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_resultSizeRelative, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(resultSizeTypeAbsolute, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_resultSizeAbsolute, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(resultSizeTypeAutomatic, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        panel.add(targetColumnLabel, gbc);
        gbc.gridy++;
        gbc.weightx = 1;
        panel.add(m_targetColumn, gbc);
        addTab("Config", panel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        TreeEnsembleShrinkerNodeConfig config = new TreeEnsembleShrinkerNodeConfig();
        config.setResultSizeType(m_resultSizeType.getSelection().getActionCommand());
        config.setResultSizeRelative((int)m_resultSizeRelative.getValue());
        config.setResultSizeAbsolute((int)m_resultSizeAbsolute.getValue());
        config.setTargetColumn(m_targetColumn.getSelectedColumn());
        config.save(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        TreeEnsembleShrinkerNodeConfig config = new TreeEnsembleShrinkerNodeConfig();
        config.loadInDialog(settings);
        String resultSizeType = config.getResultSizeType();
        Enumeration<AbstractButton> buttons = m_resultSizeType.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(resultSizeType)) {
                button.setSelected(true);
                break;
            }
        }
        m_resultSizeRelative.setValue(config.getResultSizeRelative());
        m_resultSizeAbsolute.setValue(config.getResultSizeAbsolute());
        m_targetColumn.update((DataTableSpec)specs[1], config.getTargetColumn());
    }

}
