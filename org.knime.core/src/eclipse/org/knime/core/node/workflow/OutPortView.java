/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecView;
import org.knime.core.node.port.PortObjectView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ViewUtils;

/**
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class OutPortView extends JFrame {
    private static final long serialVersionUID = 1L;

    /** Update object to reset the view. */
    private static final UpdateObject UPDATE_OBJECT_NULL = new UpdateObject(null, null, null, null, null);

    /** Keeps track if view has been opened before. */
    private boolean m_wasOpened = false;

    private final JTabbedPane m_tabbedPane;
    private final Map<String, ViewDetails> m_tabNameToViewDetailMap;


    private final LoadingPanel m_loadingPanel = new LoadingPanel();

    private static final ExecutorService UPDATE_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger m_counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r, "OutPortView-Updater-" + m_counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    private Consumer<JTabbedPane> m_displayWaitingConsumer;

    /**
     * A view showing the data stored in the specified output port.
     *
     * @param nodeNameWithID The name of the node the inspected port belongs to
     * @param portName name of the port which is also displayed in the title
     */
    public OutPortView(final String nodeNameWithID, final String portName) {
        super(portName + " - " + nodeNameWithID);
        // init frame
        super.setName(getTitle());
        KNIMEConstants.getKNIMEIcon16X16().ifPresent(i -> setIconImage(i.getImage()));
        super.setBackground(NodeView.COLOR_BACKGROUND);

        // initialize default window width / height
        DisplayMode displayMode = getGraphicsConfiguration().getDevice().getDisplayMode();
        int width = Math.min(Math.max(2 * displayMode.getWidth() / 3, 600), 1280);
        int height = Math.min(Math.max(2 * displayMode.getHeight() / 3, 400), 720);
        super.setSize(width, height);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(e -> setVisible(false));
        menu.add(item);
        menuBar.add(menu);
        setJMenuBar(menuBar);
        m_tabbedPane = new JTabbedPane();
        m_tabbedPane.addChangeListener(e -> onTabChanged());
        m_tabNameToViewDetailMap = new LinkedHashMap<>();
        getContentPane().add(m_loadingPanel);

        m_displayWaitingConsumer = null;
    }

    /**
     * shows this view and brings it to front.
     * @param knimeWindowBounds the bounds of the KAP main window
     * @noreference This method is not intended to be referenced by clients.
     */
    public void openView(final Rectangle knimeWindowBounds) {
        if (!m_wasOpened) {
            m_wasOpened = true;
            updatePortView();
            ViewUtils.centerLocation(this, knimeWindowBounds);
        }
        // if the view was already visible
        /* bug1922: if the portview is minimized and then opened again (from the
         * context menu) it stays behind the KNIME main window.
         * fix: this strange sequence of calls. It seems to work on Win, Linux,
         * and MacOS.
         */
        setVisible(false);
        setExtendedState(NORMAL);
        setVisible(true);
        toFront();

        // inform PortObjectViews that the view-window has been opened
        m_tabNameToViewDetailMap.values().forEach(c -> {
            if (c.getView() instanceof PortObjectView) {
                ((PortObjectView)c.getView()).open();
            }
        });
    }

    /**
     * Validates and repaints the super component.
     */
    final void updatePortView() {
        invalidate();
        validate();
        repaint();
    }

    /**
     * This is potentially not the final implementation, but has been placed here for quick skeleton example of how an
     * AP-5078 implementation would look. (TODO)
     *
     * This is expected to be called as part of the view construction and display which will embed the tabbed pane.
     *
     * @param consumer a consumer wanting the populated <code>JTabbedPane</code> instance wrapped in this view.
     */
    public void createDisplayAndReturnTabbedPane(final Consumer<JTabbedPane> consumer) {
        m_displayWaitingConsumer = consumer;

        ViewUtils.invokeLaterInEDT(() -> {
            updatePortView();

            if (m_updateObjectReference.get() != null) {
                runUpdateThread();
            }
        });
    }

    /**
     * A utility class that aggregates all objects that are updated in the update method.
     */
    private static final class UpdateObject {
        private final PortObject m_portObject;

        private final PortObjectSpec m_portObjectSpec;

        private final FlowObjectStack m_flowObjectStack;

        private final CredentialsProvider m_credentialsProvider;

        private final NodeContext m_nodeContext;

        private final HiLiteHandler m_hiliteHandler;

        private UpdateObject(final PortObject po, final PortObjectSpec spec, final FlowObjectStack stack,
            final CredentialsProvider prov, final HiLiteHandler hiliteHandler) {
            m_portObject = po;
            m_portObjectSpec = spec;
            m_flowObjectStack = stack;
            m_credentialsProvider = prov;
            m_hiliteHandler = hiliteHandler;
            m_nodeContext = NodeContext.getContext();
        }
    }

    private final AtomicReference<UpdateObject> m_updateObjectReference = new AtomicReference<UpdateObject>();

    /**
     * Sets the content of the view.
     *
     * @param portObject a data table, model content or other
     * @param portObjectSpec data table spec or model content spec or other spec
     * @param stack The {@link FlowObjectStack} of the node.
     * @param credentials the CredenialsProvider used in out-port view
     * @param hiliteHandler the hilite handler active at the port
     * @noreference This method is not intended to be referenced by clients.
     * @since 3.7
     */
    public void update(final PortObject portObject, final PortObjectSpec portObjectSpec, final FlowObjectStack stack,
        final CredentialsProvider credentials, final HiLiteHandler hiliteHandler) {
        UpdateObject updateObject = new UpdateObject(portObject, portObjectSpec, stack, credentials, hiliteHandler);

        // set update object, run update thread only if there was no previous
        // update object (otherwise an update is currently ongoing)
        if (m_updateObjectReference.getAndSet(updateObject) == null && isVisible()) {
            runUpdateThread();
        }

    }

    /** Queues runnable that consumes the update object(s) that it finds in {@link #m_updateObjectReference}. */
    private void runUpdateThread() {
        UPDATE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                UpdateObject upO;
                while ((upO = m_updateObjectReference.get()) != null) {
                    updateInternal(upO);
                    // invalidate update reference only if there is no
                    // new update object in the reference, otherwise
                    // do a new iteration.
                    if (m_updateObjectReference.compareAndSet(upO, null)) {
                        // break out here, do not rely on while() statement
                        // as new UO may be set (another thread is queued)
                        break;
                    }
                }

                if (m_displayWaitingConsumer != null) {
                    m_displayWaitingConsumer.accept(m_tabbedPane);

                    m_displayWaitingConsumer = null;
                }
            }
        });
    }

    /** Internal update method that creates new tabs and displays them. */
    private void updateInternal(final UpdateObject updateObject) {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                NodeContext.pushContext(updateObject.m_nodeContext);
                try {
                    runWithContext();
                } finally {
                    NodeContext.removeLastContext();
                }
            }

            private void runWithContext() {
                // add all port object tabs
                m_tabNameToViewDetailMap.clear();
                PortObject portObject = updateObject.m_portObject;
                PortObjectSpec portObjectSpec = updateObject.m_portObjectSpec;
                FlowObjectStack stack = updateObject.m_flowObjectStack;
                CredentialsProvider credentials = updateObject.m_credentialsProvider;
                HiLiteHandler hiliteHandler = updateObject.m_hiliteHandler;
                if (portObject != null) {
                    JComponent[] poViews = portObject.getViews();
                    if (poViews != null) {
                        for (JComponent comp : poViews) {
                            // fix (bugzilla) 2379: CredentialsProvider needed in
                            // DatabasePortObject to create db connection
                            // while accessing data for preview
                            // fix (jira) AP-6017: hilite support in output table view
                            if (comp instanceof PortObjectView) {
                                PortObjectView poView = (PortObjectView)comp;
                                poView.setCredentialsProvider(credentials);
                                poView.setHiliteHandler(hiliteHandler);
                                poView.open();
                            }
                            m_tabNameToViewDetailMap.put(comp.getName(), ViewDetails.of(comp));
                        }
                    }
                } else {
                    // what to display, if no port object is available?
                    JPanel noDataPanel = new JPanel();
                    noDataPanel.setLayout(new BorderLayout());
                    Box boexle = Box.createHorizontalBox();
                    boexle.add(Box.createHorizontalGlue());
                    boexle.add(new JLabel("No data available!"));
                    boexle.add(Box.createHorizontalGlue());
                    noDataPanel.add(boexle, BorderLayout.CENTER);
                    noDataPanel.setName("No Table");
                    m_tabNameToViewDetailMap.put("No Table", ViewDetails.of(noDataPanel));
                }
                JComponent[] posViews = portObjectSpec == null ? new JComponent[0] : portObjectSpec.getViews();
                if (posViews != null) {
                    for (JComponent comp : posViews) {
                        m_tabNameToViewDetailMap.put(comp.getName(), ViewDetails.of(comp));
                    }
                }

                FlowObjectStackView stackView = new FlowObjectStackView();
                stackView.update(stack);
                m_tabNameToViewDetailMap.put("Flow Variables", ViewDetails.of(stackView));

                for (Component oldComponent : m_tabbedPane.getComponents()) {
                    if (oldComponent instanceof PortObjectView) {
                        PortObjectView poView = (PortObjectView)oldComponent;
                        poView.setCredentialsProvider(null);
                        poView.setHiliteHandler(null);
                        poView.dispose();
                    }
                    if (oldComponent instanceof PortObjectSpecView) {
                        ((PortObjectSpecView)oldComponent).dispose();
                    }
                }
                m_tabbedPane.removeAll();
                for (Map.Entry<String, ViewDetails> entry : m_tabNameToViewDetailMap.entrySet()) {
                    ViewDetails viewDetails = entry.getValue();
                    m_tabbedPane.addTab(entry.getKey(), viewDetails.getView());
                }
                remove(m_loadingPanel);
                if (m_displayWaitingConsumer == null) {
                    add(m_tabbedPane);
                }
                onTabChanged();
                invalidate();
                validate();
                repaint();
            }
        });

    }

    /** Called when a different tab is selected ... updates the menu. */
    private void onTabChanged() {
        assert SwingUtilities.isEventDispatchThread() : "Not in EDT";
        final JMenuBar menuBar = getJMenuBar();
        for (int i = menuBar.getMenuCount(); --i >= 0; ) {
            JMenu m = menuBar.getMenu(i);
            if (m != null && !"File".equals(m.getText())) {
                menuBar.remove(m);
            }
        }
        Component selectedTab = m_tabbedPane.getSelectedComponent();
        String selectedTabName = selectedTab != null ? selectedTab.getName() : null;
        if (/*m_tabbedPane.isVisible() &&*/ selectedTabName != null) {
            ViewDetails viewDetails = m_tabNameToViewDetailMap.get(selectedTabName);
            assert viewDetails != null : "No view details for tab \"" + selectedTabName + "\"";
            viewDetails.getMenus().ifPresent(m -> {
                Arrays.stream(m).forEach(menuBar::add);
            });
        }
        menuBar.repaint();
    }

    /** Sets visibility and checks if an update thread needs to run.
     * {@inheritDoc} */
    @Override
    public void setVisible(final boolean b) {
        if (isVisible() && !b) {
            //if view has been closed
            m_tabNameToViewDetailMap.values().forEach(c -> {
                if (c.getView() instanceof PortObjectView) {
                    ((PortObjectView)c.getView()).close();
                }
            });
        }
        super.setVisible(b);
        if (b && m_updateObjectReference.get() != null) {
            runUpdateThread();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        // release all - identified memory leak via
        // sun.awt.AppContext -> ... Maps -> swing.RepaintManager -> ...> JTabbedPane -> ... -> WFM
        m_updateObjectReference.set(null);
        updateInternal(UPDATE_OBJECT_NULL);
        remove(m_tabbedPane);
        super.dispose();
    }

    /** Displays "loading port content". */
    @SuppressWarnings("serial")
    private static final class LoadingPanel extends JPanel {

        LoadingPanel() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            Box centerBox = Box.createHorizontalBox();
            centerBox.add(Box.createHorizontalGlue());
            centerBox.add(new JLabel("Loading port content..."));
            centerBox.add(Box.createHorizontalGlue());
            add(centerBox);
        }

    }

    private static final class ViewDetails {

        private final JComponent m_view;
        private final Optional<JMenu[]> m_menusOptional;

        /**
         * @param view
         * @param menus
         */
        ViewDetails(final JComponent view, final JMenu[] menus) {
            m_view = Objects.requireNonNull(view);
            m_menusOptional = Optional.ofNullable(menus);
        }

        /** @return the menus */
        Optional<JMenu[]> getMenus() {
            return m_menusOptional;
        }

        /** @return the view */
        JComponent getView() {
            return m_view;
        }

        static ViewDetails of(final JComponent c) {
            return new ViewDetails(c, c instanceof BufferedDataTableView ? ((BufferedDataTableView)c).getMenus() : null);
        }

    }
}
