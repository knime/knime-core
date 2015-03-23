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
package org.knime.base.node.preproc.draganddroppanel.droppanes;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.preproc.draganddroppanel.SelectionConfiguration;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author tibuch
 */
public class TypePane extends Pane {

    /**
     *
     */
    protected String m_type;

    /**
     * @param includePanel
     * @param config
     * @param i
     */
    public TypePane(final JPanel includePanel, final SelectionConfiguration config, final int i) {
        super(includePanel, config, i);

        m_removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                getConfig().addElement(m_type);

                getParent().remove(getComponentPanel());
                getParent().repaint();
                setParent(null);
                getConfig().removePanel(getPosition());
            }
        });

        createColumnList();

        GridBagConstraints c =
            new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(4, 4, 4, 0), 0, 0);
        c.gridwidth = 1;
        c.gridheight = 1;
        getComponentPanel().add(new JLabel(m_type), c);

        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        getComponentPanel().add(m_removeButton, c);
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        c.gridheight = 1;
        getComponentPanel().add(m_dialog.getComponentPanel(), c);
        getParent().revalidate();
    }

    /**
     *
     */
    protected void createColumnList() {
        m_type = getConfig().getData().get(getPosition()).getSelection().get(0);
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getConfig().getData().get(getPosition()).getSelection().clear();
        getConfig().getData().get(getPosition()).getSelection().add(m_type);

        NodeSettings ns = new NodeSettings(CFGKEY_DROPPANE + getPosition());
        m_dialog.saveSettingsTo(ns);
        settings.addNodeSettings(ns);
    }

    /**
     * @param settings
     * @param specs
     * @throws InvalidSettingsException
     * @throws NotConfigurableException
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws InvalidSettingsException, NotConfigurableException {

        NodeSettingsRO ns = settings.getNodeSettings(CFGKEY_DROPPANE + getPosition());
        m_dialog.loadSettingsFrom(ns, specs);
    }

}
