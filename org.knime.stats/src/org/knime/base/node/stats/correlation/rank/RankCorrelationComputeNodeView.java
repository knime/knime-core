/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.base.node.stats.correlation.rank;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.DoubleGrayValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
final class RankCorrelationComputeNodeView extends NodeView<RankCorrelationComputeNodeModel> {

    private final TableView m_tableView;
    private String m_currentRendererID = ColorRender.DESCRIPTION;

    /** Inits GUI.
     * @param model The underlying model.
     */
    public RankCorrelationComputeNodeView(final RankCorrelationComputeNodeModel model) {
        super(model);
        m_tableView = new TableView(new MyTableContentView());
        m_tableView.setColumnHeaderResizingAllowed(true);
        Component c = m_tableView.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER);
        if (c instanceof JTableHeader) {
            JTableHeader cornerHeader = (JTableHeader)c;
            cornerHeader.setDefaultRenderer(new LegendCornerAll());
        }
        TableContentView contentView = m_tableView.getContentTable();
        contentView.setShowIconInColumnHeader(false);
        m_tableView.setShowColorInfo(false);
        contentView.getTableHeader().setReorderingAllowed(false);
        contentView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setComponent(m_tableView);
        getJMenuBar().add(getJMenu());
    }

    /** {@inheritDoc} */
    @Override
    protected void modelChanged() {
        DataTable table = getNodeModel().getCorrelationTable();
        TableContentModel cntModel = m_tableView.getContentModel();
        cntModel.setDataTable(table);
        changeRenderer(m_currentRendererID);
        // must not call this on cntView as that would not affect the
        // row header column
        m_tableView.setRowHeight(16);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClose() {

    }

    /** {@inheritDoc} */
    @Override
    protected void onOpen() {

    }

    private JMenu getJMenu() {
        JMenu menu = new JMenu("View");
        JCheckBoxMenuItem useColorBox = new JCheckBoxMenuItem("Use Colors");
        useColorBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (((JCheckBoxMenuItem)e.getSource()).isSelected()) {
                    changeRenderer(ColorRender.DESCRIPTION);
                } else {
                    changeRenderer(DoubleValueRenderer
                            .STANDARD_RENDERER.getDescription());
                }
            }
        });
        useColorBox.addPropertyChangeListener(
                "ancestor", new PropertyChangeListener() {
           /** {@inheritDoc} */
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                ((JCheckBoxMenuItem)evt.getSource()).setSelected(
                        m_currentRendererID.equals(ColorRender.DESCRIPTION));
            }
        });
        JMenuItem colWidthItem = new JMenuItem("Cell Size...");
        colWidthItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int colWidth = m_tableView.getColumnWidth();
                JSpinner s = new JSpinner(new SpinnerNumberModel(
                        colWidth, 1, Integer.MAX_VALUE, 1));
                int r = JOptionPane.showConfirmDialog(m_tableView, s,
                        "Cell Size", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (r == JOptionPane.OK_OPTION) {
                    m_tableView.setColumnWidth((Integer)s.getValue());
                    m_tableView.setRowHeight((Integer)s.getValue());
                }
            }
        });
        menu.add(useColorBox);
        menu.add(colWidthItem);
        return menu;
    }

    private void changeRenderer(final String renderer) {
        TableContentView tcv = m_tableView.getContentTable();
        tcv.changeRenderer(DoubleCell.TYPE, renderer);
        Component c = m_tableView.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER);
        LegendCornerAll cornerRenderer = null;
        if (c instanceof JTableHeader) {
            JTableHeader corner = (JTableHeader)c;
            TableCellRenderer ren = corner.getDefaultRenderer();
            if (ren instanceof LegendCornerAll) {
                cornerRenderer = (LegendCornerAll)ren;
            }
        }
        if (cornerRenderer != null) {
            if (renderer.equals(ColorRender.DESCRIPTION)) {
                cornerRenderer.setPaintLegend(true);
                // no need to set column header height, done by label UI
                tcv.setColumnWidth(15);
                TableCellRenderer r = m_tableView.getContentTable()
                    .getTableHeader().getDefaultRenderer();
                if (r instanceof JLabel) {
                    ((JLabel)r).setUI(new VerticalLabelUI());
                }
            } else {
                cornerRenderer.setPaintLegend(false);
                m_tableView.setColumnHeaderViewHeight(16);
                tcv.setColumnWidth(75);
                TableCellRenderer r = m_tableView.getContentTable()
                    .getTableHeader().getDefaultRenderer();
                if (r instanceof JLabel) {
                    ((JLabel)r).updateUI();
                }
            }
        }
        m_currentRendererID = renderer;
    }

    private static class MyTableContentView extends TableContentView {
        /** {@inheritDoc} */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec colSpec) {
            if (!colSpec.getType().equals(DoubleCell.TYPE)) {
                return super.getRendererFamily(colSpec);
            }
            DataValueRenderer[] renderers = new DataValueRenderer[2];
            renderers[0] = new ColorRender(colSpec);
            renderers[1] = DoubleValueRenderer.STANDARD_RENDERER;
            return new DefaultDataValueRendererFamily(renderers);
        }

        /** {@inheritDoc} */
        @Override
        public Component prepareRenderer(final TableCellRenderer renderer,
                final int row, final int column) {
            Component result = super.prepareRenderer(renderer, row, column);
            // overwrite the component's tooltip text
            // must not call on this as the component tooltip overwrites
            // our tooltip
            JComponent jresult = null;
            if (result instanceof JComponent) {
                jresult = (JComponent)result;
            }
            Object val = getValueAt(row, column);
            if (val instanceof DoubleValue) {
                String rowName = getContentModel().getRowKey(row).toString();
                String colName = getColumnName(column);
                if (jresult != null) {
                    double corr = ((DoubleValue)val).getDoubleValue();
                    String dS = DoubleFormat.formatDouble(corr);
                    jresult.setToolTipText(dS + " ("
                            + rowName + " - " + colName + ")");
                }
            } else {
                if (jresult != null) {
                    jresult.setToolTipText(null);
                }
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        protected void onMouseClickInHeader(final MouseEvent e) {
            super.onMouseClickInHeader(e);
            // disallow changing renderer via right click
        }

    }

    private static class ColorRender extends DoubleGrayValueRenderer {

        /** Name of the renderer. */
        @SuppressWarnings("hiding")
        public static final String DESCRIPTION = "Correlation Coloring";

        /** Passes argument to super constructor.
         * @param spec The spec for the column.
         */
        public ColorRender(final DataColumnSpec spec) {
            super(spec);
            setPaintCrossForMissing(true);
        }

        /** {@inheritDoc} */
        @Override
        public void setBorder(final Border border) {
            // ignore
        }

        /** {@inheritDoc} */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /** {@inheritDoc} */
        @Override
        protected Color setDoubleValue(final double val,
                final double min, final double max) {
            if (max == min) {
                return Color.WHITE;
            }
            if (Double.isNaN(val)) {
                return null;
            }
            float v = (float)Math.abs(2.0 * (val - min) / (max - min) - 1.0);
            if (val < (max + min) / 2.0) {
                // from red to white
                return new Color(1, 1 - v, 1 - v);
            } else {
                // from blue to white
                return new Color(1 - v, 1 - v, 1);
            }
        }
    }

    private static class VerticalLabelUI extends BasicLabelUI {
        /** {@inheritDoc} */
        @Override
        public Dimension getMaximumSize(final JComponent c) {
            Dimension s = super.getMaximumSize(c);
            return new Dimension(s.height, s.width);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getMinimumSize(final JComponent c) {
            Dimension s = super.getMinimumSize(c);
            return new Dimension(s.height, s.width);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredSize(final JComponent c) {
            Dimension s = super.getPreferredSize(c);
            return new Dimension(s.height, s.width);
        }

        /** {@inheritDoc} */
        @Override
        protected String layoutCL(final JLabel label,
                final FontMetrics fontMetrics, final String text,
                final Icon icon, final Rectangle viewR, final Rectangle iconR,
                final Rectangle textR) {
            Rectangle viewRC = new Rectangle(
                    viewR.y, viewR.x, viewR.height, viewR.width);
            Rectangle iconRC = iconR;
            Rectangle textRC = textR;
            return super.layoutCL(
                    label, fontMetrics, text, icon, viewRC, iconRC, textRC);
        }

        /** {@inheritDoc} */
        @Override
        public void paint(final Graphics g, final JComponent c) {
            Graphics2D g2D = (Graphics2D)g;
            g2D.rotate(-Math.PI / 2);
            g2D.translate(-c.getHeight(), 0);
            super.paint(g2D, c);
        }
    }

    private static class LegendCorner extends ColorRender {
        private static final DataColumnSpec SPEC;
        private static final DataCell MIN = new DoubleCell(-1.0);
        private static final DataCell MAX = new DoubleCell(1.0);
        static {
            DataColumnSpecCreator c =
                new DataColumnSpecCreator("ignored", DoubleCell.TYPE);
            c.setDomain(new DataColumnDomainCreator(MIN, MAX).createDomain());
            SPEC = c.createSpec();
        }

        /** Just some initialization is done here. */
        public LegendCorner() {
            super(SPEC);
            setPaintCrossForMissing(true);
            setIconTextGap(6);
        }

        /** {@inheritDoc} */
        @Override
        protected int getIconHeight() {
            return getFont().getSize();
        }

        /** {@inheritDoc} */
        @Override
        protected int getIconWidth() {
            return getIconHeight();
        }

        private void setShowMin() {
            setValue(MIN);
            setTextInternal("corr = -1");
        }

        private void setShowMax() {
            setValue(MAX);
            setTextInternal("corr = +1");
        }

        private void setShowMissing() {
            setValue(DataType.getMissingCell());
            setTextInternal("corr = n/a");
        }
    }

    private static class LegendCornerAll extends DefaultTableCellRenderer {

        private final LegendCorner m_delegate = new LegendCorner();
        private boolean m_paintLegend = true;

        /** {@inheritDoc} */
        @Override
        public Component getTableCellRendererComponent(
                final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus,
                final int row, final int column) {
            m_delegate.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            return super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
        }

        /** {@inheritDoc} */
        @Override
        protected void paintComponent(final Graphics g) {
            if (m_paintLegend) {
                Insets s = getInsets();
                int width = getWidth() - s.left - s.right;
                int height = Math.max(1, (getHeight() - s.top - s.bottom) / 3);
                m_delegate.setSize(width, height);
                Rectangle v = new Rectangle(s.left, s.top, width, height);
                m_delegate.setShowMin();
                SwingUtilities.paintComponent(g, m_delegate, this, v);
                m_delegate.setShowMax();
                v.y += height;
                SwingUtilities.paintComponent(g, m_delegate, this, v);
                m_delegate.setShowMissing();
                v.y += height;
                SwingUtilities.paintComponent(g, m_delegate, this, v);
            } else {
                super.paintComponent(g);
            }
        }

        private void setPaintLegend(final boolean paintIt) {
            m_paintLegend = paintIt;
        }

        /** {@inheritDoc} */
        @Override
        public void setBorder(final Border border) {
            super.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        }

    }
}
