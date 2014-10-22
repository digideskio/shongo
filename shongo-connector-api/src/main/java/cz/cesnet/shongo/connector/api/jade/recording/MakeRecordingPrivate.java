package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#deleteRecording}
 */
public class MakeRecordingPrivate extends ConnectorCommand
{
    private String recordingId;

    public MakeRecordingPrivate()
    {
    }

    public MakeRecordingPrivate(String recordingId)
    {
        this.recordingId = recordingId;
    }

    public String getRecordingId()
    {
        return recordingId;
    }

    public void setRecordingId(String recordingId)
    {
        this.recordingId = recordingId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getRecording(connector).makeRecordingPrivate(recordingId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(MakeRecordingPrivate.class.getSimpleName() + " (recordingId: %s)", recordingId);
    }
}
