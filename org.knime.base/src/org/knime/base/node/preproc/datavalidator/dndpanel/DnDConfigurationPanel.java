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
 *   04.10.2014 (Marcel): created
 */
package org.knime.base.node.preproc.datavalidator.dndpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TooManyListenersException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.knime.base.node.preproc.datavalidator.dndpanel.DnDConfigurationPanel.DnDConfigurationSubPanel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;

/**
 * A UI widget, which helps to create drag and drop based UIs. It consists of a scroll panel, which comprises several
 * configuration sub panels (implementations of {@link DnDConfigurationSubPanel}). Each of the configuration is intended
 * to configure the node behavior for multiple columns. Add this panel to JComponent which uses the KLNIME DnD* classes
 * to enable drag and drop. Then users can drag and drop {@link DataColumnSpec}s from that source to a creation button
 * in this dialog and to the individual sub-dialogs.
 *
 * @author Marcel Hanser
 * @param <T>
 * @since 2.11
 */
@SuppressWarnings("serial")
public abstract class DnDConfigurationPanel<T extends DnDConfigurationSubPanel> extends JPanel implements Iterable<T>,
    DnDStateListener, DnDDropListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DnDConfigurationPanel.class);

    /**
     * Name of the property changed if configuration have been added.
     */
    public static final String CONFIGURATION_CHANGED = "CONFIGURATION_CHANGED";

    /**
     * A green "+" icon.
     */
    public static final Icon ADD_ICON_48;

    /**
     * A small green "+" icon.
     */
    public static final Icon ADD_ICON_16;

    static {
        Icon toSetBit;
        Icon toSetSmall;
        try {
            BufferedImage bi = ImageIO.read(DnDConfigurationPanel.class.getResourceAsStream("add.png"));
            toSetBit = new ImageIcon(bi.getScaledInstance(48, 48, Image.SCALE_DEFAULT));
            toSetSmall = new ImageIcon(bi.getScaledInstance(15, 15, Image.SCALE_DEFAULT));
        } catch (IOException e) {
            toSetBit = null;
            toSetSmall = null;
            LOGGER.error("cannot load add icon", e);
        }
        ADD_ICON_48 = toSetBit;
        ADD_ICON_16 = toSetSmall;
    }

    private final InnerConfigurationPanel m_configPanel;

    private final JPanel m_helpPanel;

    private final JScrollPane m_scroller;

    private JFrame m_currentDragAddPanel;

    /** Set box layout. */
    public DnDConfigurationPanel() {

        m_configPanel = new InnerConfigurationPanel();

        m_scroller =
            new JScrollPane(m_configPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel scrollP = new JPanel(new FlowLayout());
        scrollP.add(m_scroller);

        setLayout(new BorderLayout());

        m_helpPanel = new JPanel(new BorderLayout());
        m_helpPanel.setPreferredSize(getDefaultPreferredSize());
        JPanel inner = new JPanel();
        inner.setLayout(new BorderLayout());
        inner.add(new JLabel("<html><center>Drag columns<br><center>"), BorderLayout.NORTH);
        JLabel jLabel = new JLabel(ADD_ICON_16);
        //        jLabel.setEnabled(false);
        inner.add(jLabel, BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createTitledBorder(""));
        m_helpPanel.add(ViewUtils.getInFlowLayout(inner), BorderLayout.CENTER);
        m_helpPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        add(m_helpPanel, BorderLayout.CENTER);
    }

    /**
     * @return the preferred size of the an used {@link DnDConfigurationSubPanel} implementation.
     */
    protected abstract Dimension getDefaultPreferredSize();

    /**
     * Called if the user dropped {@link DataColumnSpec}s onto the add button.
     *
     * @param specs the dropped specs
     * @return a concrete {@link DnDConfigurationSubPanel} based on the given specs
     */
    protected abstract T createConfigurationPanel(final List<DataColumnSpec> specs);

    /**
     * Adds a configuration panel.
     *
     * @param panel to add
     */
    public void addConfigurationPanel(final T panel) {
        m_configPanel.add(panel);
        checkForHelpPanel();
        m_scroller.revalidate();
        firePropertyChange(CONFIGURATION_CHANGED, null, null);
    }

    /**
     * Removes the configuration panel. If the last panel is removed the helper panel for the DnD usage is shown.
     *
     * @param panel to remove
     */
    public void removeConfigurationPanel(final T panel) {
        m_configPanel.remove(panel);
        checkForHelpPanel();
        m_scroller.revalidate();
        firePropertyChange(CONFIGURATION_CHANGED, null, null);
    }

    /**
     * Removes all configuration panel. The helper panel for the DnD usage is shown.
     */
    public void removeAllConfigurationPanels() {
        m_configPanel.removeAll();
        checkForHelpPanel();
    }

    /**
     * @param curr current item
     */
    public void ensureConfigurationPanelVisible(final T curr) {
        m_scroller.validate();
        m_configPanel.scrollRectToVisible(curr.getBounds());
    }

    /**
     * Safe since we only adding T in {@link #addConfigurationPanel(DnDConfigurationSubPanel)}.
     *
     * @return all added configuration panels
     */
    @SuppressWarnings("unchecked")
    public List<T> getConfigurationPanels() {
        List<T> toReturn = new ArrayList<>();
        for (Component c : m_configPanel.getComponents()) {
            toReturn.add((T)c);
        }
        return toReturn;
    }

    /**
     * @return the panel opened on a dropable
     */
    protected JPanel createNewConfigPanel() {
        JPanel creatorPanel = new JPanel(new BorderLayout());
        JButton addButton = new JButton(ADD_ICON_48);
        creatorPanel.add(new JLabel("New configuration"), BorderLayout.NORTH);
        creatorPanel.add(addButton, BorderLayout.CENTER);
        creatorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return creatorPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void dragStoppedAt() {
        if (m_currentDragAddPanel != null) {
            m_currentDragAddPanel.setVisible(false);
            m_currentDragAddPanel = null;
        }
    }

    @Override
    public void dragStartedAt(final Point location, final Transferable transferable) {
        if (isDropable(DnDColumnSpecTransferable.extractColumnSpecs(transferable))) {
            m_currentDragAddPanel = new JFrame();
            m_currentDragAddPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Already there
            m_currentDragAddPanel.setUndecorated(true);
            m_currentDragAddPanel.setAlwaysOnTop(true);
            JPanel creatorPanel = createNewConfigPanel();

            creatorPanel.setTransferHandler(new DnDColumnSpecTargetTransferHander(this));

            try {
                creatorPanel.getDropTarget().addDropTargetListener(new BorderingDnDListener(creatorPanel, this));
            } catch (TooManyListenersException e) {
                LOGGER.coding("Too many listeners on own created panel... should not happen", e);
            }

            Point locationOnScreen = getLocationOnScreen();
            locationOnScreen.y = location.y;
            m_currentDragAddPanel.add(creatorPanel);
            m_currentDragAddPanel.pack();
            locationOnScreen.y -= m_currentDragAddPanel.getPreferredSize().getHeight() / 2;
            m_currentDragAddPanel.setLocation(locationOnScreen);
            m_currentDragAddPanel.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean update(final List<DataColumnSpec> extractColumnSpecs) {
        addConfigurationPanel(createConfigurationPanel(extractColumnSpecs));
        return true;
    }

    private void checkForHelpPanel() {
        if (m_configPanel.getComponents().length == 0) {
            if (getComponent(0) != m_helpPanel) {
                removeAll();
                add(m_helpPanel);
                revalidate();
                repaint();
            }
        } else {
            if (getComponent(0) != m_scroller) {
                removeAll();
                add(m_scroller);
                revalidate();
                repaint();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        return m_scroller.getPreferredSize();
    }

    @Override
    public Iterator<T> iterator() {
        return getConfigurationPanels().iterator();
    }

    /**
     * The individual configuration panels.
     *
     * @author Marcel Hanser
     */
    public abstract static class DnDConfigurationSubPanel extends JPanel implements DnDDropListener {

        /**
         * Constructor.
         */
        public DnDConfigurationSubPanel() {
            this.setTransferHandler(new DnDColumnSpecTargetTransferHander(this));
            try {
                this.getDropTarget().addDropTargetListener(new BorderingDnDListener(this, this));
            } catch (TooManyListenersException e) {
                LOGGER.coding("Too many listeners on own created panel... should not happen", e);
            }
        }
    }

    private static final class BorderingDnDListener extends DropTargetAdapter {
        private Border m_borderBackup;

        private final JComponent m_componentToBorder;

        private final DnDDropListener m_dropListener;

        private BorderingDnDListener(final JComponent componentToBorder, final DnDDropListener dropListener) {
            super();
            m_componentToBorder = componentToBorder;
            m_dropListener = dropListener;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dragEnter(final DropTargetDragEvent dtde) {
            if (dtde.getTransferable().isDataFlavorSupported(DnDColumnSpecTransferable.DATA_COLUMN_SPEC_FLAVOR)) {
                m_borderBackup = m_componentToBorder.getBorder();
                List<DataColumnSpec> extractColumnSpecs =
                    DnDColumnSpecTransferable.extractColumnSpecs(dtde.getTransferable());

                if (extractColumnSpecs != null && m_dropListener.isDropable(extractColumnSpecs)) {
                    m_componentToBorder.setBorder(BorderFactory.createLineBorder(Color.green));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dragExit(final DropTargetEvent dte) {
            restoreBackup();
        }

        private void restoreBackup() {
            m_componentToBorder.setBorder(m_borderBackup);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void drop(final DropTargetDropEvent dtde) {

        }
    }

    /**
     * The individual panel at the right side of the panel.
     *
     * @author Marcel Hanser
     */
    private final class InnerConfigurationPanel extends JPanel implements Scrollable {

        private InnerConfigurationPanel() {
            BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(layout);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredSize() {

            if (getComponentCount() < 1) {
                return getDefaultPreferredSize();
            }
            int height = 0;
            int width = 0;
            for (Component c : getComponents()) {
                Dimension h = c.getPreferredSize();
                height += h.height;
                width = Math.max(width, h.width);
            }
            return new Dimension(width, height);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension preferredSize = getDefaultPreferredSize();
            preferredSize.height *= 2;
            return preferredSize;
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, //
            final int direction) {
            int rh = getComponentCount() > 0 ? getComponent(0).getHeight() : 0;
            return (rh > 0) ? Math.max(rh, (visibleRect.height / rh) * rh) : visibleRect.height;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return getComponentCount() > 0 ? getComponent(0).getHeight() : 100;
        }
    }
}
