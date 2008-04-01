/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   31.03.2008 (gabriel): created
 */
package org.knime.workbench.ui.masterkey;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * Preference page used to enter (or not) a master key for KNIME.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MasterKeyPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {
    
    private BooleanFieldEditor m_isMasterKey;
    private StringFieldEditor m_masterKey;
    private StringFieldEditor m_masterKeyConfirm;
    
    /**
     * Static encryption key supplier registered with the eclipse framework
     * and serves as a master key provider when the preference page is opened
     * the first time.
     */
    public static final EclipseEncryptionKeySupplier SUPPLIER = 
        new EclipseEncryptionKeySupplier();    
    
    private static class EclipseEncryptionKeySupplier 
            implements EncryptionKeySupplier {
        private String m_lastMasterKey;
        private Boolean m_isDefined;
        /**
         * {@inheritDoc}
         */
        public String getEncryptionKey() {
            if (m_isDefined != null) {
                 return (m_isDefined.booleanValue() ? m_lastMasterKey : null);
            }
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    m_lastMasterKey = openDialogAndReaderKey();
                }
            });
            m_isDefined = (m_lastMasterKey != null);
            return m_lastMasterKey;
        }
    }
   
    /**
     * 
     */
    public MasterKeyPreferencePage() {
        super(GRID);
        setDescription(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();
        m_isMasterKey = new BooleanFieldEditor(
                PreferenceConstants.P_MASTER_KEY_DEFINED, 
                "Enable password en-/decryption", parent);
        m_isMasterKey.load();
        super.addField(m_isMasterKey);
        m_masterKey = new StringFieldEditor(
                PreferenceConstants.P_MASTER_KEY, "Master key: ", 20, parent);
        m_masterKey.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKey);
        m_masterKeyConfirm = new StringFieldEditor(
                PreferenceConstants.P_MASTER_KEY, "Confirm: ", 20, parent);
        m_masterKeyConfirm.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKeyConfirm);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        if (SUPPLIER.m_isDefined != null) {
            getPreferenceStore().setValue(
                    PreferenceConstants.P_MASTER_KEY_DEFINED, 
                    SUPPLIER.m_isDefined);
        }
        super.initialize();
        m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
        m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        if (m_isMasterKey.getBooleanValue()) {
            String masterKey = m_masterKey.getStringValue();
            boolean valid = 
                checkMasterKeys(masterKey, m_masterKeyConfirm.getStringValue());
            if (!valid) {
                SUPPLIER.m_isDefined = false;
                return false;
            }
            SUPPLIER.m_lastMasterKey = m_masterKey.getStringValue();
        } else {
            SUPPLIER.m_lastMasterKey = null;
        }
        SUPPLIER.m_isDefined = m_isMasterKey.getBooleanValue();
        getPreferenceStore().setValue(PreferenceConstants.P_MASTER_KEY_DEFINED, 
                SUPPLIER.m_isDefined);
        return true;
    }
    
    private static boolean checkMasterKeys(final String masterKey, 
            final String confirmMasterKey) {
        if (masterKey == null) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Empty master key...");
            mb.setMessage("The master key must not be empty.");
            mb.open();
            return false;
        }
        if (!masterKey.equals(confirmMasterKey)) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Confirm master key...");
            mb.setMessage("Make sure both master keys are the same.");
            mb.open();
            return false;
        }
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void performApply() {
        this.performOk();
    }
    
    private static final String DESCRIPTION = 
              "KNIME requires an encryption key to encrypt/decrypt passwords,\n"
            + "mainly for database passwords in nodes connecting to databases\n"
            + "(e.g. database reader/writer nodes).\n"
            + "This avoids having to enter passwords for each new session\n"
            + "and each node individually. To avoid storing those passwords\n"
            + "in plain text, the central encryption key is used.\n\n"

            + "Up to now no key has been supplied which can be used to\n"
            + "encrypt/decrypt the passwords.\n\n"

            + "Note: if you have entered a key in a previous session which\n"
            + "has been used to encrypt passwords, those passwords can\n"
            + "obviously only be decrypted with the same key.\n\n";

    private static String openDialogAndReaderKey() {
        final String[] masterKey = new String[]{null};
        Display display;
        try {
            display = new Display();
        } catch (Throwable t) {
            display = Display.getDefault();
        }
        // search for suitable parents
        Shell[] shells = display.getShells();
        Shell dialogShell = null;
        if (shells != null) {
            for (Shell posShell : shells) {
                if (posShell.getData() != null
                      && posShell.getData() instanceof WrappedNodeDialog) {
                    dialogShell = posShell;
                    break;
                } else if (posShell.getData() != null
                   && posShell.getData() instanceof ProgressMonitorDialog) {
                    dialogShell = posShell;
                    break;
                }
            }

            // if dialogShell is null get another default shell
            if (dialogShell == null) {
                if (shells.length > 0) {
                    dialogShell = shells[0];
                }
            }
        }

        Shell shell = null;
        if (dialogShell != null) {
            shell = new Shell(dialogShell);
        } else {
            shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
        }

        try {
            shell.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, "/icons/knime.png").createImage());
        } catch (Throwable e) {
            // do nothing, is just the icon
        }
        shell.setText("KNIME Master Key");
        shell.setSize(300, 200);
        shell.forceActive();
        shell.forceFocus();

        GridLayout gridLayout = new GridLayout();

        gridLayout.numColumns = 1;

        shell.setLayout(gridLayout);

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;

        final Label label = new Label(shell, SWT.NONE);
        label.setText(DESCRIPTION);
        label.setBounds(20, 15, 300, 260);
        label.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        final Label keyLable = new Label(shell, SWT.NONE);
        keyLable.setText("Master key:");
        keyLable.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        final Label spaceLabel2 = new Label(shell, SWT.NONE);
        spaceLabel2.setText("");
        spaceLabel2.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        // gridData.widthHint = 200;
        final Text text = new Text(shell, SWT.PASSWORD | SWT.BORDER);
        // text.setBounds(140, 270, 200, 20);
        text.setSize(200, 20);
        text.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        // gridData.widthHint = 200;
        final Text confirm = new Text(shell, SWT.PASSWORD | SWT.BORDER);
        // text.setBounds(140, 270, 200, 20);
        confirm.setSize(200, 20);
        confirm.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        final Label spaceLabel = new Label(shell, SWT.NONE);
        spaceLabel.setText("\n");
        spaceLabel.setLayoutData(gridData);
        
        gridLayout.numColumns = 2;

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
        gridData.widthHint = 80;
        gridData.horizontalSpan = 1;
        final Button button = new Button(shell, SWT.PUSH);
        button.setText("OK");
        // button.setBounds(20, 270, 180, 20);
        button.setSize(150, 20);
        button.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
        gridData.widthHint = 80;
        gridData.horizontalSpan = 1;
        final Button cancel = new Button(shell, SWT.PUSH);
        cancel.setText("Cancel");
        // cancel.setBounds(20, 270, 180, 20);
        cancel.setSize(150, 20);
        cancel.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;

        final Label info = new Label(shell, SWT.NONE);
        info.setText("");
        // label.setBounds(20, 15, 380, 260);
        info.setLayoutData(gridData);

        Composite parent = shell.getParent();
        if (parent != null) {
            shell.setBounds(parent.getBounds().width / 2, parent
                    .getBounds().height / 2, 300, 200);
        } else {
            shell.setBounds(display.getBounds().width / 2, display
                    .getBounds().height / 2, 300, 200);
        }

        shell.pack();
        shell.open();

        text.setFocus();
        
        final boolean[] finished = new boolean[1];
        final Listener buttonListener = new Listener() {
            public void handleEvent(final Event event) {
                String key = text.getText();
                finished[0] = checkMasterKeys(key, confirm.getText());
                if (finished[0]) {
                    masterKey[0] = key;
                }
            }
        };

        button.addListener(SWT.Selection, buttonListener);

        KeyListener keyListener = new KeyListener() {

            public void keyPressed(final KeyEvent e) {
                if (e.character == '\r') {
                    buttonListener.handleEvent(null);
                }
            }

            public void keyReleased(final KeyEvent e) {
                // do nothing
            }
        };

        text.addKeyListener(keyListener);
        confirm.addKeyListener(keyListener);
        shell.addKeyListener(keyListener);

        Listener cancelListener = new Listener() {
            public void handleEvent(final Event event) {
                MessageBox mb = new MessageBox(Display.getDefault()
                     .getActiveShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
                mb.setText("Warning...");
                mb.setMessage(
                    "All passwords will be saved as plain text. Are you sure?");
                if (mb.open() == SWT.YES) {
                    masterKey[0] = null;
                    finished[0] = true;
                } else {
                    finished[0] = false;
                }
            }

        };

        cancel.addListener(SWT.Selection, cancelListener);

        while (!shell.isDisposed() && !finished[0]) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        if (!shell.isDisposed()) {
            shell.close();
            shell.dispose();
        }
        return masterKey[0];
    }

}
