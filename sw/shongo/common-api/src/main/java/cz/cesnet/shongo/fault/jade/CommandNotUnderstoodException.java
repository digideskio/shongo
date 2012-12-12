package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandNotUnderstoodException extends CommandFailureException
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_NOT_UNDERSTOOD;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The requested command was not understood by the connector.");
    }
}