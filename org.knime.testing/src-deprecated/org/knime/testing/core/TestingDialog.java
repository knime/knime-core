/*
 * ------------------------------------------------------------------
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
 *   03.08.2007 (ohl): created
 */
package org.knime.testing.core;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.FilelistAccessory;

/**
 *
 * @author ohl, University of Konstanz
 */
@Deprecated
class TestingDialog extends JDialog {

    private static final String PATTERN_HISTORY = "TESTING_PATTERN";

    private static final String ROOTDIR_HISTORY = "TESTING_ROOTDIR";

    private static final String OUTDIR_HISTORY = "TESTING_OUTDIR";

    private boolean m_closedViaOK = false;

    private String m_testRootDir = null;

    private String m_testNamePattern = null;

    private boolean m_analyze = false;

    private String m_analOutdir = null;

    private final JComboBox m_pattern = new JComboBox();

    private final JComboBox m_rootDir = new JComboBox();

    private final JCheckBox m_anal =
            new JCheckBox("Analyze log file after run");

    private final JCheckBox m_testDialogs =
        new JCheckBox("Test dialogs (load and save settings)");

    private final JCheckBox m_testViews =
        new JCheckBox("Test views (open all view prior to execution)");


    private final JCheckBox m_saveTests =
        new JCheckBox("Save tests after execution in runtime workspace");

    private final JComboBox m_analout = new JComboBox();

    /**
     * Creates a modal dialog asking for user parameters. Set it visible to open
     * it. It returns after OK or Cancel was clicked.
     *
     * @param parent the parent frame this should be modal to.
     * @param pattern default value for test name pattern
     * @param rootDir starting dir for the dir selection
     * @param analyze default value for analyze checkbox
     * @param analOutDir default value for output dir
     */
    TestingDialog(final Frame parent, final String pattern, final File rootDir,
            final boolean analyze, final String analOutDir) {
        super(parent);
        m_closedViaOK = false;
        setModal(true);

        initialize();

        // set default values
        if (rootDir != null) {
            if (rootDir.isFile()) {
                m_rootDir.setSelectedItem(rootDir.getParent());
            } else {
                m_rootDir.setSelectedItem(rootDir.getAbsolutePath());
            }
        }
        m_pattern.setSelectedItem(pattern);
        m_anal.setSelected(analyze);
        m_analout.setSelectedItem(analOutDir);

        analChanged(); // update enable status
    }

    /**
     * This method initializes this.
     */
    private void initialize() {
        this.setSize(520, 420);
        this.setTitle("KNIME Testing");
        this.setContentPane(getPanel());
    }

    private JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // the box with the textfield for the root dir
        panel.add(getRootDirBox());
        // the box where the test name pattern is being entered.
        panel.add(getPatternBox());
        // the box for the analyze log file choices
        panel.add(getAnalBox());
        // make it look better when resized
        panel.add(Box.createVerticalGlue());
        panel.add(Box.createVerticalGlue());
        // the box with the OK and Cancel button
        panel.add(getControlBox());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private Component getRootDirBox() {

        m_rootDir.setEditable(true);
        m_rootDir.setRenderer(new ConvenientComboBoxRenderer());
        m_rootDir.setPreferredSize(new Dimension(350, 25));
        m_rootDir.setMinimumSize(new Dimension(350, 25));
        m_rootDir.setMaximumSize(new Dimension(350, 25));
        // fill it from the history
        m_rootDir.setModel(new DefaultComboBoxModel(StringHistory.getInstance(
                ROOTDIR_HISTORY).getHistory()));

        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String newDir = browse((String)m_rootDir.getSelectedItem());
                if (newDir != null) {
                    m_rootDir.setSelectedItem(newDir);
                }
            }
        });
        Box rootdirBox = Box.createVerticalBox();
        rootdirBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Test location:"));

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel("Enter path to root directory of tests:"));
        labelBox.add(Box.createHorizontalGlue());
        rootdirBox.add(labelBox);

        Box editBox = Box.createHorizontalBox();
        editBox.add(Box.createHorizontalStrut(50));
        editBox.add(m_rootDir);
        editBox.add(Box.createHorizontalStrut(7));
        editBox.add(browse);
        editBox.add(Box.createHorizontalGlue());
        rootdirBox.add(editBox);

        rootdirBox.add(Box.createVerticalStrut(20));
        return rootdirBox;
    }

    private String browse(final String startDir) {
        String startingDir = startDir;
        if ((startingDir == null) || (startingDir.length() == 0)) {
            startingDir = System.getProperty("user.home");
        }

        JFileChooser chooser = new JFileChooser(startingDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAccessory(new FilelistAccessory(chooser));
        int retVal = chooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;

    }

    private Component getPatternBox() {

        m_pattern.setEditable(true);
        m_pattern.setRenderer(new ConvenientComboBoxRenderer());
        m_pattern.setPreferredSize(new Dimension(200, 25));
        m_pattern.setMinimumSize(new Dimension(200, 25));
        m_pattern.setMaximumSize(new Dimension(200, 25));
        // fill it from the history
        m_pattern.setModel(new DefaultComboBoxModel(StringHistory.getInstance(
                PATTERN_HISTORY).getHistory()));

        Box patternBox = Box.createVerticalBox();
        patternBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Test selection:"));

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel("Enter name (regular expression) of "
                + "testcase(s) to run:"));
        labelBox.add(Box.createHorizontalGlue());
        patternBox.add(labelBox);

        Box editBox = Box.createHorizontalBox();
        editBox.add(Box.createHorizontalStrut(50));
        editBox.add(m_pattern);
        editBox.add(Box.createHorizontalGlue());
        patternBox.add(editBox);

        labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(55));
        labelBox.add(new JLabel("(Empty input runs all.)"));
        labelBox.add(Box.createHorizontalGlue());
        patternBox.add(labelBox);

        return patternBox;
    }

    private Component getAnalBox() {

        m_analout.setEditable(true);
        m_analout.setRenderer(new ConvenientComboBoxRenderer());
        m_analout.setPreferredSize(new Dimension(300, 25));
        m_analout.setMinimumSize(new Dimension(300, 25));
        m_analout.setMaximumSize(new Dimension(300, 25));
        // fill it from the history
        m_analout.setModel(new DefaultComboBoxModel(StringHistory.getInstance(
                OUTDIR_HISTORY).getHistory()));

        Box analBox = Box.createVerticalBox();
        analBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Log file analysis:"));

        Box checkBox = Box.createHorizontalBox();
        m_anal.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                analChanged();
            }
        });
        analChanged(); // update en/disabled status now
        checkBox.add(m_anal);
        checkBox.add(Box.createHorizontalGlue());
        analBox.add(checkBox);

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(30));
        labelBox.add(new JLabel("Enter directory for analysis results:"));
        labelBox.add(Box.createHorizontalGlue());
        analBox.add(labelBox);

        Box editBox = Box.createHorizontalBox();
        editBox.add(Box.createHorizontalStrut(30));
        editBox.add(m_analout);
        editBox.add(Box.createHorizontalGlue());
        analBox.add(editBox);

        labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(30));
        labelBox.add(new JLabel("(leave emtpy to use Java temp dir)"));
        labelBox.add(Box.createHorizontalGlue());
        analBox.add(labelBox);

        Box saveBox = Box.createHorizontalBox();
        saveBox.add(m_saveTests);
        saveBox.add(Box.createHorizontalGlue());
        analBox.add(saveBox);

        Box dialogBox = Box.createHorizontalBox();
        dialogBox.add(m_testDialogs);
        dialogBox.add(Box.createHorizontalGlue());
        analBox.add(dialogBox);

        Box viewBox = Box.createHorizontalBox();
        viewBox.add(m_testViews);
        viewBox.add(Box.createHorizontalGlue());
        analBox.add(viewBox);

        return analBox;
    }

    private Component getControlBox() {

        Dimension bSize = new Dimension(100, 25);

        Box controlBox = Box.createVerticalBox();

        controlBox.add(Box.createVerticalStrut(30));

        Box buttonBox = Box.createHorizontalBox();
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // store the values in our members
                m_testNamePattern = (String)m_pattern.getSelectedItem();
                if (m_testNamePattern == null) {
                    m_testNamePattern = "";
                }
                m_analyze = m_anal.isSelected();
                m_analOutdir = (String)m_analout.getSelectedItem();
                if (m_analOutdir == null) {
                    m_analOutdir = "";
                }
                m_testRootDir = (String)m_rootDir.getSelectedItem();
                if (m_testRootDir == null) {
                    m_testRootDir = "";
                }
                // update the histories
                if ((m_pattern.getSelectedIndex() < 0)
                        && (m_testNamePattern.length() > 0)) {
                    StringHistory.getInstance(PATTERN_HISTORY).add(
                            m_testNamePattern);
                }
                if ((m_analout.getSelectedIndex() < 0)
                        && (m_analOutdir.length() > 0)) {
                    StringHistory.getInstance(OUTDIR_HISTORY).add(m_analOutdir);
                }
                if ((m_rootDir.getSelectedIndex() < 0)
                        && (m_testRootDir.length() > 0)) {
                    StringHistory.getInstance(ROOTDIR_HISTORY).add(
                            m_testRootDir);
                }
                m_closedViaOK = true;
                setVisible(false);
            }
        });
        ok.setPreferredSize(bSize);
        ok.setMinimumSize(bSize);
        ok.setMaximumSize(bSize);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_closedViaOK = false;
                setVisible(false);
            }
        });
        cancel.setPreferredSize(bSize);
        cancel.setMinimumSize(bSize);
        cancel.setMaximumSize(bSize);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(ok);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(cancel);
        buttonBox.add(Box.createVerticalStrut(40));

        controlBox.add(buttonBox);

        return controlBox;

    }

    /**
     * updates the enabled/disabled status of the components depending on the
     * check mark of the "analyze log file" box.
     */
    private void analChanged() {
        m_analout.setEnabled(m_anal.isSelected());
    }

    /**
     * After the dialog returned from setVisible(true) this methods tells,
     * whether the dialog was canceled or ok'ed.
     *
     * @return true if the user pressed OK to close the dialog
     */
    boolean closedViaOK() {
        return m_closedViaOK;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * value entered as test name regular expression matching pattern.
     *
     * @return the new regular expression to match test names against.
     */
    String getTestRootDir() {
        return m_testRootDir;
    }

    /**
     * Returns if dialogs for all nodes in the workflow should be tested, i.e. load settings and save settings.
     *
     * @return <code>true</code> if dialogs should be tested, <code>false</code> otherwise
     */
    boolean getTestDialogs() {
        return m_testDialogs.isSelected();
    }

    /**
     * Returns if all views should be opened prior to running the workflow.
     *
     * @return <code>true</code> if views should be opened, <code>false</code> otherwise
     */
    boolean getTestViews() {
        return m_testViews.isSelected();
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * value entered as test name regular expression matching pattern.
     *
     * @return the new regular expression to match test names against.
     */
    String getTestNamePattern() {
        return m_testNamePattern;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * user's choice whether to analyze the log file or not.
     *
     * @return true if the log file should be analyzed.
     */
    boolean getAnalyzeLogFile() {
        return m_analyze;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * value entered as output dir for the log file analysis result dir.
     *
     * @return the new dir to store the analysis results in
     */
    String getAnalysisOutputDir() {
        return m_analOutdir;
    }

    boolean getSaveTests() {
        return m_saveTests.isSelected();
    }

    /**
     * The main.
     *
     * @param args the args of main.
     */
    public static void main(final String[] args) {
        TestingDialog dlg = new TestingDialog(null, "foo", null, true, null);
        dlg.setVisible(true);
        dlg.dispose();
        if (!dlg.closedViaOK()) {
            System.out.println("Canceled.");
            return;
        }
        System.out.println("TestPattern: '" + dlg.getTestNamePattern() + "'");
        System.out.println("Analyze Log File?: "
                + (dlg.getAnalyzeLogFile() ? "yes, please." : "no thanks."));
        System.out.println("Anal Outdir: '" + dlg.getAnalysisOutputDir() + "'");
    }

}
