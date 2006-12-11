// Namespaces.java - Analyze namespace nodes in a DOM tree

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xml.resolver.helpers;

import org.w3c.dom.*;

/**
 * Static Namespace query methods.
 *
 * <p>This class defines a set of static methods that can be called
 * to analyze the namespace properties of DOM nodes.</p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class Namespaces {
    /**
     * Returns the "prefix" part of a QName or the empty string (not
     * null) if the name has no prefix.
     *
     * @param element The QName of an element.
     * @return The prefix part of the element name.
     */
    public static String getPrefix(Element element) {
	String name = element.getTagName();
	String prefix = "";

	if (name.indexOf(':') > 0) {
	    prefix = name.substring(0, name.indexOf(':'));
	}

	return prefix;
    }

    /**
     * Returns the "localname" part of a QName, which is the whole
     * name if it has no prefix.
     *
     * @param element The QName of an element.
     * @return The local part of a QName.
     */
    public static String getLocalName(Element element) {
	String name = element.getTagName();

	if (name.indexOf(':') > 0) {
	    name = name.substring(name.indexOf(':')+1);
	}

	return name;
    }

    /**
     * Returns the namespace URI for the specified prefix at the
     * specified context node.
     *
     * @param node The context node.
     * @param prefix The prefix.
     * @return The namespace URI associated with the prefix, or
     * null if no namespace declaration exists for the prefix.
     */
    public static String getNamespaceURI(Node node, String prefix) {
	if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
	    return null;
	}

	if (prefix.equals("")) {
	    if (((Element) node).hasAttribute("xmlns")) {
		return ((Element) node).getAttribute("xmlns");
	    }
	} else {
	    String nsattr = "xmlns:" + prefix;
	    if (((Element) node).hasAttribute(nsattr)) {
		return ((Element) node).getAttribute(nsattr);
	    }
	}

	return getNamespaceURI(node.getParentNode(), prefix);
    }

    /**
     * Returns the namespace URI for the namespace to which the
     * element belongs.
     *
     * @param element The element.
     * @return The namespace URI associated with the namespace of the
     * element, or null if no namespace declaration exists for it.
     */
    public static String getNamespaceURI(Element element) {
	String prefix = getPrefix(element);
	return getNamespaceURI(element, prefix);
    }
}
