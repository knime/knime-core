/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   Nov 3, 2006 (sieb): created
 */
package org.knime.workbench.editor2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.knime.core.util.EncryptionKeySupplier;

/**
 * This class implements a decryption key supplier that is registered at the
 * {@link org.knime.core.util.KnimeEncryption} static class for encryption
 * purpose. The class asks the user to input a key to encrypt and decrypt
 * passwords, keys, etc. If a decryption should take place the key given by the
 * user must be the same that was used for encryption.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class EclipseEncryptionKeySupplier2 implements EncryptionKeySupplier {

    private String m_pw;

    private boolean m_returnNow;

    /**
     * This method opens a window to which the user can input the encryption
     * key. The key is then returned to the invoker (which is normaly the
     * {@link org.knime.core.util.KnimeEncryption} static class).
     * 
     * @see org.knime.core.util.EncryptionKeySupplier#getEncryptionKey()
     */
    public String getEncryptionKey() {

        final KeyReaderShell keyReaderShell =
                new KeyReaderShell("KNIME encryption key",
                        "An encryption/decryption key is needed but "
                                + "not available for this session.\nPlease "
                                + "type in your encryption key.\nNote: "
                                + "Previously encrypted strings can only be "
                                + "decrypted with the former key.\n"
                                + "The key must be at least 8 "
                                + "characters long.");
        final Object wait = new Object();
        m_returnNow = false;
        new Thread() {

            public void run() {
                m_pw = keyReaderShell.readPW();
                m_returnNow = true;
                wait.notify();
            }
        }.start();

        synchronized (wait) {
            try {
                if (!m_returnNow) {
                    wait.wait();
                }
            } catch (Exception e) {
            }
        }
        return m_pw;
    }

    private final class KeyReaderShell {

        private String m_title;

        private String m_text;

        private String m_key;

        private KeyReaderShell(final String title, final String text) {
            m_title = title;
            m_text = text;

        }

        private String readPW() {

            final JFrame shell = new JFrame(m_title);

            final Object wait = new Object();

            try {
                UIManager.setLookAndFeel(UIManager
                        .getSystemLookAndFeelClassName());
            } catch (Exception e) {

            }

            try {
                // shell.setImage(ImageRepository.getImageDescriptor(
                // "icons/knime.bmp").createImage());
            } catch (Throwable e) {
                // do nothing, is just the icon
            }
            shell.setSize(320, 280);

            GridBagLayout gridLayout = new GridBagLayout();
            shell.setLayout(gridLayout);

            GridBagConstraints gridData = new GridBagConstraints();
            gridData.fill = GridBagConstraints.HORIZONTAL;
            gridData.gridx = 0;
            gridData.gridy = 0;
            gridData.gridwidth = 2;
            final JTextArea label = new JTextArea(m_text + "\n\n");
            label.setLineWrap(true);
            label.setWrapStyleWord(true);
            label.setEditable(false);
            label.setBounds(20, 15, 320, 300);
            shell.add(label, gridData);
            // label.setLayoutData(gridData);

            gridData.gridwidth = 1;
            gridData.gridx = 0;
            gridData.gridy = 1;
            final JLabel keyLable = new JLabel("Encryption key:");
            // keyLable.setLayoutData(gridData);
            shell.add(keyLable, gridData);

            // gridData.horizontalAlignment =
            // GridData.HORIZONTAL_ALIGN_BEGINNING;
            gridData.gridwidth = 1;
            gridData.gridx = 1;
            gridData.gridy = 1;
            // gridData.widthHint = 280;
            final JPasswordField text = new JPasswordField();
            // text.setBounds(140, 270, 200, 20);
            text.setSize(200, 20);
            shell.add(text, gridData);
            // text.setLayoutData(gridData);

            gridData.fill = GridBagConstraints.HORIZONTAL;
            gridData.gridx = 0;
            gridData.gridy = 2;
            gridData.gridwidth = 2;
            final JLabel spaceLabel = new JLabel("\n");
            // spaceLabel.setLayoutData(gridData);
            shell.add(spaceLabel, gridData);

            // gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            // gridData. = 80;
            gridData.gridwidth = 1;
            gridData.gridx = 0;
            gridData.gridy = 3;
            final JButton okButton = new JButton();
            okButton.setText("OK");
            // button.setBounds(20, 270, 180, 20);
            okButton.setSize(180, 20);
            // button.setLayoutData(gridData);
            shell.add(okButton, gridData);

            gridData.fill = GridBagConstraints.HORIZONTAL;
            // gridData.widthHint = 80;
            gridData.gridwidth = 1;
            gridData.gridx = 1;
            gridData.gridy = 3;
            final JButton cancelButton = new JButton();
            cancelButton.setText("Cancel");
            // cancel.setBounds(20, 270, 180, 20);
            cancelButton.setSize(180, 20);
            // cancel.setLayoutData(gridData);
            shell.add(cancelButton, gridData);

            gridData.fill = GridBagConstraints.HORIZONTAL;
            gridData.gridwidth = 2;
            gridData.gridx = 0;
            gridData.gridy = 4;
            final JLabel info = new JLabel();
            info.setText("");
            // label.setBounds(20, 15, 380, 260);
            // info.setLayoutData(gridData);
            shell.add(info, gridData);

            shell.setVisible(true);
            shell.setAlwaysOnTop(true);

            final ActionListener okButtonListener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    // check if the key is valid
                    if (text.getText().length() < 8) {
                        info.setText("Key must be at least 8 characters long.");
                        return;
                    }
                    if (text.getText().indexOf(" ") >= 0) {
                        info.setText("Key must not contain a blank.");
                        return;
                    }

                    m_key = text.getText();
                    shell.setVisible(false);

                    synchronized (wait) {
                        wait.notify();
                    }
                }
            };

            okButton.addActionListener(okButtonListener);

            KeyListener keyListener = new KeyListener() {

                public void keyTyped(KeyEvent e) {
                    // TODO Auto-generated method stub

                }

                public void keyPressed(final KeyEvent e) {

                    if (e.getKeyChar() == '\n') {
                        okButtonListener.actionPerformed(null);
                    }

                }

                public void keyReleased(final KeyEvent e) {
                    // do nothing

                }
            };

            text.addKeyListener(keyListener);
            shell.addKeyListener(keyListener);

            ActionListener cancelListener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    shell.setVisible(false);
                    synchronized (wait) {
                        wait.notify();
                    }
                }
            };

            cancelButton.addActionListener(cancelListener);

            synchronized (wait) {
                try {
                    wait.wait();
                } catch (Exception e) {

                }
            }
            return m_key;
        }
    }

    public static void main(String[] args) {
        EclipseEncryptionKeySupplier2 supplier =
                new EclipseEncryptionKeySupplier2();
        String key = supplier.getEncryptionKey();
        System.out.println("Key: " + key);
    }
}
