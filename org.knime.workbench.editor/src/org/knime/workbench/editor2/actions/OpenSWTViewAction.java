/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   10.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class OpenSWTViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private final int m_index;

//    private static final NodeLogger LOGGER = NodeLogger
//            .getLogger(OpenSWTViewAction.class);

//    private Map<Menu, JMenu>m_menuMapping = new HashMap<Menu, JMenu>();

    /**
     * New action to opne a node view.
     *
     * @param nodeContainer The node
     * @param viewIndex The index of the node view
     */
    public OpenSWTViewAction(final NodeContainer nodeContainer,
            final int viewIndex) {
        m_nodeContainer = nodeContainer;
        m_index = viewIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens node view " + m_index + ": "
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "SWT View: " + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // TODO: temporarily disabled to make it compile
        /*
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                try {
                    Display display = Display.getCurrent();
                    final Shell shell = new Shell(display);
                    shell.setText("Node View " + m_index + " for Node: "
                            + m_nodeContainer.getNameWithID());
                    final Component viewContent =
                            m_nodeContainer.getView(m_index).getComponent();
                    shell.setSize(viewContent.getPreferredSize().width + 100,
                            viewContent.getPreferredSize().height + 100);

                    Composite composite = new Composite(shell, SWT.EMBEDDED);
                    composite.setBounds(0, 0,
                            viewContent.getPreferredSize().width + 100,
                            viewContent.getPreferredSize().height + 100);
                    composite.setLayout(new FillLayout());

                    final Frame frame = SWT_AWT.new_Frame(composite);
                    final JApplet applet = new JApplet();

                    createMenuBar(shell,
                            m_nodeContainer.getView(m_index).getJMenuBar());

                    frame.add(applet);
                    applet.add(viewContent);

                    frame.pack();
                    shell.pack();
                    shell.open();
                    viewContent.repaint();

                    while (!shell.isDisposed()) {
                        if (!display.readAndDispatch()) {
                            display.sleep();
                        }
                    }
                    if (!shell.isDisposed()) {
                        shell.close();
                    }
                } catch (Throwable t) {
                    LOGGER.error("The view " + m_index + " for node '"
                            + m_nodeContainer.getNameWithID()
                            + "' has thrown a '"
                            + t.getClass().getSimpleName()
                            + "'. That is most likely an implementation error.",
                            t);
                }
            
            }

            private void createMenuBar(final Shell shell,
                    final JMenuBar menuBar) {
                Menu topMenu = new Menu(shell, SWT.BAR);
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu jMenu = menuBar.getMenu(i);
                    LOGGER.debug("menu: " + jMenu.getText());
                    MenuItem menuItem = new MenuItem(topMenu, SWT.CASCADE);
                    menuItem.setText(jMenu.getText());
                    addMenuItems(menuItem, jMenu);
                }
                shell.setMenuBar(topMenu);
            }

            private void addMenuItems(final MenuItem topMenu,
                    final JMenu jMenu) {
                Menu swtMenu = new Menu(topMenu);
                for (int i = 0; i < jMenu.getItemCount(); i++) {
                    final JMenuItem jItem = jMenu.getItem(i);
                    final MenuItem swtItem = new MenuItem(swtMenu, SWT.PUSH);
                    swtItem.setText(jItem.getText());
                    LOGGER.debug("sub item: " + jItem.getText());
                    swtItem.addSelectionListener(new SelectionListener() {

                        public void widgetDefaultSelected(
                                final SelectionEvent e) {
                            widgetSelected(e);
                        }

                        public void widgetSelected(final SelectionEvent e) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    for (ActionListener l : jItem.getActionListeners()) {
                                        l.actionPerformed(new ActionEvent(jItem, 0,
                                                jItem.getText()));
                                    }

                                }

                            });
                        }

                    });
                }
                topMenu.setMenu(swtMenu);
            }
        });
        */
    }
}
