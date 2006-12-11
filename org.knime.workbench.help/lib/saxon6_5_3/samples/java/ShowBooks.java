import com.icl.saxon.Controller;
import com.icl.saxon.Context;
import com.icl.saxon.RuleManager;
import com.icl.saxon.ExtendedInputSource;
import com.icl.saxon.handlers.ElementHandlerBase;
import com.icl.saxon.expr.Expression;
import com.icl.saxon.expr.StandaloneContext;
import com.icl.saxon.expr.SortedSelection;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.om.DocumentInfo;
import com.icl.saxon.om.Builder;
import com.icl.saxon.output.Outputter;
import com.icl.saxon.output.SaxonOutputKeys;
import com.icl.saxon.sort.SortKeyDefinition;

import javax.xml.transform.Result;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import java.util.Hashtable;
import java.util.Properties;
import java.io.File;

/**
  * Class ShowBooks:
  * This class produces an HTML rendition of a List of Books supplied in XML.
  * It is intended to be run with the input file books.xml (which is
  * adapted from the MSXML distribution).
  *
  * @author Michael H. Kay (Michael.Kay@icl.com)
  * @version 20 September 2000
  */
  
public class ShowBooks extends Controller
{
            // table of category codes and descriptions

    private Hashtable categories = new Hashtable();
    private StandaloneContext expContext;
    
    /**
      * main()<BR>
      * Expects one argument, the input filename<BR>
      * It produces an HTML rendition on the standard output<BR>
      */

    public static void main (String args[])
    throws java.lang.Exception
    {
        // Check the command-line arguments

        if (args.length != 1) {
            System.err.println("Usage: java ShowBooks input-file >output-file");
            System.exit(1);
        }
        ShowBooks app = new ShowBooks();
        app.go(args[0]);
    }
    
    public void go(String filename) throws TransformerException {

        // Set up element handlers
        RuleManager rm = new RuleManager(getNamePool());
        expContext = rm.getStandaloneContext();
        setRuleManager(rm);
        prepare(rm);

        // Set up output destination details
        Properties details = new Properties();
        Result result = new StreamResult(System.out);
        details.setProperty(OutputKeys.METHOD, "html");
        details.setProperty(OutputKeys.INDENT, "yes");
        details.setProperty(SaxonOutputKeys.INDENT_SPACES, "6");
        details.setProperty(OutputKeys.ENCODING, "iso-8859-1");
        changeOutputDestination(details, result);
        
        // Build the source document tree, stripping whitespace nodes
        Builder builder = makeBuilder();
        builder.getStripper().setStripAll();

        SAXSource ss = new SAXSource(new ExtendedInputSource( new File(filename) ));
        DocumentInfo doc = builder.build(ss);

        // run the parse, calling registered handlers as appropriate
        run(doc); 

        // close the output file
        resetOutputDestination(null);
        
        System.exit(0);
    }

    /**
    * Register the element handlers
    */

    public void prepare(RuleManager rm) throws TransformerException {

        // define how each XML element type should be handled

        rm.setHandler( "BOOKLIST", new BookListHandler());

        rm.setHandler( "BOOKS", new BooksHandler() );
        
        rm.setHandler( "ITEM", new ItemHandler() );
                         
        rm.setHandler( "CATEGORY", new CategoryHandler() );
                         
    }

    /////////////////////////////////////////////////////////////////////////////
    // INNER CLASSES
    /////////////////////////////////////////////////////////////////////////////

    /**
    * Handler for the BOOKLIST element (the outermost element)
    */

    private class BookListHandler extends ElementHandlerBase {

        public void startElement(NodeInfo e, Context c) throws TransformerException {
            Expression categories = Expression.make("//CATEGORY", expContext);
            Expression books = Expression.make("BOOKS", expContext);

            // process the categories
            c.getController().applyTemplates(c, categories, null, null);

            // process the books
            c.getController().applyTemplates(c, books, null, null);
        }
    }

    /**
    * Handler for the BOOKS element.
    * This extends ItemRenderer, which has the capability to display an HTML string before and
    * after the element content.
    */

    private class BooksHandler extends ElementHandlerBase {

        String before = "<HTML><HEAD><TITLE>Book List</TITLE></HEAD>\n" +
                        "<BODY><H1>Book List</H1><TABLE BORDER='1'>\n" +
                        "<TR><TH>Category</TH><TH>Author</TH><TH>Title</TH>" +
                        "<TH>Publisher</TH><TH>Quantity</TH><TH>Price</TH></TR>";
        String after = "</TABLE></BODY></HTML>";
        
        public void startElement(NodeInfo e, Context c) throws TransformerException {

            Controller ctrl = c.getController();
            
            // write the "before" string
            ctrl.getOutputter().write(before);

            // create an expression to select the child elements of this node and sort them
            Expression children = Expression.make("*", expContext);
            SortedSelection sortedChildren = new SortedSelection(children, 2);
            
            SortKeyDefinition sk1 = new SortKeyDefinition();
            sk1.setSortKey(Expression.make("AUTHOR", expContext));
            sortedChildren.setSortKey(sk1, 0);
            
            SortKeyDefinition sk2 = new SortKeyDefinition();
            sk2.setSortKey(Expression.make("TITLE", expContext));
            sortedChildren.setSortKey(sk2, 1);

            // process the nodes selected by this expression
            ctrl.applyTemplates(c, sortedChildren, null, null);

            // write the "after" string
            ctrl.getOutputter().write(after);

        }

    }

    /**
    * CategoryHandler keeps track of category codes and descriptions
    * in a local hash table for use while processing the book details
    */

    private class CategoryHandler extends ElementHandlerBase {
        public void startElement( NodeInfo e, Context context )
        throws TransformerException {            
            String code = e.getAttributeValue("", "CODE");
            String desc = e.getAttributeValue("", "DESC");
            categories.put(code, desc);
        }
    }

    /**
    * Handler for ITEM elements (representing individual books)
    */

    private class ItemHandler extends ElementHandlerBase {

        public void startElement( NodeInfo e, Context c ) throws TransformerException {
            Outputter out = c.getOutputter();
            int tr = getNamePool().allocate("", "", "TR");
            out.writeStartTag(tr);
            writeEntry(out, (String)categories.get(e.getAttributeValue("", "CAT")));
            writeEntry(out, Expression.make("AUTHOR", expContext).evaluateAsString(c));
            writeEntry(out, Expression.make("TITLE", expContext).evaluateAsString(c));
            writeEntry(out, Expression.make("PUBLISHER", expContext).evaluateAsString(c));
            writeEntry(out, Expression.make("QUANTITY", expContext).evaluateAsString(c));
            writeEntry(out, Expression.make("PRICE", expContext).evaluateAsString(c));
            out.writeEndTag(tr);
        }

        private void writeEntry(Outputter out, String val) throws TransformerException {
            int td = getNamePool().allocate("", "", "TD");
            out.writeStartTag(td);
            out.writeContent(val);
            out.writeEndTag(td);
        }
        
    }          			
} 
