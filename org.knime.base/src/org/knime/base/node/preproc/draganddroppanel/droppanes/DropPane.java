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
 *   10.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel.droppanes;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JList.DropLocation;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.base.node.preproc.draganddroppanel.SelectionConfiguration;
import org.knime.base.node.preproc.draganddroppanel.transferhandler.ListTransferHandler;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 */
public class DropPane extends Pane {
    /**
     * Boolean indicating if the dialog gets loaded.
     */
    private boolean m_loading = false;

    /**
     * List which holds all columns of this configuration panel.
     */
    private JList<String> m_columnList;

    /**
     * Model of the included columns.
     */
    private DefaultListModel<String> m_columnListModel;

    /**
     * @param parent
     * @param config
     * @param position
     */
    public DropPane(final JPanel parent, final SelectionConfiguration config, final int position) {
        super(parent, config, position);
        m_loading = true;
        m_removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                for (int i = 0; i < m_columnListModel.getSize(); i++) {
                    getConfig().addElement(m_columnListModel.getElementAt(i));
                }
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
        getBody().add(m_columnList, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        c.gridheight = 1;
        getBody().add(m_dialog.getComponentPanel(), c);
        getParent().revalidate();
        m_loading = false;
    }


    /**
     *
     */
    protected void createColumnList() {
        m_columnListModel = new DefaultListModel<String>();
        m_columnList = new JList<String>(m_columnListModel);
        m_columnList.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mousePressed(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int i = m_columnList.getSelectedIndex();
                    getConfig().addElement(m_columnListModel.get(i));
                    m_columnListModel.remove(i);
                }
            }
        });
        m_columnList.setCellRenderer(new ListCellRenderer<String>() {

            @Override
            public Component getListCellRendererComponent(final JList<? extends String> list, final String value,
                final int index, final boolean isSelected, final boolean cellHasFocus) {
                if (value.endsWith(" ")) {
                    JLabel l = new JLabel(value.trim());
                    l.setBorder(BorderFactory.createLineBorder(Color.RED));
                    return l;
                } else {
                    return new JLabel(value);
                }
            }
        });
        m_columnList.setAutoscrolls(true);

        m_columnList.addPropertyChangeListener("dropLocation", new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                DropLocation dropLocation = (DropLocation)(evt.getNewValue());
                if (dropLocation != null) {
                    int mouseY = getParent().getMousePosition().y;
                    int mouseX = getParent().getMousePosition().x;
                    getParent().dispatchEvent(
                        new MouseEvent(getParent(), MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, mouseX,
                            mouseY, mouseX, mouseY, 1, false, MouseEvent.BUTTON1));
                }
            }
        });

        m_columnList.setDragEnabled(true);
        m_columnList.setTransferHandler(new ListTransferHandler());
        m_columnList.getModel().addListDataListener(new ListDataListener() {

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                if (m_columnList.getModel().getSize() == 0) {
                    getParent().remove(getComponentPanel());
                    getParent().repaint();
                    setParent(null);
                    getConfig().removePanel(getPosition());
                } else if (!m_loading) {
                    getConfig().getData().get(getPosition()).getSelection().clear();
                    for (int i = 0; i < m_columnListModel.getSize(); i++) {
                        getConfig().getData().get(getPosition()).getSelection()
                            .add(m_columnListModel.getElementAt(i));
                    }
                }
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                if (!m_loading) {
                    getConfig().getData().get(getPosition()).getSelection().clear();
                    for (int i = 0; i < m_columnListModel.getSize(); i++) {
                        getConfig().getData().get(getPosition()).getSelection()
                            .add(m_columnListModel.getElementAt(i));
                    }
                }
            }

            @Override
            public void contentsChanged(final ListDataEvent e) {
                // nothing
            }
        });
        m_columnList.setPreferredSize(new Dimension(100, 100));
        List<String> columns = getConfig().getData().get(getPosition()).getSelection();
        for (int i = 0; i < columns.size(); i++) {
            m_columnListModel.add(i, columns.get(i));
        }

        m_columnList.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getConfig().getData().get(getPosition()).getSelection().clear();
        for (int i = 0; i < m_columnListModel.getSize(); i++) {
            getConfig().getData().get(getPosition()).getSelection().add(m_columnListModel.getElementAt(i));
        }
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
        m_loading = true;
        NodeSettingsRO ns = settings.getNodeSettings(CFGKEY_DROPPANE + getPosition());
        m_dialog.loadSettingsFrom(ns, specs);
        m_loading = false;
    }
}
