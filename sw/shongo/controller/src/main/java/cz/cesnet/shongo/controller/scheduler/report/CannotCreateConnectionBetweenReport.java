package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.compartment.Endpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CannotCreateConnectionBetweenReport extends AbstractConnectionReport
{
    /**
     * Constructor.
     */
    public CannotCreateConnectionBetweenReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CannotCreateConnectionBetweenReport(Endpoint endpointFrom, Endpoint endpointTo)
    {
        super(endpointFrom, endpointTo);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Cannot create connection in any direction between %s and %s.",
                getEndpointFromAsString(), getEndpointToAsString());
    }
}