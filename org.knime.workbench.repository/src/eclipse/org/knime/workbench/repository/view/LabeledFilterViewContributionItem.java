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
 * ------------------------------------------------------------------------
 */
public class LabeledFilterViewContributionItem extends
        FilterViewContributionItem {
    private Text m_label;

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     */
    public LabeledFilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter) {
        super(viewer, filter);
    }

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     * @param liveUpdate Set to true if the filter should be updated on every
     *      key pressed. If false, it is only updated on pressing enter.
     *
     */
    public LabeledFilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter, final boolean liveUpdate) {
        super(viewer, filter, liveUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout();
        layout.fill = true;
        layout.wrap = false;
        layout.center = true;
        comp.setLayout(layout);
        m_label = new Text(comp, SWT.NONE);
        m_label.setText("Filter");
        m_label.setEditable(false);
        m_label.setBackground(parent.getBackground());
        Combo combo = new Combo(comp, SWT.DROP_DOWN);
        combo.addKeyListener(this);
        combo.addSelectionListener(createSelectionAdaptor());
        RowData rowData = new RowData();
        rowData.width = 150;
        combo.setLayoutData(rowData);
        setCombo(combo);
        return comp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeWidth(final Control control) {
        return super.computeWidth(control)
                + m_label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    }

}
