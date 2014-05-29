package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#getRecording}
 */
public class GetRecording extends ConnectorCommand<Recording>
{
    private String recordingId;

    public GetRecording()
    {
    }

    public GetRecording(String recordingId)
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
    public Recording execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).getRecording(recordingId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRecording.class.getSimpleName() + " (recordingId: %s)", recordingId);
    }
}
