/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   03.07.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeView;

/**
 * This node view of "One-Sample T-Test" Node.
 *
 * @author Heiko Hofer
 */
public class OneSampleTTestNodeView extends NodeView<OneSampleTTestNodeModel> {

    /** The text pane that holds the information. */
    private JEditorPane m_headerPane;
    private JEditorPane m_descrStatPane;
    private JEditorPane m_statPane;

    /**
     * New instance.
     *
     * @param model the model to look at
     */
    public OneSampleTTestNodeView(final OneSampleTTestNodeModel model) {
        super(model);

        // create content
        JPanel p = createMainPanel();
        p.setBackground(Color.white);
        JScrollPane scroller = new JScrollPane(p);
        scroller.setBackground(Color.white);
        setComponent(scroller);
    }

    /** The content of the view. */
    private JPanel createMainPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = createGridBagConstraints();
        c.gridwidth = 2;
        m_headerPane = new JEditorPane("text/html", "");
        m_headerPane.setEditable(false);
        p.add(m_headerPane, c);
        c.gridwidth = 1;

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        m_descrStatPane = new JEditorPane("text/html", "");
        m_descrStatPane.setEditable(false);
        p.add(m_descrStatPane, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        m_statPane = new JEditorPane("text/html", "");
        m_statPane.setEditable(false);
        p.add(m_statPane, c);

        c.gridy++;
        c.weighty = 1;
        JPanel foo = new JPanel();
        foo.setBackground(Color.white);
        p.add(foo, c);

        return p;
    }

    /** Convenient method to create GridBagConstraints. */
    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 3, 3, 3);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        return c;
    }


    /** Update the display of the header. */
    private void updateHeader() {
        m_headerPane.setText(renderHeader());
        m_headerPane.revalidate();
    }

    /** Update the display of the statistics. */
    private void updateStatistics() {
        m_descrStatPane.setText(renderDescriptiveStatistics());
        m_descrStatPane.revalidate();
        m_statPane.setText(renderTestStatistics());
        m_statPane.revalidate();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if (getNodeModel().getDescritiveStatistics() == null) {
            m_headerPane.setText("");
            m_descrStatPane.setText("");
            m_statPane.setText("");
        } else {
            updateHeader();
            updateStatistics();
        }
    }


    /** Create HTML of the header. */
    private String renderHeader() {
        StringBuilder buffer = NodeViewUtil.createHtmlHeader();
        buffer.append("<body>\n");
        buffer.append("<h2>Single Sample T-Test ");
        buffer.append("</h2>");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }

    /** Create HTML for the descriptive statistics. */
    private String renderDescriptiveStatistics() {
        StringBuilder buffer = NodeViewUtil.createHtmlHeader();
        buffer.append("<body>\n");

        buffer.append("<h3>Descriptive Statistics</h3>");
        NodeViewUtil.renderDataTable(getNodeModel().getDescritiveStatistics(),
                OneSampleTTestStatistics.COLUMN,
                Collections.singleton(OneSampleTTestStatistics.COLUMN),
                new HashMap<String, String>(),
                buffer);

        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }

    /** Create HTML for the test statistics. */
    private String renderTestStatistics() {
        StringBuilder buffer = NodeViewUtil.createHtmlHeader();
        buffer.append("<body>\n");

        buffer.append("<h3>Single Sample Test</h3>");

        buffer.append("<p>");
        buffer.append("Confidence Interval (CI) Probability: ");
        buffer.append(
                getNodeModel().getSettings().getConfidenceIntervalProb() * 100);
        buffer.append("%");
        buffer.append("</p>");
        Set<String> exclude = new HashSet<String>();
        exclude.add(OneSampleTTestStatistics.LABEL);
        exclude.add(OneSampleTTestStatistics.CONFIDENCE_INTERVAL_PROBABILITY);
        Map<String, String> colNames = new HashMap<String, String>();
        colNames.put(OneSampleTTestStatistics.CONFIDENCE_INTERVAL_LOWER_BOUND ,
                "CI (Lower Bound)");
        colNames.put(OneSampleTTestStatistics.CONFIDENCE_INTERVAL_UPPER_BOUND ,
                "CI (Upper Bound)");
        NodeViewUtil.renderDataTable(getNodeModel().getTestStatistics(),
                OneSampleTTestStatistics.LABEL,
                exclude, colNames, buffer);

        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing.
    }

}

