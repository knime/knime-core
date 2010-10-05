/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
