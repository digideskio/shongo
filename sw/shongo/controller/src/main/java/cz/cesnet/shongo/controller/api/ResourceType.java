package cz.cesnet.shongo.controller.api;

/**
 * Represents a resource type.
 *
 * @author Martin Srom
 */
public enum ResourceType
{
    ManagedEndPoint,
    UnmanagedEndPoint,
    MultipointServer,
    GatewayServer,
    RecordingServer,
    StreamingServer,
    VirtualRoom,
    License,
    Identifier,
    Other
}
