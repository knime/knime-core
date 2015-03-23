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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

import org.knime.base.node.preproc.draganddroppanel.droppanes.DropPaneConfig;
import org.knime.base.node.preproc.draganddroppanel.droppanes.Pane;
import org.knime.base.node.preproc.draganddroppanel.transferhandler.ListTransferHandler;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author tibuch
 */
public abstract class SelectionPanel extends JPanel implements DropTargetListener {
    private JSplitPane m_mainPanel = null;

    private JPanel m_includePanel = null;

    private JScrollPane m_scrollPane;

    private GridBagConstraints m_gbc;

    private JList<String> m_inputList = null;

    private SelectionConfiguration m_config;

    private JScrollPane m_inputListScroller;

    /**
     * @param filter
     */
    public SelectionPanel(final SelectionConfiguration config) {

        setLayout(new GridBagLayout());

        m_config = config;

        m_gbc =
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(0, 10, 0, 0), 0, 0);

        m_mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        createInputList();

        createIncludePanel();
        m_mainPanel.setTopComponent(m_inputListScroller);
        m_mainPanel.setRightComponent(m_scrollPane);
        GridBagConstraints c =
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(4,
                    4, 4, 4), 0, 0);
        add(m_mainPanel, c);
    }

    private void createIncludePanel() {
        m_includePanel = new JPanel(new GridBagLayout());
        new DropTarget(m_includePanel, DnDConstants.ACTION_MOVE, this, true, null);

        m_scrollPane =
            new JScrollPane(m_includePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        m_scrollPane.setPreferredSize(new Dimension(300, 200));
        m_scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        MouseMotionListener doScrollRectToVisible = new MouseMotionAdapter() {
            int lasty = 0;

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (Math.abs(lasty - e.getY()) < 10) {

                    int i = m_scrollPane.getVerticalScrollBar().getValue();
                    if (e.getY() > lasty) {
                        m_scrollPane.getVerticalScrollBar().setValue(i + 2);
                    } else if (e.getY() < lasty) {
                        m_scrollPane.getVerticalScrollBar().setValue(i - 2);
                    }
                }
                if (e.getY() < 10) {
                    lasty = 10;
                } else if (e.getY() > m_includePanel.getVisibleRect().getMaxY()) {
                    lasty = (int)m_includePanel.getVisibleRect().getMaxY() - 10;
                } else if (Math.abs(lasty - e.getY()) > 10){
                    lasty = e.getY();
                }

            }
        };
        m_includePanel.addMouseMotionListener(doScrollRectToVisible);

        m_includePanel.setAutoscrolls(true);
    }

    private void createInputList() {
        m_inputList = new JList<String>(m_config.getInputListModel());
        m_inputList.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(final MouseEvent e) {
                // nothing to do
            }

            @Override
            public void mousePressed(final MouseEvent e) {
             // nothing to do
            }

            @Override
            public void mouseExited(final MouseEvent e) {
             // nothing to do
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
             // nothing to do
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int selectedIndex = m_inputList.getSelectedIndex();
                    int i = m_config.drop((String)((DefaultListModel) m_inputList.getModel()).get(selectedIndex));
                     Pane dp = getNewPane(m_includePanel, m_config, i);
                     m_includePanel.add(dp.getComponentPanel(), m_gbc);
                     m_gbc.gridy++;
                     m_includePanel.setBackground(UIManager.getColor("Panel.background"));
                     m_scrollPane.revalidate();
                ((DefaultListModel) m_inputList.getModel()).removeElementAt(selectedIndex);
                }
            }
        });
        m_inputList.setDragEnabled(true);
        m_inputList.setTransferHandler(new ListTransferHandler());
        m_inputList.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        m_inputListScroller = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        m_inputListScroller.setPreferredSize(new Dimension(150, 200));
        m_inputListScroller.setViewportView(m_inputList);
    }

    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_includePanel.removeAll();
        m_gbc.gridy = 0;
        m_config.clear();
        m_config.loadSettingsFrom(settings, specs);
        HashMap<Integer, DropPaneConfig> data = m_config.getData();
        for (int i = 0; i < data.size(); i++) {

            m_includePanel.add(getNewPane(m_includePanel, m_config, i).getComponentPanel(), m_gbc);
            m_gbc.gridy++;
        }
    }

    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetDragEvent dtde) {
        if (dtde.getDropAction() == DnDConstants.ACTION_MOVE) {
            m_includePanel.setBackground(Color.green);
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);
        } else {
            dtde.rejectDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetDragEvent dtde) {
        if (dtde.getDropAction() == DnDConstants.ACTION_MOVE) {
            m_includePanel.setBackground(Color.green);
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);
        } else {
            dtde.rejectDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropActionChanged(final DropTargetDragEvent dtde) {
        if (dtde.getDropAction() == DnDConstants.ACTION_MOVE) {
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);
        } else {
            dtde.rejectDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragExit(final DropTargetEvent dte) {
        m_includePanel.setBackground(UIManager.getColor("Panel.background"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetDropEvent dtde) {
        if (dtde.getDropAction() == DnDConstants.ACTION_MOVE) {
            dtde.acceptDrop(dtde.getDropAction());
            Transferable t = dtde.getTransferable();

            String s = "default";
            try {
                s = (String)t.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
            int i = m_config.drop(s);
            Pane dp = getNewPane(m_includePanel, m_config, i);
            m_includePanel.add(dp.getComponentPanel(), m_gbc);
            m_gbc.gridy++;

            m_includePanel.setBackground(UIManager.getColor("Panel.background"));
            m_scrollPane.revalidate();
            dtde.dropComplete(true);

        }
    }

    /**
     * @param includePanel
     * @param config
     * @param i
     * @return
     */
    protected abstract Pane getNewPane(JPanel includePanel, SelectionConfiguration config, int i);

}
