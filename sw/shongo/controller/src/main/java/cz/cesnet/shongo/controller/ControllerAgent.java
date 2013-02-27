package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.ConnectorScope;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.controller.api.jade.ControllerCommand;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
import cz.cesnet.shongo.shell.Shell;
import jade.core.AID;
import org.apache.commons.cli.CommandLine;

/**
 * Jade Agent for Domain Controller
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerAgent extends Agent
{
    /**
     * Service to be used for handling {@link cz.cesnet.shongo.controller.api.jade.ControllerCommand}s.
     */
    private Service service;

    /**
     * Constructor.
     */
    public ControllerAgent()
    {
    }

    /**
     * @param service sets the {@link #service}
     */
    public void setService(Service service)
    {
        this.service = service;
    }

    /**
     * @return {@link CommandSet} for {@link ControllerAgent}
     */
    public CommandSet createCommandSet()
    {
        CommandSet commandSet = new CommandSet();
        commandSet.addCommand("list", "List all connector agents in the domain", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                for (AID agent : listConnectorAgents()) {
                    Shell.printInfo("Agent [%s]", agent.getName());
                }
            }
        });
        return commandSet;
    }

    /**
     * @return list of all connector agents
     */
    public AID[] listConnectorAgents()
    {
        return findAgentsByService(ConnectorScope.CONNECTOR_AGENT_SERVICE, 1000);
    }

    @Override
    protected void setup()
    {
        addOntology(ConnectorOntology.getInstance());
        addOntology(ControllerOntology.getInstance());

        super.setup();

        registerService(ControllerScope.CONTROLLER_AGENT_SERVICE, ControllerScope.CONTROLLER_AGENT_SERVICE_NAME);
    }

    @Override
    public SendLocalCommand sendCommand(String receiverAgentName, Command command)
    {
        Controller.requestedCommands.info("Action:{} {}.", command.getId(), command);
        SendLocalCommand sendLocalCommand = super.sendCommand(receiverAgentName, command);
        String commandState;
        switch (sendLocalCommand.getState()) {
            case SUCCESSFUL:
                Object result = sendLocalCommand.getResult();
                if (result != null && result instanceof String) {
                    commandState = String.format("OK: %s", result);
                }
                else {
                    commandState = "OK";
                }
                break;
            case FAILED:
                commandState = String.format("FAILED: %s", sendLocalCommand.getFailure().getMessage());
                break;
            default:
                commandState = "UNKNOWN";
                break;
        }
        Controller.requestedCommands.info("Action:{} Done ({}).", command.getId(), commandState);
        return sendLocalCommand;
    }

    @Override
    public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
    {
        if (service != null && command instanceof ControllerCommand) {
            ControllerCommand controllerCommand = (ControllerCommand) command;
            Controller.executedCommands.info("Action:{} {}.", controllerCommand.getId(), controllerCommand.toString());
            Object result = null;
            String resultState = "OK";
            try {
                result = controllerCommand.execute(service);
                if (result != null && result instanceof String) {
                    resultState = String.format("OK: %s", result);
                }
            }
            catch (CommandException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            catch (RuntimeException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            finally {
                Controller.executedCommands
                        .info("Action:{} Done ({}).", controllerCommand.getId(), resultState);
            }
            return result;
        }
        return super.handleCommand(command, sender);
    }
}
