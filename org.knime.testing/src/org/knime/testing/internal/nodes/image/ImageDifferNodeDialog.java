/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * History
 *   16.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.image;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.knime.testing.internal.diffcheckers.CheckerUtil;

/**
 * Dialog for the image difference checker.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class ImageDifferNodeDialog extends NodeDialogPane {
    private final DefaultListCellRenderer m_diffCheckerListRenderer = new DefaultListCellRenderer() {
        private static final long serialVersionUID = -3190079420164691890L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            if (value instanceof DifferenceCheckerFactory) {
                return super.getListCellRendererComponent(list, ((DifferenceCheckerFactory<? extends DataValue>)value)
                        .getDescription(), index, isSelected, cellHasFocus);
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    };

    private final JComboBox m_checkerBox = new JComboBox();

    private final JPanel m_configPanel = new JPanel(new GridBagLayout());

    private final ImageDifferNodeSettings m_settings = new ImageDifferNodeSettings();

    private DifferenceChecker<? extends DataValue> m_currentChecker;

    private PortObjectSpec[] m_portSpecs;

    ImageDifferNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 5);

        p.add(new JLabel("Image checker   "), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_checkerBox, c);
        m_checkerBox.setRenderer(m_diffCheckerListRenderer);
        m_checkerBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    checkerChanged(((DifferenceCheckerFactory<? extends DataValue>)e.getItem()));
                }
            }
        });

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridwidth = 2;
        p.add(new JScrollPane(m_configPanel), c);

        addTab("Standard settings", p);
    }

    private void checkerChanged(final DifferenceCheckerFactory<? extends DataValue> factory) {
        m_configPanel.setVisible(false);
        m_configPanel.removeAll();

        m_currentChecker = factory.newChecker();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;

        for (DialogComponent comp : m_currentChecker.getDialogComponents()) {
            try {
                comp.loadSettingsFrom(m_settings.checkerConfiguration(), m_portSpecs);
            } catch (NotConfigurableException ex) {
                // ignore it and use defaults
            }
            m_configPanel.add(comp.getComponentPanel(), c);
            c.gridy++;
        }
        m_configPanel.setVisible(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        if (specs[1] == null) {
            throw new NotConfigurableException("No information/spec about reference image available");
        }

        m_portSpecs = specs;
        m_settings.loadSettingsForDialog(settings);

        m_checkerBox.removeAllItems();
        DataType type = ((ImagePortObjectSpec)specs[1]).getDataType();

        for (DifferenceCheckerFactory<? extends DataValue> fac : CheckerUtil.instance.getFactoryForType(type)) {
            m_checkerBox.addItem(fac);
        }

        m_checkerBox.setSelectedItem(m_settings.checkerFactory());
        checkerChanged(m_settings.checkerFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.checkerFactory((DifferenceCheckerFactory<? extends DataValue>)m_checkerBox.getSelectedItem());

        if (m_currentChecker != null) {
            NodeSettings config = m_settings.newCheckerConfiguration();
            m_currentChecker.saveSettings(config);
        }

        m_settings.saveSettings(settings);
    }

}
