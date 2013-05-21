package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link Specification} for an endpoint which can participate in a conference.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class EndpointSpecification extends ParticipantSpecification
{
    /**
     * Persons that use the endpoint to participate in a conference.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * Defines who should initiate the call to this endpoint ({@code null} means that the {@link Scheduler}
     * can decide it).
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #persons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getPersons()
    {
        return persons;
    }

    /**
     * @param id of the {@link cz.cesnet.shongo.controller.common.Person}
     * @return {@link cz.cesnet.shongo.controller.common.Person} with given {@code id}
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link cz.cesnet.shongo.controller.common.Person} doesn't exist
     */
    @Transient
    private Person getPersonById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (Person person : persons) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(Person.class, id);
    }

    /**
     * @param person to be added to the {@link #persons}
     */
    public void addPerson(Person person)
    {
        persons.add(person);
    }

    /**
     * @param person to be removed from the {@link #persons}
     */
    public void removePerson(Person person)
    {
        persons.remove(person);
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

    @Override
    public boolean synchronizeFrom(Specification specification,
            Map<Specification, Specification> originalMap)
    {
        EndpointSpecification endpointSpecification = (EndpointSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, originalMap);
        modified |= !ObjectHelper.isSame(getCallInitiation(), endpointSpecification.getCallInitiation());

        setCallInitiation(endpointSpecification.getCallInitiation());

        return modified;
    }
}
