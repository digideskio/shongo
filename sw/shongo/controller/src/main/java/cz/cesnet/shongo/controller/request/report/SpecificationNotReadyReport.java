package cz.cesnet.shongo.controller.request.report;

import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.PersonSpecification;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.request.StatefulSpecification;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class SpecificationNotReadyReport extends Report
{
    /**
     * @see {@link Specification}
     */
    private Specification specification;

    /**
     * Constructor.
     */
    public SpecificationNotReadyReport()
    {
    }

    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     */
    public SpecificationNotReadyReport(Specification specification)
    {
        this.specification = specification;
    }

    /**
     * @return {@link #specification}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification to be formatted
     * @return formatted given {@code specification}
     */
    public static String formatSpecification(Specification specification)
    {
        if (!(specification instanceof StatefulSpecification) || ((StatefulSpecification) specification).getCurrentState() != StatefulSpecification.State.NOT_READY) {
            return null;
        }
        if (specification instanceof PersonSpecification) {
            PersonSpecification personSpecification = (PersonSpecification) specification;
            Person person = personSpecification.getPerson();
            return String.format("%s (%s) hasn't accepted/rejected invitation or hasn't selected an endpoint yet.\n",
                    person.getName(), person.getEmail());
        }
        else if (specification instanceof CompartmentSpecification) {
            CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;
            StringBuilder stringBuilder = new StringBuilder();
            for (Specification requestedSpecification : compartmentSpecification.getChildSpecifications()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("\n");
                }
                String string = formatSpecification(requestedSpecification);
                if ( string != null) {
                    stringBuilder.append(string);
                }
            }
            return stringBuilder.toString();
        }
        return String.format("Specification '%s' with id '%d' no ready!",
                specification.getClass().getSimpleName(), specification.getId());
    }

    @Override
    @Transient
    public String getText()
    {
        return formatSpecification(specification);
    }
}