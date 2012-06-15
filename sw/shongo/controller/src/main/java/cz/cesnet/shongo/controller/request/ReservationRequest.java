package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a request created by an user to get allocated some resources for videoconference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends PersistentObject
{
    /**
     * Type of reservation.
     */
    public static enum Type
    {
        /**
         * Reservation that can be created by any user.
         */
        NORMAL,

        /**
         * Reservation that can be created only by owner of resources,
         * and the reservation can request only owned resources.
         */
        PERMANENT
    }

    /**
     * A purpose for which the reservation will be used.
     */
    public static enum Purpose
    {
        /**
         * Reservation will be used e.g., for research purposes.
         */
        SCIENCE,

        /**
         * Reservation will be used for education purposes (e.g., for a lecture).
         */
        EDUCATION
    }

    /**
     * Unique identifier in whole Shongo.
     */
    private Identifier identifier;

    /**
     * Type of the reservation. Permanent reservation are created by resource owners to
     * allocate the resource for theirs activity.
     */
    private Type type = Type.NORMAL;

    /**
     * Purpose for the reservation (science/education).
     */
    private Purpose purpose;

    /**
     * Name of the reservation that is shown to users.
     */
    private String name;

    /**
     * List of date/time slots for which the reservation is requested.
     */
    private List<DateTimeSlot> requestedSlots = new ArrayList<DateTimeSlot>();

    /**
     * List of compartments that are requested for a reservation. Each
     * compartment represents a group of resources/persons that will
     * be used/participate in a separate videoconference call.
     */
    private List<Compartment> requestedCompartments = new ArrayList<Compartment>();

    /**
     * Specifies the default option who should initiate the call for all requested resources.
     */
    private CallInitiation callInitiation;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * @return {@link #identifier} as string
     */
    @Column(name = "identifier")
    public String getIdentifierAsString()
    {
        return (identifier != null ? identifier.toString() : null);
    }

    /**
     * @param identifier Sets the {@link #identifier} from string
     */
    private void setIdentifierAsString(String identifier)
    {
        if (identifier != null) {
            this.identifier = new Identifier(identifier);
        }
        else {
            this.identifier = null;
        }
    }

    /**
     * @return {@link #identifier} object (stored in db as string by IdentifierAsString methods)
     */
    @Transient
    public Identifier getIdentifier()
    {
        return identifier;
    }

    /**
     * Create a new identifier for the resource.
     *
     * @param domain domain to which the resource belongs.
     */
    public void createNewIdentifier(String domain)
    {
        if (identifier != null) {
            throw new IllegalStateException("Reservation request has already created identifier!");
        }
        identifier = new Identifier(Identifier.Type.RESERVATION, domain);
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #purpose}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Purpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(Purpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #requestedSlots}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<DateTimeSlot> getRequestedSlots()
    {
        return Collections.unmodifiableList(requestedSlots);
    }

    /**
     * @param requestedSlot slot to be added to the list of requested slots
     */
    public void addRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.add(requestedSlot);
    }

    /**
     * Add slot to the list of requested slots
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addRequestedSlot(DateTime dateTime, Period duration)
    {
        requestedSlots.add(new DateTimeSlot(dateTime, duration));
    }

    /**
     * @param requestedSlot slot to be removed from the {@link #requestedSlots}
     */
    public void removeRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.remove(requestedSlot);
    }

    /**
     * @return {@link #requestedCompartments}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reservationRequest")
    @Access(AccessType.FIELD)
    public List<Compartment> getRequestedCompartments()
    {
        return Collections.unmodifiableList(requestedCompartments);
    }

    /**
     * @param compartment compartment to be added to the {@link #requestedCompartments}
     */
    public void addRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment) == false) {
            requestedCompartments.add(compartment);
            compartment.setReservationRequest(this);
        }
    }

    /**
     * @param compartment compartment to be removed from the {@link #requestedCompartments}
     */
    public void removeRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment)) {
            requestedCompartments.remove(compartment);
            compartment.setReservationRequest(null);
        }
    }

    /**
     * @return a new compartment that was added to the list of requested resources
     */
    public Compartment addRequestedCompartment()
    {
        Compartment compartment = new Compartment();
        addRequestedCompartment(compartment);
        return compartment;
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    /**
     * @return {@link #interDomain}
     */
    @Column
    public boolean isInterDomain()
    {
        return interDomain;
    }

    /**
     * @param interDomain sets the {@link #interDomain}
     */
    public void setInterDomain(boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("identifier", getIdentifierAsString());
        map.put("type", getType().toString());
        if ( getPurpose() != null ) {
            map.put("purpose", getPurpose().toString());
        }
        addCollectionToMap(map, "slots", requestedSlots);
        addCollectionToMap(map, "compartments", requestedCompartments);
    }

    /**
     * Enumerate requested date/time slots in a specific interval.
     * @param from interval start
     * @param to   interval end
     * @return list of all requested absolute date/time slots for given interval
     */
    public List<AbsoluteDateTimeSlot> enumerateRequestedSlots(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        List<AbsoluteDateTimeSlot> enumeratedSlots = new ArrayList<AbsoluteDateTimeSlot>();
        for ( DateTimeSlot slot : requestedSlots ) {
            enumeratedSlots.addAll(slot.enumerate(from, to));
        }
        return enumeratedSlots;
    }
}