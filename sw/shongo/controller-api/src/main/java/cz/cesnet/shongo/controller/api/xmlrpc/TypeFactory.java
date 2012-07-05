package cz.cesnet.shongo.controller.api.xmlrpc;

import cz.cesnet.shongo.controller.api.FaultException;
import cz.cesnet.shongo.controller.api.util.Converter;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Map;

/**
 * TypeFactory that converts between objects and maps
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeFactory extends TypeFactoryImpl
{
    public TypeFactory(XmlRpcController pController)
    {
        super(pController);
    }

    @Override
    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI,
            String pLocalName)
    {
        // Allow for converting struct with class attribute to object
        if (MapSerializer.STRUCT_TAG.equals(pLocalName)) {
            // Create custom map parser that checks class attribute
            return new MapParser(pConfig, pContext, this)
            {
                @Override
                public void endElement(String pURI, String pLocalName, String pQName) throws SAXException
                {
                    super.endElement(pURI, pLocalName, pQName);
                    Map map = null;
                    try {
                        map = (Map) getResult();
                    }
                    catch (XmlRpcException exception) {
                        throw new SAXException(exception);
                    }
                    // If the class key is present convert the map to object
                    if (map != null && map.containsKey("class")) {
                        // Convert map to object of the class
                        try {
                            setResult(Converter.convertMapToObject(map));
                        }
                        catch (FaultException exception) {
                            throw new SAXException(exception);
                        }
                    }
                }
            };
        }
        else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    @Override
    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException
    {
        if (pObject != null && Converter.isAtomic(pObject)) {
            pObject = pObject.toString();
        }
        TypeSerializer serializer = super.getSerializer(pConfig, pObject);
        // If none serializer was found, serialize by object attributes
        if (serializer == null) {
            // Create custom map serializer to serialize object to map
            serializer = new MapSerializer(this, pConfig)
            {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException
                {
                    try {
                        Map<String, Object> map = Converter.convertObjectToMap(pObject);
                        super.write(pHandler, map);
                    }
                    catch (FaultException exception) {
                        throw new SAXException(exception);
                    }
                }
            };
        }
        return serializer;
    }
}