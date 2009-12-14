/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jan 30, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaTreeViewPropsPanel extends JPanel {
    private SotaDrawingPane m_pane;

    private JSlider m_slider;

    private JButton m_zoomIn;

    private JButton m_zoomOut;

    private JCheckBox m_setHierarchicalView;

    private JCheckBox m_setHierarchicalSeparators;

    /**
     * Creates a new instance of SotaTreeViewPropsPanel with given
     * SotaDrawingPane.
     * 
     * @param pane SotaDrawingPane to add to panel
     */
    public SotaTreeViewPropsPanel(final SotaDrawingPane pane) {
        super();
        m_pane = pane;
        JScrollPane scrollPane = new JScrollPane(m_pane);

        setLayout(new GridBagLayout());

        GridBagConstraints gBC;
        gBC = new GridBagConstraints();
        gBC.gridx = 0;
        gBC.gridy = 0;
        gBC.fill = GridBagConstraints.BOTH;
        gBC.weightx = 100;
        gBC.weighty = 100;
        gBC.gridheight = 5;
        add(scrollPane, gBC);

        if (m_pane.isHierarchicalFuzzyData()) {
            m_slider = new JSlider(SwingConstants.VERTICAL, 0, m_pane
                    .getMaxHLevel(), 0);
            m_slider.addChangeListener(new SotaTreeViewPanelController());
            m_slider.addMouseWheelListener(new SotaTreeViewPanelController());
            m_slider.setPaintLabels(true);
            m_slider.setInverted(true);
            m_slider.setLabelTable(m_slider.createStandardLabels(1));
            m_slider.setToolTipText("Move to accent clusters of hierarchical"
                    + " level.");
            m_slider.setSnapToTicks(true);

            gBC = new GridBagConstraints();
            gBC.gridx = 1;
            gBC.gridy = 0;
            gBC.fill = GridBagConstraints.BOTH;
            gBC.weightx = 1;
            gBC.weighty = 100;
            add(m_slider, gBC);

            m_zoomIn = new JButton(" + ");
            m_zoomIn.setToolTipText("zoom in");
            m_zoomIn.addActionListener(new SotaTreeViewPanelController());
            m_zoomIn.setEnabled(m_pane.isZooming());
            gBC = new GridBagConstraints();
            gBC.gridx = 1;
            gBC.gridy = 1;
            gBC.fill = GridBagConstraints.HORIZONTAL;
            gBC.weightx = 1;
            gBC.weighty = 1;
            add(m_zoomIn, gBC);

            m_zoomOut = new JButton(" - ");
            m_zoomOut.setToolTipText("zoom out");
            m_zoomOut.addActionListener(new SotaTreeViewPanelController());
            m_zoomOut.setEnabled(m_pane.isZooming());
            gBC = new GridBagConstraints();
            gBC.gridx = 1;
            gBC.gridy = 2;
            gBC.fill = GridBagConstraints.HORIZONTAL;
            gBC.weightx = 1;
            gBC.weighty = 1;
            add(m_zoomOut, gBC);

            m_setHierarchicalView = new JCheckBox("H-View");
            m_setHierarchicalView.setToolTipText("Click to change "
                    + "hierarchical view");
            m_setHierarchicalView.setSelected(m_pane
                    .isDrawHierarchicalFuzzyData());
            m_setHierarchicalView
                    .addActionListener(new SotaTreeViewPanelController());
            gBC = new GridBagConstraints();
            gBC.gridx = 1;
            gBC.gridy = 3;
            gBC.fill = GridBagConstraints.HORIZONTAL;
            gBC.weightx = 1;
            gBC.weighty = 1;
            add(m_setHierarchicalView, gBC);

            m_setHierarchicalSeparators = new JCheckBox("H-Lines");
            m_setHierarchicalSeparators.setToolTipText("Check to draw "
                    + "hierarchical separator lines");
            m_setHierarchicalSeparators.setSelected(m_pane
                    .isDrawHierarchicalSeparators());
            m_setHierarchicalSeparators
                    .addActionListener(new SotaTreeViewPanelController());
            gBC = new GridBagConstraints();
            gBC.gridx = 1;
            gBC.gridy = 4;
            gBC.fill = GridBagConstraints.HORIZONTAL;
            gBC.weightx = 1;
            gBC.weighty = 1;
            add(m_setHierarchicalSeparators, gBC);
        }
    }

    /**
     * Changes the sliders maximum value.
     */
    public void modelChanged() {
        if (m_slider != null) {
            m_slider.setMaximum(m_pane.getMaxHLevel());
            repaint();
        }
    }

    /**
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class SotaTreeViewPanelController implements ChangeListener,
            ActionListener, MouseWheelListener {
        /**
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            m_pane.setAccentHLevel(m_slider.getValue());
            m_pane.repaint();
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            Object source = e.getSource();

            if (source.equals(m_zoomIn)) {
                m_pane.zoomIn();
            } else if (source.equals(m_zoomOut)) {
                m_pane.zoomOut();
            } else if (source.equals(m_setHierarchicalView)) {
                m_pane.setDrawHierarchicalFuzzyData(m_setHierarchicalView
                        .isSelected());

                // if H-View is turned off, also turn off separator lines
                // and disable the checkbox.
                m_setHierarchicalSeparators.setEnabled(m_setHierarchicalView
                        .isSelected());
                if (!m_setHierarchicalView.isSelected()) {
                    m_pane.setDrawHierarchicalSeparators(false);
                } else {
                    if (m_setHierarchicalSeparators.isSelected()) {
                        m_pane.setDrawHierarchicalSeparators(true);
                    }
                }

                m_pane.modelChanged(false);
            } else if (source.equals(m_setHierarchicalSeparators)) {
                m_pane.setDrawHierarchicalSeparators(
                                m_setHierarchicalSeparators.isSelected());
                m_pane.repaint();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseWheelMoved(final MouseWheelEvent e) {
            int level = m_slider.getValue();
            int notches = e.getWheelRotation();
            if (notches < 0) {
                level--;
            } else {
                level++;
            }

            if (level >= m_slider.getMinimum()
                    && level <= m_slider.getMaximum()) {
                m_slider.setValue(level);
                m_pane.setAccentHLevel(level);
                m_pane.repaint();
            }
        }
    }

    /** Popup menue entry constant. */
    public static final String ENABLE_ZOOM = "Fit to size";

    /**
     * @return a JMenu entry handling the zooming ability
     */
    public JMenu createZoomMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('Z');
        ActionListener actL = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(ENABLE_ZOOM)) {

                    JMenuItem item = (JMenuItem)e.getSource();

                    if (!item.isSelected()) {
                        m_pane.setZooming(true);

                        if (m_zoomOut != null && m_zoomIn != null) {
                            m_zoomIn.setEnabled(true);
                            m_zoomOut.setEnabled(true);
                        }
                    } else {
                        m_pane.setZooming(false);

                        if (m_zoomOut != null && m_zoomIn != null) {
                            m_zoomIn.setEnabled(false);
                            m_zoomOut.setEnabled(false);
                        }
                    }

                }
            }
        };
        JMenuItem item = new JRadioButtonMenuItem(ENABLE_ZOOM);
        item.setToolTipText("Click to disable zooming and fit tree to "
                + "frame size.");
        item.addActionListener(actL);
        item.setMnemonic('E');
        item.setSelected(!m_pane.isZooming());
        menu.add(item);

        return menu;
    }
}
