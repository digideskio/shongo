package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Represents a {@link Connection} by alias.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ConnectionByAddress extends Connection
{
    /**
     * {@link Alias} for the {@link Connection#allocatedEndpointTo}.
     */
    private Technology technology;

    /**
     * IP address or URL.
     */
    private Address address;

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #address}
     */
    public Address getAddress()
    {
        return address;
    }

    /**
     * @param address sets the {@link #address}
     */
    public void setAddress(Address address)
    {
        this.address = address;
    }
}