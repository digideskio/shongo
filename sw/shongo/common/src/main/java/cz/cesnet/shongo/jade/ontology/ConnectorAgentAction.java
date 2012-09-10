package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.EndpointService;
import cz.cesnet.shongo.connector.api.MultipointService;
import jade.content.AgentAction;
import jade.content.Concept;

/**
 * A common ancestor for all agent actions used by Shongo.
 *
 * Offers some helper methods common to all agent actions.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public abstract class ConnectorAgentAction implements AgentAction
{
    /**
     * Executes the action on a given connector.
     *
     * @param connector    a connector on which the action should be executed
     * @return the result of the action, or NULL if the action does not return anything
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    public abstract Concept exec(CommonService connector) throws CommandException, CommandUnsupportedException;


    /**
     * Returns the passed connector as an EndpointService. Throws an exception if the typecast fails.
     *
     * @param connector    a connector
     * @return connector typecast to an EndpointService
     * @throws CommandUnsupportedException
     */
    protected static EndpointService getEndpoint(CommonService connector) throws CommandUnsupportedException
    {
        if (!(connector instanceof EndpointService)) {
            throw new CommandUnsupportedException("The command is implemented only on an endpoint.");
        }
        return (EndpointService) connector;
    }

    /**
     * Returns the passed connector as a MultipointService. Throws an exception if the typecast fails.
     *
     * @param connector    a connector
     * @return connector typecast to an MultipointService
     * @throws CommandUnsupportedException
     */
    protected static MultipointService getMultipoint(CommonService connector) throws CommandUnsupportedException
    {
        if (!(connector instanceof EndpointService)) {
            throw new CommandUnsupportedException("The command is implemented only on an endpoint.");
        }
        return (MultipointService) connector;
    }

}