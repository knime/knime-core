/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.navigator.actions;

import org.eclipse.jface.action.Action;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class OpenJobManagerDialogAction extends Action {

    /* 
     ************************************************
     *                 BACKUP
     *           ==================
     * If we want to have a nicer SWT dialog for 
     * configuring a workflow - activate this class.
     ************************************************ 
     *
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            OpenJobManagerDialogAction.class);
    
    private Shell m_shell;
    
    private NodeExecutorJobManagerDialogTab m_tab;
    
    private WorkflowManager m_workflow;
    
    @Override
    public String getText() {
        return "Execute Workflow...";
    }
    
    
    @Override
    public String getDescription() {
        return "Configure how this workflow is executed";
    }

    @Override
    public boolean isEnabled() {
        // get selection
          IStructuredSelection s = (IStructuredSelection)PlatformUI.getWorkbench()
          .getActiveWorkbenchWindow().getSelectionService()
          .getSelection(KnimeResourceNavigator.ID);
          Object element = s.getFirstElement();
          // check if is KNIME workflow
          if (element instanceof IContainer) {
              IContainer cont = (IContainer)element;
              if (cont.exists(new Path(WorkflowPersistor.WORKFLOW_FILE))) {
                  m_workflow = (WorkflowManager)ProjectWorkflowMap.getWorkflow(
                          cont.getFullPath().toString());
                  if (m_workflow != null) {
                      return true;
                  }
              }
          }
        return false;
    }
    
    @Override
    public void run() {
        m_shell = new Shell(Display.getDefault());
        m_shell.setLayout(new GridLayout(1, true));
        m_shell.setText("Configure Workflow Execution...");
        ImageDescriptor icon = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, "icons/knime_default.png");
        m_shell.setImage(icon.createImage());

        m_tab = new NodeExecutorJobManagerDialogTab();
        
        m_tab.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(
                        NodeExecutorJobManagerDialogTab.PROP_WIDTH)) {
                    final int newW = (Integer)evt.getNewValue();
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            // 350 -> necessary width to display all buttons
                            int w = Math.max(newW + 20, 350); 
                            m_shell.setSize(w + 20, m_shell.getSize().y);
                        }
                    });
                }
            }
        });

        m_tab.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(
                        NodeExecutorJobManagerDialogTab.PROP_HEIGHT)) {
                    final int newH = (Integer)evt.getNewValue();
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            m_shell.setSize(m_shell.getSize().x, newH + 100);
                        }
                    });
                }
            }
        });

        Composite dialogArea = new Composite(m_shell, SWT.NONE);
        dialogArea.setLayout(new GridLayout(1, true));
        
        Composite awtArea = new Composite(m_shell, SWT.EMBEDDED | SWT.FOCUSED
                | SWT.NO_BACKGROUND);
        awtArea.setLayout(new GridLayout(1, true));
//        GridData data = new GridData(SWT.LEFT, SWT.TOP, true, true);
        awtArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        
        final Frame awtFrame = SWT_AWT.new_Frame(awtArea);
        JApplet rootPane = new JApplet();
        rootPane.add(m_tab);
        awtFrame.add(rootPane);
        awtFrame.toFront();
        
        createButtons(m_shell);
        
        m_shell.pack();

        Dimension size = m_tab.getPreferredSize();
        
        Rectangle windowBounds = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell().getBounds();
        m_shell.setBounds(windowBounds.x, windowBounds.y,
                m_shell.getSize().x, m_shell.getSize().y + size.height);

        m_shell.setActive();
        m_shell.setVisible(true);
    }
    

    
    /*
     * Create the "Apply" button on the bottom of the dialog. 
     * @param parent parent composite
     *
    protected void createButtons(final Composite parent) {
        Composite buttonArea = new Composite(parent, SWT.NONE);
        buttonArea.setLayout(new GridLayout(3, true));
        buttonArea.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, 
                true, false));
        Button execBtn = new Button(buttonArea, SWT.PUSH);
        execBtn.setText("Execute");
        GridData data = new GridData();
        data.widthHint = 100;
        data.heightHint = 25;
        execBtn.setLayoutData(data);
        execBtn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                executeWorkflow();
            }
        });
        
        Button applyBtn = new Button(buttonArea, SWT.PUSH);
        applyBtn.setText("Apply");
        applyBtn.setLayoutData(data);
        applyBtn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                applySettings();
            }
            
        });
        
        Button cancelBtn = new Button(buttonArea, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(data);
        cancelBtn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                m_shell.dispose();
            }
            
        });
    }
    
    protected void executeWorkflow() {
        LOGGER.info("Now will execute workflow ");
        LOGGER.info("workflow: " + m_workflow.getName());
    }
    
    protected void applySettings() {
        LOGGER.info("Settings will be applied - without executing...");
    }
    
    
//    private WorkflowManager getSelectedWorkflow() {
//      IStructuredSelection s = (IStructuredSelection)PlatformUI.getWorkbench()
//            .getActiveWorkbenchWindow().getSelectionService()
//            .getSelection(KnimeResourceNavigator.ID);
//        Object element = s.getFirstElement();
//        if (element instanceof WorkflowManager) {
//            return (WorkflowManager)element;
//        }
//        return null;
//    }
    */
}
