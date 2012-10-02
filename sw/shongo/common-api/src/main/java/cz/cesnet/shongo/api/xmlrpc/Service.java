package cz.cesnet.shongo.api.xmlrpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an API service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Service
{
    /**
     * Annotation for methods which are public API.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface API
    {
    }

    /**
     * Get service name
     *
     * @return service name
     */
    public abstract String getServiceName();
}