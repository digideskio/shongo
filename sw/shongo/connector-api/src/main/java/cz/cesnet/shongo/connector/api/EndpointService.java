package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Alias;

/**
 * Set of functionality offered by endpoint devices.
 * <p/>
 * Any of the methods may throw CommandException when a command execution fails, or CommandUnsupportedException when the
 * command is not supported (and thus may not be implemented) by the target device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface EndpointService extends CommonService
{
    /**
     * Dials a server.
     *
     * @param server server address to dial
     */
    void dial(Alias server) throws CommandException, CommandUnsupportedException;

    /**
     * Resets the device.
     */
    void resetDevice() throws CommandException, CommandUnsupportedException;

    /**
     * Mutes this endpoint.
     */
    void mute() throws CommandException, CommandUnsupportedException;

    /**
     * Unmutes this endpoint.
     */
    void unmute() throws CommandException, CommandUnsupportedException;

    /**
     * Sets microphone (all microphones) audio level of this endpoint to a given value.
     *
     * @param level microphone level to set
     */
    void setMicrophoneLevel(int level) throws CommandException, CommandUnsupportedException;

    /**
     * Sets playback audio level of this endpoint to a given value.
     *
     * @param level microphone level to set
     */
    void setPlaybackLevel(int level) throws CommandException, CommandUnsupportedException;

    /**
     * Enables video from this endpoint.
     */
    void enableVideo() throws CommandException, CommandUnsupportedException;

    /**
     * Disables video from this endpoint.
     */
    void disableVideo() throws CommandException, CommandUnsupportedException;

    /**
     * Starts the presentation mode (turns on the media stream).
     */
    void startPresentation() throws CommandException, CommandUnsupportedException;

    /**
     * Stop the presentation mode (turns off the media stream).
     */
    void stopPresentation() throws CommandException, CommandUnsupportedException;

    /**
     * Sets the device in standby mode.
     */
    void standBy() throws CommandException, CommandUnsupportedException;

}