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
 *   16.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author tibuch
 */
public class DNDSelectionPanel extends JPanel {

    private JPanel m_mainPanel = new JPanel(new GridBagLayout());



    private DNDSelectionConfiguration config;





    public DNDSelectionPanel(final ConfigurationDialogFactory fac) {


        setLayout(new GridBagLayout());

        config = new DNDSelectionConfiguration(fac);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        add(config.getRadioButtons().getComponentPanel(), c);


        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        add(config.getManual(), c);
        add(config.getType(), c);
        add(config.getAllColumn(), c);

        config.getRadioButtons().getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if (((SettingsModelString)config.getRadioButtons().getModel()).getStringValue().equals("Manual Selection")) {
                    config.getAllColumn().setVisible(false);
                    config.getManual().setVisible(true);
                    config.getType().setVisible(false);
                } else if (((SettingsModelString)config.getRadioButtons().getModel()).getStringValue().equals("All Columns")) {
                    config.getAllColumn().setVisible(true);
                    config.getManual().setVisible(false);
                    config.getType().setVisible(false);
                } else {
                    config.getAllColumn().setVisible(false);
                    config.getManual().setVisible(false);
                    config.getType().setVisible(true);
                }
            }
        });
    }

    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        config.loadSettingsFrom(settings, specs);
    }

    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        config.saveSettingsTo(settings);

    }
}
