/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Jun 11, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableView;

/**
 * This panel is the view to a EntropyCalculator. SplitPane that shows on top
 * some basic clustering statistics and at the bottom the clusters, connnected
 * to a hilite handler.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class EntropyView extends JSplitPane {
    /** The table view containing cluster statistics. */
    private final TableView m_tableView;

    /** The text pane that holds the information. */
    private final JEditorPane m_editorPane;
    /** Scrollpane containing m_editorPane. */
    private final JScrollPane m_editorPaneScroller;
    
    /** Constructs new panel. */
    public EntropyView() {
        super(VERTICAL_SPLIT);
        m_editorPane = new JEditorPane("text/html", "");
        m_editorPane.setEditable(false);
        m_tableView = new TableView();
        m_tableView.setShowColorInfo(false);
        m_editorPaneScroller = new JScrollPane(m_editorPane);
        setTopComponent(m_editorPaneScroller);
        setBottomComponent(m_tableView);
    }

    /**
     * Sets a new model. The model may be <code>null</code>.
     * 
     * @param calculator the new entropy statistics
     */
    public void update(final EntropyCalculator calculator) {
        m_editorPane.setText("");
        if (calculator == null) {
            m_tableView.setDataTable(null);
            return;
        }
        m_tableView.setDataTable(calculator.getScoreTable());
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>");
        buffer.append("<body>\n");
        buffer.append("<h1>Clustering statistics</h1>");
        buffer.append("<hr>\n");
        buffer.append("<table>\n");
        buffer.append("<caption style=\"text-align: left\">");
        buffer.append("Data Statistics</caption>");
        buffer.append("<tr>");
        buffer.append("<th>Statistics</th>");
        buffer.append("<th>Value</th>");
        buffer.append("</tr>");
        String[] stats = new String[]{"Number of clusters found: ",
                "Number of objects in clusters: ",
                "Number of reference clusters: ", "Total number of patterns: "};
        int[] vals = new int[]{calculator.getNrClusters(),
                calculator.getPatternsInClusters(),
                calculator.getNrReference(),
                calculator.getPatternsInReference()};
        for (int i = 0; i < stats.length; i++) {
            buffer.append("<tr>\n");
            buffer.append("<td>\n");
            buffer.append(stats[i]);
            buffer.append("\n</td>\n");
            buffer.append("<td>\n");
            buffer.append(vals[i]);
            buffer.append("\n</td>\n");
            buffer.append("</tr>\n");
        }
        buffer.append("</table>\n");
        buffer.append("<table>\n");
        buffer.append("<caption style=\"text-align: left\">");
        buffer.append("Data Statistics</caption>");
        buffer.append("<tr>");
        buffer.append("<th>Score</th>");
        buffer.append("<th>Value</th>");
        buffer.append("</tr>");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Entropy: ");
        buffer.append("\n</td>\n");
        buffer.append("<td>\n");
        buffer.append(DoubleFormat.formatDouble(calculator.getEntropy()));
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Quality: ");
        buffer.append("\n</td>\n");
        buffer.append("<td>\n");
        buffer.append(DoubleFormat.formatDouble(calculator.getQuality()));
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("</table>\n");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        m_editorPane.setText(buffer.toString());
        // Do not call m_editorPane.getPreferredSize, bug in java:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4839118
        int preferredEditorSize =
                m_editorPaneScroller.getPreferredSize().height + 10;
        if (getSize().height > preferredEditorSize) {
            setDividerLocation(preferredEditorSize);
        }
    }

    /**
     * Sets the hilite handler to be used.
     * 
     * @param handler new handler or <code>null</code>
     */
    public void setHiliteHandler(final HiLiteHandler handler) {
        m_tableView.setHiLiteHandler(handler);
    }
}
