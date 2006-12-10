/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Dez 3, 2006 (sieb): created
 */
package org.knime.workbench.ui.initialupdatesite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.KNIMEConstants;
import org.knime.workbench.repository.ImageRepository;

/**
 * This shell is intended to be shown up the first time a user starts KNIME. The
 * user can proceed with the update of KNIME or other components or the user can
 * cancel it. Additionaly there is a check box to determine whether the shell is
 * shown the next time KNIME is started.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class InitialUpdateSiteIntroShell {

    private static final String NEXT_TIME_CHECK_FILE_NAME = "CheckNextTime";

    private static final String VALUE_KEY = "checkNextTime";

    /**
     * This method opens a window where the user can decide whether to open the
     * update site or to proceed. The selection is returned as a boolean value
     * 
     * @return true if the update site should be opened
     */
    public boolean open() {

        final UpdateSiteSelectionShell updateSiteSelectionShell =
                new UpdateSiteSelectionShell("Open the update site",
                        "The KNIME Workbench allows you to enhance the "
                                + "functionality of KNIME and other "
                                + "Functionality of Eclipse.\n"
                                + "If you press OK the Eclipse"
                                + " Upate Wizard opens, that giudes you "
                                + "through the update process.\n"
                                + "1. Just select 'Search for new features to "
                                + "install'\n" + "2. Press 'Next'\n"
                                + "3. Select the KNIME update site\n"
                                + "4. Press 'Finish'\n"
                                + "5. Select the new features and install "
                                + "them");

        return updateSiteSelectionShell.update();

    }

    private final class UpdateSiteSelectionShell {

        private String m_title;

        private String m_text;

        private boolean m_finished;

        private boolean m_update;

        private UpdateSiteSelectionShell(final String title, final String text) {
            m_title = title;
            m_text = text;

        }

        private boolean update() {
            Display display = Display.getDefault();
//            try {
//                display = new Display();
//            } catch (Throwable t) {
//                display = Display.getDefault();
//            }

            Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);

             try {
                shell.setImage(ImageRepository.getImageDescriptor(
                        "icons/knime.png").createImage());
            } catch (Throwable e) {
                // do nothing, is just the icon
            }
            shell.setText(m_title);
            shell.setSize(600, 400);
            shell.forceActive();
            shell.forceFocus();

            GridLayout gridLayout = new GridLayout();

            gridLayout.numColumns = 2;

            shell.setLayout(gridLayout);

            GridData gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;

            final Label label = new Label(shell, SWT.NONE);
            label.setText(m_text + "\n\n");
            label.setBounds(20, 15, 300, 100);
            label.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;
            final Label spaceLabel = new Label(shell, SWT.NONE);
            spaceLabel.setText("\n");
            spaceLabel.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            gridData.widthHint = 80;
            gridData.horizontalSpan = 1;
            final Button okButton = new Button(shell, SWT.PUSH);
            okButton.setText("OK");
            // button.setBounds(20, 270, 180, 20);
            okButton.setSize(180, 20);
            okButton.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            gridData.widthHint = 80;
            gridData.horizontalSpan = 1;
            final Button cancelButton = new Button(shell, SWT.PUSH);
            cancelButton.setText("Cancel");
            // cancel.setBounds(20, 270, 180, 20);
            cancelButton.setSize(180, 20);
            cancelButton.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;

            final Label info = new Label(shell, SWT.NONE);
            info.setText("");
            // label.setBounds(20, 15, 380, 260);
            info.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 1;
            final Label keyLable = new Label(shell, SWT.NONE);
            keyLable.setText("Show at next startup:");
            keyLable.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
            gridData.horizontalSpan = 1;
            gridData.widthHint = 280;
            final Button nextTimeCheck = new Button(shell, SWT.CHECK);
            // text.setBounds(140, 270, 200, 20);
            // text.setSize(200, 20);
            nextTimeCheck.setLayoutData(gridData);
            nextTimeCheck.setSelection(true);

            Composite parent = shell.getParent();
            if (parent != null) {
                shell.setBounds(parent.getBounds().width / 2, parent
                        .getBounds().height / 2, 550, 255);
            } else {
                shell.setBounds(display.getBounds().width / 2, display
                        .getBounds().height / 2, 550, 255);
            }

            okButton.setFocus();

            // Listener listener = new Listener() {
            // public void handleEvent(final Event event) {
            // if (event.widget == buttonOK) {
            //
            // } else {
            //
            // }
            // dialog.close();
            // }
            // };

            final Listener buttonListener = new Listener() {
                public void handleEvent(final Event event) {

                    m_update = true;
                    m_finished = true;
                }

            };

            okButton.addListener(SWT.Selection, buttonListener);

            // KeyListener keyListener = new KeyListener() {
            //
            // public void keyPressed(final KeyEvent e) {
            //
            // if (e.character == '\r') {
            // buttonListener.handleEvent(null);
            // }
            //
            // }
            //
            // public void keyReleased(final KeyEvent e) {
            // // do nothing
            //
            // }
            // };

            // text.addKeyListener(keyListener);
            // shell.addKeyListener(keyListener);

            Listener cancelListener = new Listener() {
                public void handleEvent(final Event event) {

                    m_update = false;
                    m_finished = true;
                }

            };

            cancelButton.addListener(SWT.Selection, cancelListener);

            shell.open();

            while (!shell.isDisposed() && !m_finished) {
                if (!display.readAndDispatch())
                    display.sleep();
            }

            // remember the decission whether this shell should be opened next
            // time
            saveNextTimeShowup(nextTimeCheck.getSelection());
            shell.close();
            shell.dispose();

            return m_update;
        }
    }

    private void saveNextTimeShowup(final boolean checkNextTime) {

        try {
            String knimeHomePath = KNIMEConstants.getKNIMEHomeDir();

            File checkFile =
                    new File(knimeHomePath + "//" + NEXT_TIME_CHECK_FILE_NAME);
            if (checkFile.exists()) {
                checkFile.delete();
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(checkFile));

            bw.write(VALUE_KEY + "=" + checkNextTime);
            bw.close();
        } catch (Throwable t) {
            // do nothing
        }

    }

    public static boolean loadNextTimeShowup() {

        try {
            String knimeHomePath = KNIMEConstants.getKNIMEHomeDir();

            File checkFile =
                    new File(knimeHomePath + "//" + NEXT_TIME_CHECK_FILE_NAME);
            BufferedReader br = new BufferedReader(new FileReader(checkFile));
            String line = null;
            while ((line = br.readLine()) != null) {
                try {
                    String[] seperated = line.split("=");

                    if (seperated.length != 2) {
                        continue;
                    }

                    if (!seperated[0].trim().equals(VALUE_KEY)) {
                        continue;
                    }

                    if (seperated[1].trim().equals("false")) {
                        return false;
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
            br.close();
        } catch (Throwable t) {
            // do nothing
        }

        return true;
    }

    public static void main(String[] args) {
        InitialUpdateSiteIntroShell supplier =
                new InitialUpdateSiteIntroShell();
        boolean key = supplier.open();
        System.out.println("update?: " + key);
    }
}
