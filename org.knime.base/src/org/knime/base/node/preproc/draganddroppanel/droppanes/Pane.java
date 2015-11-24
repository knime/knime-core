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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.knime.base.node.preproc.draganddroppanel.PaneConfigurationDialog;
import org.knime.base.node.preproc.draganddroppanel.SelectionConfiguration;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author tibuch
 */
public abstract class Pane {
    /**
     * The configuration key of this dialog component.
     */
    protected static final String CFGKEY_DROPPANE = "dialogPane_";

    /**
     * The panel which holds all components of this dialog pane.
     */
    private JPanel m_panel;

    /**
     * The configuration dialog of this pane.
     */
    protected PaneConfigurationDialog m_dialog;

    /**
     * Parent panel which is the drop target and holds all config panels.
     */
    private JPanel m_parent;

    /**
     * Position in the drop list.
     */
    private int m_position;

    /**
     * The configuration which holds all other config pane models.
     */
    private SelectionConfiguration m_config;

    /**
     * Button to remove the whole panel. No action listener implemented.
     */
    protected JButton m_removeButton;

    private JSeparator m_sep;

    private JPanel m_header;

    private JPanel m_body;

    private JPanel m_footer;

    /**
     * @param parent of this panel
     * @param config which holds all other dialog panels.
     * @param position in the drag and drop list
     */
    public Pane(final JPanel parent, final SelectionConfiguration config, final int position) {
        m_parent = parent;
        m_position = position;
        m_config = config;
        m_panel = new JPanel(new GridBagLayout());

        m_header = new JPanel(new GridBagLayout());
        m_body = new JPanel(new GridBagLayout());
        m_footer = new JPanel(new GridBagLayout());
        m_dialog = m_config.getData().get(m_position).getDialog();

        m_removeButton = new JButton(new ImageIcon(getClass().getResource("../remove_icon.png")));
        m_removeButton.setMargin(new Insets(0, 0, 0, 0));
        m_removeButton.setBorderPainted(false);
        m_removeButton.setOpaque(true);
        m_removeButton.setContentAreaFilled(false);
        m_removeButton.setToolTipText("Remove this panel");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 0;
        c.gridheight = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(2, 0, 0, 0);
        m_header.add(m_removeButton, c);

        m_sep = new JSeparator(SwingConstants.HORIZONTAL);
        m_sep.setVisible(false);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 2, 0);
        c.weightx = 1;
        c.gridy++;
        c.weighty = 1;
        m_footer.setBackground(Color.black);
        m_footer.add(m_sep, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        m_panel.add(m_header, c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy++;
        m_panel.add(m_body, c);
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridheight = 1;

        m_panel.add(m_footer, c);
        m_panel.setBorder(BorderFactory.createLoweredBevelBorder());
    }


    /**
     * @param visibility of the separator
     *
     */
    public void setSeparatorVisibility(final boolean visibility) {
        m_sep.setVisible(visibility);
    }

    public JPanel getBody() {
        return m_body;
    }

    /**
     * @return JPanel which holds all dialog elements.
     */
    public JPanel getComponentPanel() {
        return m_panel;
    }

    /**
     * @param settings to save the dialog configuration
     * @throws InvalidSettingsException thrown if settings are invalid.
     */
    public abstract void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * @param settings where the dialog configuration is saved
     * @param specs the data table specs
     * @throws InvalidSettingsException thrown if the settings are invalid
     * @throws NotConfigurableException throw if the dialog cannot be configured
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws InvalidSettingsException, NotConfigurableException;

    /**
     * @return the parent
     */
    public JPanel getParent() {
        return m_parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(final JPanel parent) {
        m_parent = parent;
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return m_position;
    }

    /**
     * @return the config
     */
    public SelectionConfiguration getConfig() {
        return m_config;
    }


    /**
     * @param sep
     */
    public void setSeparator(final JSeparator sep) {
        // TODO Auto-generated method stub
        m_sep = sep;
    }

}
