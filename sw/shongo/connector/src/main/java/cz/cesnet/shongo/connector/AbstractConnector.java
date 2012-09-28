package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * A common functionality for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
abstract public class AbstractConnector implements CommonService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnector.class);

    /**
     * Info about the connector and the device.
     */
    protected ConnectorInfo info = new ConnectorInfo(getClass().getSimpleName());

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return info;
    }





    /**
     * Just for debugging purposes, for printing results of commands.
     * <p/>
     * Taken from:
     * http://stackoverflow.com/questions/2325388/java-shortest-way-to-pretty-print-to-stdout-a-org-w3c-dom-document
     *
     * @param doc
     * @param out
     * @throws IOException
     * @throws javax.xml.transform.TransformerException
     */
    protected static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
