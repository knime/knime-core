/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * ---------------------------------------------------------------------
 *
 * Created on 18.12.2012 by koetter
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;

/**
 * {@link JDialog} that displays and allows the editing of the additional parameters of an
 * {@link AggregationMethod}.
 * @author Tobias Koetter, University of Konstanz
 * @since 2.8
 */
public class AggregationParameterDialog extends JDialog {

    /**This is the first version of the dialog.*/
    private static final long serialVersionUID = 1L;
    private final AggregationMethod m_method;


    /**
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param method the aggregation method the parameters should be edited
     * @param spec the table spec of the table the method will be applied to
     * @throws NotConfigurableException if the input spec does not satisfies all requirements
     */
    public AggregationParameterDialog(final Frame owner, final AggregationMethod method,
                                      final DataTableSpec spec) throws NotConfigurableException {
        this(owner, true, " Parameter ", method, spec);
    }

    /**
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other top-level
     *     windows when shown. If {@code true}, the modality type property is set to
     *     {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog is modeless.
     * @param title  the {@code String} to display in the dialog's
     *     title bar
     * @param method the aggregation method the parameters should be edited
     * @param spec the table spec of the table the method will be applied to
     * @throws NotConfigurableException if the input spec does not satisfies all requirements
     */
    public AggregationParameterDialog(final Frame owner, final boolean modal, final String title,
                                      final AggregationMethod method,
                                      final DataTableSpec spec) throws NotConfigurableException {
        super(owner, title, modal);
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        if (!method.hasOptionalSettings()) {
            throw new IllegalArgumentException("Aggregation method has no optional settings");
        }
        if (spec == null) {
            throw new NullPointerException("spec must not be null");
        }
        m_method = method;
        if (KNIMEConstants.KNIME16X16 != null) {
            setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }

        //save the initial settings to restore them on cancel and to initialize the dialog
        final NodeSettings initialSettings = new NodeSettings("tmp");
        m_method.saveSettingsTo(initialSettings);
        //load the default settings including the input table spec
        //to initialize the dialog panel
        m_method.loadSettingsFrom(initialSettings, spec);

        final JPanel settingsPanel = new JPanel();
        settingsPanel.add(m_method.getSettingsPanel());
        String settingsTitle;
        if (m_method instanceof ColumnAggregator) {
            final ColumnAggregator op = (ColumnAggregator)m_method;
            settingsTitle = op.getOriginalColSpec().getName() + ": " + m_method.getLabel();
        } else if (m_method instanceof NamedAggregationOperator) {
            final NamedAggregationOperator op = (NamedAggregationOperator)m_method;
            settingsTitle = op.getName() + ": " + m_method.getLabel();
        } else {
            settingsTitle = m_method.getLabel() + " parameter";
        }
        final Border settingsBorder =
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                             " " + settingsTitle + " ");
        settingsPanel.setBorder(settingsBorder);


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
                if (validateSettings()) {
                    closeDialog();
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
                onCancel(initialSettings);
            }
        };
        cancelButton.addActionListener(cancelActionListener);
        rootPanel.add(cancelButton, gc);
        setContentPane(rootPanel);

        setDefaultCloseOperation(
            WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent we) {
                //handle all window closing events triggered by none of
                //the given buttons
                onCancel(initialSettings);
            }
        });
        pack();
    }

    private boolean validateSettings() {
        final NodeSettings tmpSettings = new NodeSettings("tmp");
        m_method.saveSettingsTo(tmpSettings);
        try {
            m_method.validateSettings(tmpSettings);
            return true;
        } catch (InvalidSettingsException e) {
            //show the error message
            JOptionPane.showMessageDialog(this, e.getMessage(),
                              "Invalid Settings", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * @param initialSettings the initial settings
     */
    private void onCancel(final NodeSettingsRO initialSettings) {
        //reset the settings
        try {
            m_method.loadValidatedSettings(initialSettings);
        } catch (InvalidSettingsException e) {
            //this should not happen
        }
        closeDialog();
    }

    /**
     * @param dialog the dialog to close
     */
    private void closeDialog() {
        setVisible(false);
        dispose();
    }
}
