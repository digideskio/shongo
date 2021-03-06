package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.connector.api.jade.common.GetStatus;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.jade.SendLocalCommand;
import jade.core.AID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonServiceImpl extends AbstractServiceImpl
        implements CommonService, Component.EntityManagerFactoryAware,
                   Component.ControllerAgentAware, Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Common";
    }

    @Override
    @Debug
    public Controller getController()
    {
        Controller controller = new Controller();
        controller.setDomain(cz.cesnet.shongo.controller.Domain.getLocalDomain().toApi());
        return controller;
    }

    @Override
    @Debug
    public Collection<Domain> listDomains(SecurityToken token)
    {
        authorization.validate(token);

        List<Domain> domainList = new ArrayList<Domain>();
        domainList.add(cz.cesnet.shongo.controller.Domain.getLocalDomain().toApi());
        return domainList;
    }

    @Override
    @Debug
    public Collection<Connector> listConnectors(SecurityToken token)
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        List<DeviceResource> deviceResourceList = resourceManager.listManagedDevices();
        Map<String, DeviceResource> deviceResourceMap = new HashMap<String, DeviceResource>();
        for (DeviceResource deviceResource : deviceResourceList) {
            String agentName = ((cz.cesnet.shongo.controller.booking.resource.ManagedMode) deviceResource.getMode())
                    .getConnectorAgentName();
            deviceResourceMap.put(agentName, deviceResource);
        }

        List<Connector> connectorList = new ArrayList<Connector>();
        for (AID aid : controllerAgent.listConnectorAgents()) {
            String agentName = aid.getLocalName();

            Connector connector = new Connector();
            connector.setName(agentName);

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new GetStatus());
            if (sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
                ConnectorStatus connectorStatus = (ConnectorStatus) sendLocalCommand.getResult();
                connector.setAgentState(Connector.AgentState.AVAILABLE);
                connector.setStatus(connectorStatus);
            }
            else {
                connector.setAgentState(Connector.AgentState.NOT_AVAILABLE);
                connector.setStatus(new ConnectorStatus(ConnectorStatus.State.NOT_AVAILABLE));
            }

            DeviceResource deviceResource = deviceResourceMap.get(agentName);
            if (deviceResource != null) {
                connector.setResourceId(ObjectIdentifier.formatId(deviceResource));
                deviceResourceMap.remove(agentName);
            }

            connectorList.add(connector);
        }

        for (Map.Entry<String, DeviceResource> entry : deviceResourceMap.entrySet()) {
            Connector connector = new Connector();
            connector.setName(entry.getKey());
            connector.setResourceId(ObjectIdentifier.formatId(entry.getValue()));
            connector.setAgentState(Connector.AgentState.NOT_AVAILABLE);
            connector.setStatus(new ConnectorStatus(ConnectorStatus.State.NOT_AVAILABLE));
            connectorList.add(connector);
        }

        entityManager.close();

        return connectorList;
    }
}
