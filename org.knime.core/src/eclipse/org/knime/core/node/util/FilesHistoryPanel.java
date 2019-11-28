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
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.FileUtil;

/**
 * Panel that contains an editable Combo Box showing the file to write to and a
 * button to trigger a file chooser. The elements in the combo are files that
 * have been recently used.
 *
 * @see org.knime.core.node.util.StringHistory
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public final class FilesHistoryPanel extends JPanel {
    private static final Duration DEFAULT_CONNECT_TIMEOUT =
        Duration.ofMillis(Integer.getInteger(KNIMEConstants.PROPERTY_URL_TIMEOUT, 1000));

    /**
     * Enum for whether and which location validation should be performed.
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     * @since 2.11
     */
    public static enum LocationValidation {
        /** No location validation. */
        None(NoCheckLabel.class),
        /** Validate file input locations. */
        FileInput(FileReaderCheckLabel.class),
        /** Validate directory input locations. */
        DirectoryInput(DirectoryReaderCheckLabel.class),
        /** Validate file output locations. */
        FileOutput(FileWriterCheckLabel.class),
        /** Validate directory output locations. */
        DirectoryOutput(DirectoryWriterCheckLabel.class);


        private final Class<? extends LocationCheckLabel> m_labelClass;

        private LocationValidation(final Class<? extends LocationCheckLabel> labelClass) {
            m_labelClass = labelClass;
        }

        /**
         * Creates a label for this location validation type.
         *
         * @return a location check label
         */
        public LocationCheckLabel createLabel() {
            try {
                return m_labelClass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                // won't happen
                NodeLogger.getLogger(LocationValidation.class).error(
                    "Could not create validation label: " + ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Abstract class for labels that do some basic checks on input/output locations.
     *
     * @since 2.11
     */
    public static abstract class LocationCheckLabel extends JLabel {
        private static final long serialVersionUID = 6692875443028625895L;
        /** <code>true</code> if remote URLs are allowed, <code>false</code> otherwise. */
        protected boolean m_allowRemoteURLs = true;

        /**
         * Color for error messages.
         */
        @SuppressWarnings("hiding")
        protected static final Color ERROR = Color.RED;

        /**
         * Color for warning messages.
         */
        protected static final Color WARNING = new Color(150, 120, 0);

        /**
         * Color for info messages.
         */
        protected static final Color INFO = new Color(64, 64, 255);

        /**
         * Checks a source or destination location.
         *
         * @param url URL of the location
         */
        public abstract void checkLocation(final URL url);

        /**
         * Sets if this file panel should allow remote URLs. In case they are not allowed and the user enters a non-local
         * URL an error message is shown. The default is to allow remote URLs
         *
         * @param b <code>true</code> if remote URLs are allowed, <code>false</code> otherwise
         */
        public void setAllowRemoteURLs(final boolean b) {
            m_allowRemoteURLs = b;
        }
    }

    /**
     * A label that checks the destination location and displays a warning or error message if certain required
     * conditions are not fulfilled (e.g. directory does not exist, file is not writable, etc.).
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     * @since 2.11
     */
    static class FileWriterCheckLabel extends LocationCheckLabel {
        private static final long serialVersionUID = -440167673631870065L;

        @Override
        public void checkLocation(final URL url) {
            try {
                Path path = FileUtil.resolveToPath(url);
                setText("");
                if (path != null) {
                    if (Files.exists(path)) {
                        if (Files.isDirectory(path)) {
                            setText("Error: output location is a directory");
                            setForeground(ERROR);
                        } else if (!Files.isWritable(path)
                            && !(FileUtil.looksLikeUNC(path) || FileUtil.isWindowsNetworkMount(path))) {
                            setText("Error: no write permission to output file");
                            setForeground(ERROR);
                        } else {
                            setText("Warning: output file exists");
                            setForeground(WARNING);
                        }
                    } else {
                        Path parent = path.getParent();
                        if ((parent == null) || !Files.exists(parent)) {
                            setText("Error: directory of output file does not exist");
                            setForeground(ERROR);
                        } else if (!Files.isWritable(path.getParent())
                            && !(FileUtil.looksLikeUNC(url) || FileUtil.isWindowsNetworkMount(path.getParent()))) {
                            setText("Error: no write permissions in directory");
                            setForeground(ERROR);
                        }
                    }
                } else if (!m_allowRemoteURLs) {
                    setText("Error: remote locations are not supported");
                    setForeground(ERROR);
                } else {
                    setText("Info: remote output file will be overwritten if it exists");
                    setForeground(INFO);
                }
            } catch (IOException | URISyntaxException | IllegalArgumentException ex) {
                // ignore it
            }
        }
    }

    /**
     * A label that checks the destination location and displays a warning or error message if certain required
     * conditions are not fulfilled (e.g. directory does not exist, file is not writable, etc.).
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     * @since 2.11
     */
    static class DirectoryWriterCheckLabel extends LocationCheckLabel {
        private static final long serialVersionUID = -440167673631870065L;

        @Override
        public void checkLocation(final URL url) {
            try {
                Path path = FileUtil.resolveToPath(url);
                if (path != null) {
                    // Java does not detect permission for UNC correctly
                    setText("");
                    if (Files.exists(path)) {
                        if (!Files.isDirectory(path)) {
                            setText("Error: output location is not a directory");
                            setForeground(ERROR);
                        } else if (!Files.isWritable(path)
                            && !(FileUtil.looksLikeUNC(path) || FileUtil.isWindowsNetworkMount(path))) {
                            setText("Error: no write permission to output directory");
                            setForeground(ERROR);
                        }
                    } else {
                        setText("Error: output directory does not exist");
                        setForeground(ERROR);
                    }
                } else if (!m_allowRemoteURLs) {
                    setText("Error: remote directories are not supported");
                    setForeground(ERROR);
                } else {
                    setText("Info: remote output directory will be overwritten if it exists");
                    setForeground(INFO);
                }
            } catch (IOException | URISyntaxException | InvalidPathException ex) {
                // ignore it
            }
        }
    }

    static class NoCheckLabel extends LocationCheckLabel {
        private static final long serialVersionUID = -440167673631870065L;

        @Override
        public void checkLocation(final URL url) {
            // do nothing
        }
    }


    /**
     * A label that checks the source location and displays a warning or error message if certain required
     * conditions are not fulfilled (e.g. file does not exist, file is not readable, etc.).
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     * @since 2.11
     */
    static class FileReaderCheckLabel extends LocationCheckLabel {
        private static final long serialVersionUID = -440167673631870065L;

        @Override
        public void checkLocation(final URL url) {
            try {
                Path path = FileUtil.resolveToPath(url);
                setText("");
                if (path != null) {
                    // Java does not detect permission for UNC correctly
                    if (Files.exists(path)) {
                        if (Files.isDirectory(path)) {
                            setText("Error: input location is a directory");
                            setForeground(ERROR);
                        } else if (!Files.isReadable(path)
                            && !(FileUtil.looksLikeUNC(path) || FileUtil.isWindowsNetworkMount(path))) {
                            setText("Error: no read permission on input file");
                            setForeground(ERROR);
                        }
                    } else {
                        setText("Error: Input file does not exist");
                        setForeground(ERROR);
                    }
                } else if (!m_allowRemoteURLs) {
                    setText("Error: remote locations are not supported");
                    setForeground(ERROR);
                }
            } catch (IOException | URISyntaxException | IllegalArgumentException ex) {
                // ignore it
            }
        }
    }


    /**
     * A label that checks the source location and displays a warning or error message if certain required
     * conditions are not fulfilled (e.g. directory does not exist, is not readable, etc.).
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     * @since 2.11
     */
    static class DirectoryReaderCheckLabel extends LocationCheckLabel {
        private static final long serialVersionUID = -440167673631870065L;

        @Override
        public void checkLocation(final URL url) {
            try {
                Path path = FileUtil.resolveToPath(url);
                if (path != null) {
                    setText("");
                    if (Files.exists(path)) {
                        if (!Files.isDirectory(path)) {
                            setText("Error: input location is not a directory");
                            setForeground(ERROR);
                        } else if (!Files.isReadable(path)
                            && !(FileUtil.looksLikeUNC(path) || FileUtil.isWindowsNetworkMount(path))) {
                            setText("Error: no read permission on input directory");
                            setForeground(ERROR);
                        }
                    } else {
                        setText("Error: input directory does not exist");
                        setForeground(ERROR);
                    }
                } else if (!m_allowRemoteURLs) {
                    setText("Error: remote directories are not supported");
                    setForeground(ERROR);
                }
            } catch (IOException | URISyntaxException | InvalidPathException ex) {
                // ignore it
            }
        }
    }

    private final List<ChangeListener> m_changeListener = new ArrayList<ChangeListener>();

    private final JComboBox<String> m_textBox;

    private final JButton m_chooseButton;

    private String[] m_suffixes;

    private final String m_historyID;

    private final LocationCheckLabel m_warnMsg;

    private final FlowVariableModelButton m_flowVariableButton;

    private int m_selectMode;

    private int m_dialogType = JFileChooser.OPEN_DIALOG;

    private String m_forcedFileExtensionOnSave = null;

    /** See {@link #setAllowSystemPropertySubstitution(boolean)}. */
    private boolean m_allowSystemPropertySubstitution = false;

    private final JSpinner m_connectTimeoutSpinner;

    private final JCheckBox m_userSetTimeoutCheckBox;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     *
     * @param historyID identifier for the string history, see
     *            {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     */
    public FilesHistoryPanel(final String historyID, final String... suffixes) {
        this(null, historyID, LocationValidation.None, suffixes);
    }


    /**
     * Creates new instance, sets properties, for instance renderer, accordingly.
     *
     * @param historyID identifier for the string history, see {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     * @param fvm model to allow to use a variable instead of the text field.
     * @param showErrorMessage if true there are error messages if the file exists or the path is not available and so
     *            on.
     * @deprecated use {@link #FilesHistoryPanel(FlowVariableModel, String, LocationValidation, String...)} instead in
     *             order to specify what kind of location validation to perform
     */
    @Deprecated
    public FilesHistoryPanel(final FlowVariableModel fvm,
            final String historyID, final boolean showErrorMessage,
            final String... suffixes) {
        this(fvm, historyID, showErrorMessage ? LocationValidation.FileInput : LocationValidation.None, suffixes);
    }


    /**
     * Creates new instance, sets properties, for instance renderer, accordingly.
     *
     * @param historyID identifier for the string history, see {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     * @param fvm model to allow to use a variable instead of the text field.
     * @param validation what kind of validation on the location should be performed
     * @since 2.11
     */
    public FilesHistoryPanel(final FlowVariableModel fvm, final String historyID, final LocationValidation validation,
        final String... suffixes) {
        if (historyID == null || suffixes == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        }
        if (Arrays.asList(suffixes).contains(null)) {
            throw new IllegalArgumentException("Array must not contain null.");
        }
        m_historyID = historyID;
        m_suffixes = suffixes;

        if ((validation == LocationValidation.DirectoryInput) || (validation == LocationValidation.DirectoryOutput)) {
            m_selectMode = JFileChooser.DIRECTORIES_ONLY;
        } else {
            m_selectMode = JFileChooser.FILES_AND_DIRECTORIES;
        }

        m_textBox = new JComboBox<String>(new DefaultComboBoxModel<String>());
        m_textBox.setEditable(true);
        m_textBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new ConvenientComboBoxRenderer());
        m_textBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fileLocationChanged();
                }
            }
        });

        /* install action listeners */
        m_textBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });
        Component editor = m_textBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }
            });
        }

        m_chooseButton = new JButton("Browse...");
        m_chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String newFile = getOutputFileName();
                if (newFile != null) {
                    m_textBox.setSelectedItem(newFile);
                    StringHistory.getInstance(m_historyID).add(newFile);
                    fileLocationChanged();
                }
            }
        });
        m_warnMsg = validation.createLabel();
        // this ensures correct display of the changing label content...
        m_warnMsg.setPreferredSize(new Dimension(350, 25));
        m_warnMsg.setMinimumSize(new Dimension(350, 25));
        m_warnMsg.setForeground(Color.red);

        m_connectTimeoutSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE / 1000, 1));
        m_connectTimeoutSpinner.setEnabled(false);
        m_userSetTimeoutCheckBox = new JCheckBox("Custom connection timeout [s]: ");
        m_userSetTimeoutCheckBox
            .addActionListener(e -> m_connectTimeoutSpinner.setEnabled(m_userSetTimeoutCheckBox.isSelected()));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 5, 0, 0);

        add(m_textBox, c);

        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 5, 0, 5);
        add(m_chooseButton, c);

        if (fvm != null) {
            m_flowVariableButton = new FlowVariableModelButton(fvm);

            c.gridx = 2;
            c.insets = new Insets(2, 0, 0, 5);
            add(m_flowVariableButton, c);
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    FlowVariableModel wvm = (FlowVariableModel)(evt.getSource());
                    boolean variableReplacementEnabled = wvm.isVariableReplacementEnabled();
                    m_textBox.setEnabled(!variableReplacementEnabled);
                    m_chooseButton.setEnabled(!variableReplacementEnabled);
                    if (variableReplacementEnabled) {
                        // if the location is overwritten by a variable show its value
                        wvm.getVariableValue().ifPresent(fv -> setSelectedFile(fv.getStringValue()));
                    }
                    fileLocationChanged();
                }
            });
            this.addAncestorListener(new AncestorListener() {

                @Override
                public void ancestorRemoved(final AncestorEvent event) {
                }

                @Override
                public void ancestorMoved(final AncestorEvent event) {
                }

                @Override
                public void ancestorAdded(final AncestorEvent event) {
                    if (fvm.isVariableReplacementEnabled() && fvm.getVariableValue().isPresent()) {
                        String newPath = fvm.getVariableValue().get().getStringValue();
                        String oldPath = getSelectedFile();
                        if ((newPath != null) && !newPath.equals(oldPath)) {
                            ViewUtils.invokeLaterInEDT(() -> {
                                setSelectedFile(newPath);
                                fileLocationChanged();
                            });
                        }
                    }
                }
            });
        } else {
            m_flowVariableButton = null;
        }

        if (validation != LocationValidation.None) {
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.insets = new Insets(0, 5, 0, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = (fvm == null) ? 2 : 3;
            add(m_warnMsg, c);
        }
        m_connectTimeoutSpinner.setPreferredSize(new Dimension(100, m_connectTimeoutSpinner.getPreferredSize().height));
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        JPanel timeoutPanel = new JPanel();
        timeoutPanel.add(m_userSetTimeoutCheckBox);
        timeoutPanel.add(m_connectTimeoutSpinner);
        timeoutPanel.setVisible(false);
        add(timeoutPanel, c);
        fileLocationChanged();
        updateHistory();
    }

    /**
     * Creates new instance, sets properties, for instance renderer, accordingly.
     *
     * @param historyID identifier for the string history, see {@link StringHistory}
     * @param showErrorMessage if true there are error messages if the file exists or the path is not available and so
     *            on.
     * @deprecated use {@link #FilesHistoryPanel(String, LocationValidation)} instead in order to specify what kind of
     *             location validation to perform
     */
    @Deprecated
    public FilesHistoryPanel(final String historyID,
            final boolean showErrorMessage) {
        this(null, historyID, showErrorMessage);
    }


    /**
     * Creates new instance, sets properties, for instance renderer, accordingly.
     *
     * @param historyID identifier for the string history, see {@link StringHistory}
     * @param validation what kind of validation on the location should be performed
     * @since 2.11
     */
    public FilesHistoryPanel(final String historyID, final LocationValidation validation) {
        this(null, historyID, validation);
    }


    private String getOutputFileName() {
        // file chooser triggered by choose button
        FileSystemBrowser browser;
        if (NodeContext.getContext() != null && NodeContext.getContext().getNodeContainer() != null) {
            //if an ordinary node context is available, use default file system browser
            browser = new LocalFileSystemBrowser();
        } else {
            browser = FileSystemBrowserExtension.collectFileSystemBrowsers().stream().filter(b -> b.isCompatible())
                .sorted((b1, b2) -> b1.getPriority() - b2.getPriority()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No applicable file system browser available"));
        }
        String selectedFile;
        if (m_allowSystemPropertySubstitution) {
            selectedFile = getSelectedFileWithPropertiesReplaced();
        } else {
            selectedFile = getSelectedFile();
        }
        return browser.openDialogAndGetSelectedFileName(FileSelectionMode.fromJFileChooserCode(m_selectMode),
            DialogType.fromJFileChooserCode(m_dialogType), this, m_forcedFileExtensionOnSave, selectedFile, m_suffixes);
    }

    /**
     * Set file file as part of the suffix.
     *
     * @param suffixes The new list of valid suffixes.
     */
    public void setSuffixes(final String... suffixes) {
        m_suffixes = suffixes;
    }

    /** @return the currently set list of file filter suffixes. */
    public String[] getSuffixes() {
        return m_suffixes;
    }

    /**
     * Get currently selected file.
     *
     * @return the current file url
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    public String getSelectedFile() {
        return ((JTextField) m_textBox.getEditor().getEditorComponent()).getText();
    }

    /**
     * Internal getter to resolve possible system properties as per
     * {@link #setAllowSystemPropertySubstitution(boolean)}.
     */
    private String getSelectedFileWithPropertiesReplaced() {
        String selectedFile = getSelectedFile();
        if (m_allowSystemPropertySubstitution) {
            selectedFile = StrSubstitutor.replaceSystemProperties(selectedFile);
        }
        return selectedFile;
    }

    /**
     * Set the file url as default.
     *
     * @param url the file to choose
     * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
     */
    public void setSelectedFile(final String url) {
        if (SwingUtilities.isEventDispatchThread()) {
            m_textBox.setSelectedItem(url);
        } else {
            ViewUtils.invokeAndWaitInEDT(new Runnable() {
                @Override
                public void run() {
                    m_textBox.setSelectedItem(url);
                }
            });
        }
    }

    /**
     * Returns the connect timeout.
     *
     * @return the connect timeout if set by the user or an empty optional
     * @since 3.4
     */
    public Optional<Duration> getConnectTimeout() {
        return m_userSetTimeoutCheckBox.isSelected()
            ? Optional.of(Duration.ofSeconds((int)m_connectTimeoutSpinner.getValue())) : Optional.empty();
    }

    /**
     * Sets the connect timeout in the dialog. Note the that currently the component will only show full seconds.
     *
     * @param timeout the timeout or <code>null</code> if the default timeout should be used
     * @since 3.4
     */
    public void setConnectTimeout(final Duration timeout) {
        final Duration timeoutToUse = timeout == null ? DEFAULT_CONNECT_TIMEOUT : timeout;
        if (SwingUtilities.isEventDispatchThread()) {
            m_connectTimeoutSpinner.setValue(timeoutToUse.getSeconds());
            if ((timeout != null) && !m_userSetTimeoutCheckBox.isSelected()) {
                m_userSetTimeoutCheckBox.doClick();
            }
        } else {
            ViewUtils.invokeAndWaitInEDT(new Runnable() {
                @Override
                public void run() {
                    m_connectTimeoutSpinner.setValue(timeoutToUse.getSeconds());
                    if ((timeout != null) && !m_userSetTimeoutCheckBox.isSelected()) {
                        m_userSetTimeoutCheckBox.doClick();
                    }
                }
            });
        }
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history = StringHistory.getInstance(m_historyID);
        String[] allVals = history.getHistory();
        LinkedHashSet<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < allVals.length; i++) {
            String cur = allVals[i];
            if (!cur.isEmpty()) {
                try {
                    URL url = new URL(cur);
                    list.add(url.toString());
                } catch (MalformedURLException mue) {
                    // ignore, it's probably not a URL
                    list.add(new File(cur).getAbsolutePath());
                }
            }
        }
        m_textBox.setModel(new DefaultComboBoxModel<String>(list.toArray(new String[list.size()])));
        // changing the model will also change the minimum size to be
        // quite big. We have tooltips, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /**
     * Adds a change listener that gets notified if a new file name is entered
     * into the text field.
     *
     * @param cl a change listener
     */
    public void addChangeListener(final ChangeListener cl) {
        m_changeListener.add(cl);
    }

    /**
     * Removes the given change listener from the listener list.
     *
     * @param cl a change listener
     */
    public void removeChangeListener(final ChangeListener cl) {
        m_changeListener.remove(cl);
    }

    /**
     * Sets the select mode for the file chooser dialog.
     *
     * @param mode one of {@link JFileChooser#FILES_ONLY},
     *            {@link JFileChooser#DIRECTORIES_ONLY}, or
     *            {@link JFileChooser#FILES_AND_DIRECTORIES}
     *
     * @see JFileChooser#setFileSelectionMode(int)
     */
    public void setSelectMode(final int mode) {
        m_selectMode = mode;
    }

    /**
     * Sets the type of brows dialog that is being opened.
     *
     * @param type one of {@link JFileChooser#OPEN_DIALOG} or {@link JFileChooser#SAVE_DIALOG}
     * @see JFileChooser#setDialogType(int)
     * @since 2.11
     */
    public void setDialogType(final int type) {
        m_dialogType = type;
        m_forcedFileExtensionOnSave = null;
    }

    /** Sets the dialog type to SAVE {@link JFileChooser#SAVE_DIALOG}, whereby it also forces the given file extension
     * when the user enters a path in the text field that does not end with either the argument extension or any
     * extension specified in {@link #setSuffixes(String...)} (ignoring case).
     * Calling this method will overwrite the parameter set in {@link #setDialogType(int)}.
     * @param forcedExtension optional parameter to force a file extension to be appended to the selected
     *        file name, e.g. ".txt" (null and blanks not force any extension).
     * @since 3.2
     * @see #setDialogType
     */
    public void setDialogTypeSaveWithExtension(final String forcedExtension) {
        setDialogType(JFileChooser.SAVE_DIALOG);
        m_forcedFileExtensionOnSave = StringUtils.defaultIfBlank(forcedExtension, null);
    }

    /**
     * Sets whether to show the connect timeout field in the panel.
     *
     * @param showTimeout
     * @since 3.4
     */
    public void setShowConnectTimeoutField(final boolean showTimeout){
        m_connectTimeoutSpinner.getParent().setVisible(showTimeout);
    }

    /**
     * Return a file object for the given fileName. It makes sure that if the
     * fileName is not absolute it will be relative to the user's home dir.
     *
     * @param fileOrUrl the file name to convert to a file
     * @return a file representing fileName
     * @deprecated use {@link FileUtil#resolveToPath(URL)} instead
     */
    @Deprecated
    public static final File getFile(final String fileOrUrl) {
        String path = fileOrUrl;
        try {
            URL u = new URL(fileOrUrl);
            if ("file".equals(u.getProtocol())) {
                path = u.getPath();
            }
        } catch (MalformedURLException ex) {
        }

        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(new File(System.getProperty("user.home")), path);
        }
        return f;
    }

    private void fileLocationChanged() {
        String selFile = getSelectedFileWithPropertiesReplaced();
        m_warnMsg.setText("");
        if (StringUtils.isNotEmpty(selFile)) {
            try {
                URL url = FileUtil.toURL(selFile);
                m_warnMsg.checkLocation(url);
            } catch (InvalidPathException ex) {
                m_warnMsg.setText("Invalid file system path: " + ex.getMessage());
                m_warnMsg.setForeground(Color.RED);
            } catch (IOException ex) {
                // ignore
            }
        }
        final ChangeEvent changeEvent = new ChangeEvent(this);
        m_changeListener.stream().forEach(c -> c.stateChanged(changeEvent));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        if (m_flowVariableButton != null) {
            boolean replacedByVariable = m_flowVariableButton.getFlowVariableModel().isVariableReplacementEnabled();

            m_chooseButton.setEnabled(enabled && !replacedByVariable);
            m_textBox.setEnabled(enabled && !replacedByVariable);
            m_flowVariableButton.setEnabled(enabled);
        } else {
            m_chooseButton.setEnabled(enabled);
            m_textBox.setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        m_textBox.setToolTipText(text);
        m_chooseButton.setToolTipText(text);
    }

    private final ChangeListener m_checkIfExistsListener =
            new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    // we can only check local files, ignore everything else
                    Component editorComponent =
                            m_textBox.getEditor().getEditorComponent();
                    String selFile = getSelectedFileWithPropertiesReplaced();
                    if (new File(selFile).exists()) {
                        editorComponent.setBackground(Color.WHITE);
                        return;
                    }

                    if (selFile.startsWith("file:")) {
                        selFile = selFile.substring(5);
                        File file = new File(selFile);
                        if (file.exists()) {
                            editorComponent.setBackground(Color.WHITE);
                            return;
                        } else {
                            // maybe the URL is encoded (e.g. %20 for
                            // spaces)
                            // so try to decode it
                            try {
                                file = new File(URIUtil.decode(selFile, "UTF-8"));
                                if (file.exists()) {
                                    editorComponent.setBackground(Color.WHITE);
                                    return;
                                }
                            } catch (URIException ex) {
                                // ignore it
                            }
                        }
                    } else {
                        editorComponent.setBackground(Color.WHITE);
                        return;
                    }

                    editorComponent.setBackground(Color.ORANGE);
                }
            };

    /**
     * Sets if the text field should be colored if the selected file does not
     * exist.
     *
     * @param b <code>true</code> if the text field should be colored,
     *            <code>false</code> otherwise
     */
    public void setMarkIfNonexisting(final boolean b) {
        if (b) {
            addChangeListener(m_checkIfExistsListener);
        } else {
            removeChangeListener(m_checkIfExistsListener);
        }
    }

    /**
     * Sets if this file panel should allow remote URLs. In case they are not allowed and the user enters a non-local
     * URL an error message is shown. The default is to allow remote URLs
     *
     * @param b <code>true</code> if remote URLs are allowed, <code>false</code> otherwise
     * @since 2.11
     */
    public void setAllowRemoteURLs(final boolean b) {
        m_warnMsg.setAllowRemoteURLs(b);
    }

    /**
     * If set to <code>true</code>, the path may contain system properties such as "${user.home}/file.txt". The file
     * existence checks as per {@link #setMarkIfNonexisting(boolean)} will then resolve the full path. The
     * {@link #getSelectedFile()} method will return the path as entered by the user (incl. placeholders) so the caller
     * needs to resolve them also (see for instance {@link org.apache.commons.lang3.text.StrSubstitutor}).
     *
     * <p>This method should be called right after construction.
     *
     * @param b Whether to allow system properties in the string (default is <code>false</code>)
     * @since 3.5
     */
    public void setAllowSystemPropertySubstitution(final boolean b) {
        m_allowSystemPropertySubstitution = b;
        fileLocationChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        m_textBox.getEditor().getEditorComponent().requestFocus();
    }

    /**
     * Adds the current location to the history.
     * @since 2.11
     */
    public void addToHistory() {
        StringHistory.getInstance(m_historyID).add(getSelectedFile());
    }

    /**
     * @return true if the value is replaced by a variable.
     * @since 3.3
     */
    public boolean isVariableReplacementEnabled(){
        if(m_flowVariableButton == null){
            return false;
        }
        return m_flowVariableButton.getFlowVariableModel().isVariableReplacementEnabled();
    }
}
