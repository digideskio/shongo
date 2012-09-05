package cz.cesnet.shongo.jade.ontology;

import jade.content.Concept;

/**
 * A command result telling that the command is not supported on the target device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandNotSupported implements Concept
{
    private String description;

    public CommandNotSupported()
    {
    }

    public CommandNotSupported(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}