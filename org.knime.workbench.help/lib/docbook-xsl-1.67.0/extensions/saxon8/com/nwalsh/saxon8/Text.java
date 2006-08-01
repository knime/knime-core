// Text - Saxon extension element for inserting text

package com.nwalsh.saxon8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;

import net.sf.saxon.Controller;
import net.sf.saxon.instruct.ExtensionInstruction;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.xpath.XPathException;

/*
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;
*/

import org.xml.sax.AttributeList;

/**
 * <p>Saxon extension element for inserting text
 *
 * <p>$Id$</p>
 *
 * <p>Copyright (C) 2000 Norman Walsh.</p>
 *
 * <p>This class provides a
 * <a href="http://users.iclway.co.uk/mhkay/saxon/">Saxon</a>
 * extension element for inserting text into a result tree.</p>
 *
 * <p><b>Change Log:</b></p>
 * <dl>
 * <dt>1.0</dt>
 * <dd><p>Initial release.</p></dd>
 * </dl>
 *
 * @author Norman Walsh
 * <a href="mailto:ndw@nwalsh.com">ndw@nwalsh.com</a>
 *
 * @version $Id$
 *
 */
public class Text extends ExtensionInstruction {
  Expression hrefExpr;
  Expression encodingExpr;

  /**
   * <p>Constructor for Text</p>
   *
   * <p>Does nothing.</p>
   */
  public Text() {
  }

  /**
   * <p>Can this element contain a template-body?</p>
   *
   * <p>Yes, it can, but only so that it can contain xsl:fallback.</p>
   *
   * @return true
   */
  public boolean mayContainTemplateBody() {
    return true;
  }

  /**
   * <p>Validate the arguments</p>
   *
   * <p>The element must have an href attribute.</p>
   */
  public void prepareAttributes() throws TransformerConfigurationException {
    // Get mandatory href attribute
    String fnAtt = getAttribute("href");
    if (fnAtt == null || "".equals(fnAtt)) {
      reportAbsence("href");
    }
    hrefExpr = makeAttributeValueTemplate(fnAtt);

    fnAtt = getAttribute("encoding");
    if (fnAtt == null || "".equals(fnAtt)) {
      encodingExpr = null;
    } else {
      encodingExpr = makeAttributeValueTemplate(fnAtt);
    }
  }

  /** Validate that the element occurs in a reasonable place. */
  public void validate() throws TransformerConfigurationException {
    checkWithinTemplate();
    hrefExpr = typeCheck("href", hrefExpr);
    if (encodingExpr != null) {
      encodingExpr = typeCheck("encoding", encodingExpr);
    }
  }

  public Expression compile(Executable exec)
    throws TransformerConfigurationException {
    return new TextInstruction(hrefExpr, encodingExpr);
  }

  private static class TextInstruction extends SimpleExpression {
    Expression hrefExpr;
    Expression encodingExpr;

    public TextInstruction(Expression hrefExpr, Expression encExpr) {
      this.hrefExpr = hrefExpr;
      encodingExpr = encExpr;
    }

    public int getImplementationMethod() {
      return EVALUATE_METHOD;
    }

    public String getExpressionType() {
      return "s8text:insertfile";
    }

    /**
     * <p>Insert the text of the file into the result tree</p>
     *
     * <p>Processing this element inserts the contents of the URL named
     * by the href attribute into the result tree as plain text.</p>
     *
     * <p>Optional encoding attribute can specify encoding of resource.
     * If not specified default system encoding is used.</p>
     *
     */
    public Item evaluateItem(XPathContext context) throws XPathException {
      Controller controller = context.getController();
      NamePool namePool = controller.getNamePool();
      Receiver out = context.getReceiver();
      String href = hrefExpr.evaluateAsString(context);

      String encoding = "";
      if (encodingExpr != null) {
	encoding = encodingExpr.evaluateAsString(context);
      }

      String baseURI = ((NodeInfo) context.getContextItem()).getBaseURI();

      URIResolver resolver = context.getController().getURIResolver();

      if (resolver != null) {
	try {
	  Source source = resolver.resolve(href, baseURI);
	  href = source.getSystemId();
	} catch (TransformerException te) {
	  // nop
	}
      }

      URL baseURL = null;
      URL fileURL = null;

      try {
	baseURL = new URL(baseURI);
      } catch (MalformedURLException e0) {
	// what the!?
	baseURL = null;
      }

      try {
	try {
	  fileURL = new URL(baseURL, href);
	} catch (MalformedURLException e1) {
	  try {
	    fileURL = new URL(baseURL, "file:" + href);
	  } catch (MalformedURLException e2) {
	    System.out.println("Cannot open " + href);
	    return null;
	  }
	}

	InputStreamReader isr = null;
	if (encoding.equals("") == true)
	  isr = new InputStreamReader(fileURL.openStream());
	else
	  isr = new InputStreamReader(fileURL.openStream(), encoding);

	BufferedReader is = new BufferedReader(isr);

	final int BUFFER_SIZE = 4096;
	char chars[] = new char[BUFFER_SIZE];
	char nchars[] = new char[BUFFER_SIZE];
	int len = 0;
	int i = 0;
	int carry = -1;

	while ((len = is.read(chars)) > 0) {
	  // various new lines are normalized to LF to prevent blank lines
	  // between lines

	  int nlen = 0;
	  for (i=0; i<len; i++) {
	    // is current char CR?
	    if (chars[i] == '\r') {
	      if (i < (len - 1)) {
		// skip it if next char is LF
		if (chars[i+1] == '\n') continue;
		// single CR -> LF to normalize MAC line endings
		nchars[nlen] = '\n';
		nlen++;
		continue;
	      } else {
		// if CR is last char of buffer we must look ahead
		carry = is.read();
		nchars[nlen] = '\n';
		nlen++;
		if (carry == '\n') {
		  carry = -1;
		}
		break;
	      }
	    }
	    nchars[nlen] = chars[i];
	    nlen++;
	  }

	  out.characters(String.valueOf(nchars), 0, 0);
	  // handle look aheaded character
	  if (carry != -1) out.characters(String.valueOf((char)carry), 0, 0);
	  carry = -1;
	}
	is.close();
      } catch (Exception e) {
	System.out.println("Cannot read " + href);
      }

      return null;
    }
  }
}
