package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.joda.time.DateTimeZone;

/**
 * Persist {@link org.joda.time.DateTimeZone} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTimeZone extends PersistentStringType
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "DateTimeZone";

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = 32;

    public static final PersistentDateTimeZone INSTANCE = new PersistentDateTimeZone();

    @Override
    public Class returnedClass()
    {
        return DateTimeZone.class;
    }

    @Override
    protected Object fromNonNullString(String string) throws HibernateException
    {
        return DateTimeZone.forID(string);
    }

    @Override
    protected String toNonNullString(Object value) throws HibernateException
    {
        return ((DateTimeZone) value).getID();
    }
}

