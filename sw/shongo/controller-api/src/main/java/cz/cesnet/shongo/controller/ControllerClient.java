package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.rpc.RpcClient;
import org.apache.xmlrpc.XmlRpcException;

import java.net.ConnectException;
import java.net.URL;

/**
 * Client for a domain controller from Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerClient extends RpcClient
{
    /**
     * Constructor.
     */
    public ControllerClient()
    {
        addReportSet(new CommonReportSet());
        addReportSet(new ControllerReportSet());
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param controllerUrl
     * @throws Exception
     */
    public ControllerClient(String controllerUrl) throws Exception
    {
        this();

        if (!controllerUrl.startsWith("http")) {
            controllerUrl = "http://" + controllerUrl;
        }
        URL url = new URL(controllerUrl);
        int port = url.getPort();
        if (port == -1) {
            port = 8181;
        }
        String protocol = url.getProtocol();
        String host = url.getHost();
        connect(protocol + "://" + host, port);
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public ControllerClient(String host, int port) throws Exception
    {
        this();

        connect(host, port);
    }

    @Override
    protected Exception convertException(XmlRpcException xmlRpcException)
    {
        Exception exception = super.convertException(xmlRpcException);
        if (exception instanceof ConnectException) {
            exception = new ControllerConnectException(getConfiguration().getServerURL(), exception);
        }
        return exception;
    }
}
