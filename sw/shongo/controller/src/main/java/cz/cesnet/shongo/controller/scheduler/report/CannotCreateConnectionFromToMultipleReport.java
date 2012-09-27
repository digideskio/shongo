package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.compartment.Endpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CannotCreateConnectionFromToMultipleReport extends AbstractConnectionReport
{
    /**
     * Constructor.
     */
    public CannotCreateConnectionFromToMultipleReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CannotCreateConnectionFromToMultipleReport(Endpoint endpointFrom, Endpoint endpointTo)
    {
        super(endpointFrom, endpointTo);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Cannot create connection from %s to %s, "
                + "because the target represents multiple endpoints (not supported yet).",
                getEndpointFromAsString(), getEndpointToAsString());
    }
}