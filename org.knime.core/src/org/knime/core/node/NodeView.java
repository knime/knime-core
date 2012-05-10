/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.knime.core.node.util.ViewUtils;

/**
 * Node view class that displays the view content in an
 * {@linkplain JFrame AWT-frame}. The part specific to the special purpose
 * node view must be implemented in the derived class and must be placed in
 * a {@linkplain JComponent panel}. This panel is registered in this base
 * class (method {@link #setComponent(Component)}and will be displayed in
 * the <code>JFrame</code> provided and handled by this class.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @param <T> the implementation of the {@link NodeModel} this node view
 *          is based on
 */
public abstract class NodeView<T extends NodeModel> extends AbstractNodeView<T>
        implements NodeModelWarningListener {

    /** Default background color. */
    public static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;

    /** Initial width of the dummy component during construction. */
    private static final int INIT_COMP_WIDTH = 300;

    /** Initial height of the dummy component during construction. */
    private static final int INIT_COMP_HEIGTH = 200;

    /** Underlying frame, not visible from the outside. */
    private final JFrame m_frame;

    /** Component in the center of this frame, set by
     * {@link #setComponent(Component)}. */
    private Component m_comp;

    /** Component that is shown when no data is available (not connected, e.g.)
     * and the view is open. */
    private Component m_noDataComp;

    /** References either to <code>m_comp</code> or <code>m_noDataComp</code>
     * depending on which is currently shown. */
    private Component m_activeComp;

    /** Remembers the first time the actual component was set. The reason is the
     * resizing, the first time the proper component (not the "no data"
     * component) is set. This resizing should only occur the first time. */
    private boolean m_componentSet = false;

    /** Determines if this view is always on top. Useful if special views should
     * stay on top all the time */
    private boolean m_alwaysOnTop = false;

    private final WindowListener m_windowListener = new WindowAdapter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void windowClosing(final WindowEvent e) {
            // triggered when user clicks [x] to close window
            NodeView.super.closeView();
        }
    };

    /** Warning label showing the current warning message of the node model. */
    private final JLabel m_warningLabel;

    /**
     * This class sends property events when the status changes.
     */
    public static final String PROP_CHANGE_CLOSE = "nodeview_close";
    /* TODO
     * So far, the very only possible listener is the EmbeddedNodeView that is
     * informed when the view finally closes (e.g. because the node was
     * deleted). Once the member m_frame is deleted from this class, the frame
     * will also be a potential listener.
     */

    /** Create a new view for a given (non-null) model. Subclasses will
     * initialize all view components in the their constructor and set it
     * by calling {@link #setComponent(Component)}.
     *
     * <p>This constructor creates the frame and the default menu bar.
     *
     * @param nodeModel The underlying node model.
     * @throws NullPointerException If the <code>nodeModel</code> is null.
     * @see #setComponent(Component)
     */
    protected NodeView(final T nodeModel) {
        super(nodeModel);

        m_frame = new JFrame();
        if (KNIMEConstants.KNIME16X16 != null) {
            m_frame.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        m_frame.setBackground(COLOR_BACKGROUND);
        // DO_NOTHING sends a windowClosing to window listeners
        m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        m_frame.addWindowListener(m_windowListener);

        // creates menu item to close this view
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');

        // create always on top entry
        JMenuItem item = new JCheckBoxMenuItem("Always on top", m_alwaysOnTop);
        item.setMnemonic('T');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {

                boolean selected =
                    ((JCheckBoxMenuItem)event.getSource()).isSelected();
                m_alwaysOnTop = selected;
                m_frame.setAlwaysOnTop(m_alwaysOnTop);
            }
        });
        menu.add(item);

        item = NodeViewExport.createNewMenu(this);
        menu.add(item);

        // create close entry
        item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                NodeView.super.closeView();
            }
        });
        menu.add(item);
        menuBar.add(menu);
        m_frame.setJMenuBar(menuBar);

        // set properties of the content pane
        Container cont = m_frame.getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(COLOR_BACKGROUND);

        // set a dummy component to get the default size
        setShowNODATALabel(true);
        setComponent(m_noDataComp);

        // add warning label
        m_warningLabel = new JLabel("", WARNING_ICON, SwingConstants.LEFT);
        Font font = m_warningLabel.getFont().deriveFont(Font.BOLD);
        m_warningLabel.setFont(font);
        m_warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        m_warningLabel.setBackground(Color.WHITE);
        m_warningLabel.setOpaque(true);
        cont.add(m_warningLabel, BorderLayout.NORTH);
    }

    /** Icon in the status bar that shows the node's warning message. */
    static final Icon WARNING_ICON = ViewUtils.loadIcon(
            NodeView.class, "/icon/warning.png");

    /** Method that is called by the framework when the view is saved as image
     * or vector graphics (action available through the file menu). The default
     * implementation simply returns the component that was added by calling
     * {@link #setComponent(Component)}; subclasses can overwrite it and return
     * the "important" component, e.g. excluding an overview or controls.
     * @return The component being rendered when the view is exported as image,
     *         never null.
     * @since 2.6
     */
    protected Container getExportComponent() {
        return m_frame.getContentPane();
    }

    /**
     * Sets the property if the "no data" label is shown when the underlying
     * node is not executed but the view is shown (replaces whatever has been
     * set by <code>#setComponent(Component)</code>. Once the node is
     * executed the user panel is shown again.
     *
     * @param showIt <code>true</code> for replace the current view,
     *            <code>false</code> always show the real view.
     */
    protected final void setShowNODATALabel(final boolean showIt) {
        m_noDataComp = (showIt ? createNoDataComp() : null);
    }

    /** {@inheritDoc} */
    @Override
    final void callModelChanged() {
        synchronized (getNodeModel()) {
            super.callModelChanged();
            setComponent(m_comp);
        }
    }

    /**
     * Invoked when the window is about to be closed. Unregister
     * <code>HiLiteListeners</code>. Dispose internal members. <br />
     * This method is the first to be called on a close request (right after
     * the view is unregistered from the {@link NodeModel}
     */
    protected abstract void onClose();

    /**
     * Invoked when the window has been opened. Register property listeners.
     * <br />
     * This method is called last on view construction - right before the
     * components are made visible. It is not called on re-opening.
     */
    protected abstract void onOpen();

    /**
     * Returns menu bar of the accompanying frame.
     *
     * @return menu bar.
     */
    public final JMenuBar getJMenuBar() {
        return m_frame.getJMenuBar();
    }

    /** {@inheritDoc} */
    @Override
    final void callOpenView(final String title) {
        try {
            onOpen();
        } catch (Throwable t) {
            getLogger().error("NodeView.onOpen() causes an error "
                    + "on opening node view, reason: " + t.getMessage(), t);
        }
        getNodeModel().addWarningListener(this);
        callModelChanged();
        warningChanged(getNodeModel().getWarningMessage());
        // show frame, make sure to do this in EDT (GUI related task)
        Runnable runner = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                m_frame.setName(title);
                setTitle(title);
                if (m_comp != null) {
                    m_comp.invalidate();
                    m_comp.repaint();
                }
                m_frame.pack();
                m_frame.setLocationRelativeTo(null); // puts in screen center
                m_frame.setVisible(true);
                m_frame.toFront();
            }
        };
        ViewUtils.runOrInvokeLaterInEDT(runner);
    }

    /** Closes the view programmatically. Sub-classes should not call this
     * method as view closing is task of the framework. This method is
     * public for historical reasons and will be removed in upcoming versions.
     * @deprecated Will be removed without replacement in future versions
     * of KNIME. Sub-classes should not be required to programmatically close
     * views. */
    @Deprecated
    @Override
    public final void closeView() {
        super.closeView();
    }

    /**
     * Called by the node when it is deleted or by the "close" button. Disposes
     * the frame.
     * <p>
     * Calls the onClose method and unregisters this view from the model. If you
     * derive this class, <strong>do not</strong> call this method. It's being
     * used by the framework (if views are shown within a JFrame) or by eclipse
     * (if available, i.e. when views are embedded in eclipse).
     */
    @Override
    public final void callCloseView() {
        try {
            onClose();
        } catch (Throwable t) {
            getLogger().error("NodeView.onClose() causes an error "
                    + "during closing node view, reason: " + t.getMessage(), t);
        }
        getNodeModel().removeWarningListener(this);
        m_frame.getContentPane().firePropertyChange(PROP_CHANGE_CLOSE, 0, 1);
        m_activeComp = null;
        m_comp = null;
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                m_frame.setVisible(false);
                m_frame.removeWindowListener(m_windowListener);
                m_frame.getContentPane().removeAll();
                m_frame.dispose();
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * Initializes all view components and returns the view's content pane. If
     * you derive this class, <strong>do not</strong> call this method. It's
     * being used by the framework (if views are shown within a JFrame) or by
     * eclipse (if available, i.e. when views are embedded in eclipse)
     *
     * @return The view's content pane.
     * @deprecated Will be removed without replacement in future
     *             versions of KNIME.
     * @see #getComponent()
     */
    @Deprecated
    public final Component openViewComponent() {
        return m_frame.getContentPane();
    }

    /** Opens the new view. Subclasses should not be required to open views
     * programmatically. Opening is done via the framework and dedicated user
     * actions.
     * @param viewTitle the tile for this view
     * @return a {@link JFrame} with an initialized {@link NodeView}
     * @deprecated This method will be removed without replacement in future
     *             versions of KNIME as client code should not be required to
     *             open views.
     */
    @Deprecated
    public final JFrame createFrame(final String viewTitle) {
        final String name;
        if (viewTitle == null) {
            name = "View \"no title\"";
        } else {
            name = viewTitle;
        }
        openView(name);
        return m_frame;
    }

    /**
     * @return Checks whether the view is open or not.
     */
    protected final boolean isOpen() {
        // broken if view is opened via openViewComponent
        return m_frame.isVisible();
    }

    /**
     * Append this suffix to the current view name.
     * If <code>suffix</code> is <code>null</code> the title does not change.
     *
     * @param suffix append this suffix to the current view name
     */
    protected final void setViewTitleSuffix(final String suffix) {
        if (suffix != null) {
            setTitle(m_frame.getName() + " " + suffix);
        }
    }

    private void setTitle(final String title) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                m_frame.setTitle(title);
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * @return The current view's title.
     */
    public final String getViewTitle() {
        return m_frame.getTitle();
    }

    /**
     * Returns the underlying content pane's panel placed at the center of the
     * view.
     *
     * @return panel of the view's center area.
     */
    protected final Component getComponent() {
        return m_comp;
    }

    /**
     * Sets the panel of the view's content pane center area. Register your
     * <code>Component</code> that implements the functionality of the derived
     * class with this function. The foreground and background colors of your
     * panel are set to the default colors defined in this class.
     *
     * @param comp Component to set in the center of the view.
     */
    protected final void setComponent(final Component comp) {
        Runnable runner = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
             // pack frame only when setting the component for the first time
                boolean pack = false;
                if (!getNodeModel().hasContent() && m_noDataComp != null) {
                    setComponentIntern(m_noDataComp);
                } else {
                    setComponentIntern(comp);
                    if (!m_componentSet) {
                        pack = true;
                    }
                    m_componentSet = true;
                }
                m_comp = comp;
                if (pack) {
                    /*
                     * This is necessary when the view was opened before the
                     * node was executed. When the node gets executed the view
                     * has to be resized and adapted to its content.
                     * We had to remove the call to pack() in order to make it
                     * run on a Mac Now we manually resize the frame
                     */
                    m_frame.invalidate();
                    m_frame.validate();
                    Dimension size = m_frame.getRootPane().getPreferredSize();
                    m_frame.setSize(size);
                } else {
                    m_frame.invalidate();
                    m_frame.validate();
                }
                m_frame.repaint();
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * Helper method that internally sets the current component; it does not
     * update m_comp (which setComponent does).
     *
     * @param cmp The new component to show (might be m_noDataComp)
     */
    private void setComponentIntern(final Component cmp) {
        if (m_activeComp == cmp || cmp == null) {
            return;
        }

        Container cont = m_frame.getContentPane();
        if (m_activeComp != null) {
            cont.remove(m_activeComp);
        }

        m_activeComp = cmp;
        cont.add(m_activeComp, BorderLayout.CENTER);
    }

    /**
     * Creates the label that is shown when no node is not connected or not
     * executed.
     *
     * @return Default "no label" component.
     */
    private Component createNoDataComp() {
        JLabel noData =
            new JLabel("<html><center>No data to display</center></html>",
                    SwingConstants.CENTER);
        noData.setPreferredSize(new Dimension(INIT_COMP_WIDTH,
                INIT_COMP_HEIGTH));
        return noData;
    }

    /** {@inheritDoc} */
    @Override
    public void warningChanged(final String warning) {
        if (warning != null && warning.trim().length() > 0) {
            m_warningLabel.setIcon(WARNING_ICON);
            m_warningLabel.setText(warning.trim());
            m_warningLabel.setToolTipText(warning.trim());
            m_warningLabel.setPreferredSize(
                    new Dimension(m_frame.getContentPane().getWidth(), 20));
        } else {
            m_warningLabel.setIcon(null);
            m_warningLabel.setText(null);
            m_warningLabel.setToolTipText(null);
            m_warningLabel.setPreferredSize(new Dimension(0, 0));
        }
    }
}
