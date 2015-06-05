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
package org.knime.workbench.ui.metainfo.model;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class MultilineMetaGUIElement extends MetaGUIElement {

    public MultilineMetaGUIElement(final String label, final String value, final boolean isReadOnly) {
        super(label, value, isReadOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control createGUIElement(final FormToolkit toolkit, final Composite parent) {
        int style = SWT.BORDER | SWT.MULTI | SWT.SCROLL_LINE | SWT.V_SCROLL | SWT.H_SCROLL;
        Text text = toolkit.createText(parent, getValue().trim(), style);

        attachScrollbarListener(text);

        GridData layout = new GridData(GridData.FILL_HORIZONTAL);
        layout.heightHint = 350;
        text.setLayoutData(layout);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                fireModifiedEvent(e);
            }
        });
        text.setEnabled(!isReadOnly());
        setControl(text);
        return text;
    }

    /**
     * @param text
     */
    private void attachScrollbarListener(Text text) {
        Listener scrollBarListener = new Listener() {
            @Override
            public void handleEvent(final Event event) {
                Text t = (Text)event.widget;
                Rectangle r1 = t.getClientArea();
                Rectangle r2 = t.computeTrim(r1.x, r1.y, r1.width, r1.height);
                Point p = t.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                t.getHorizontalBar().setVisible(r2.width <= p.x);
                t.getVerticalBar().setVisible(r2.height <= p.y);
                if (event.type == SWT.Modify) {
                    t.getParent().layout(true);
                    t.showSelection();
                }
            }
        };
        text.addListener(SWT.Resize, scrollBarListener);
        text.addListener(SWT.Modify, scrollBarListener);
    }

    private Text getTextControl() {
        return (Text)getControl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveTo(final TransformerHandler parentElement) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", "multiline");
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", getLabel());
        atts.addAttribute(null, null, MetaGUIElement.READ_ONLY, "CDATA", "" + isReadOnly());
        parentElement.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        char[] value = getTextControl().getText().trim().toCharArray();
        parentElement.characters(value, 0, value.length);
        parentElement.endElement(null, null, MetaGUIElement.ELEMENT);
    }

}
