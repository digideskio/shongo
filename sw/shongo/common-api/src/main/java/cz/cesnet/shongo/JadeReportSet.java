package cz.cesnet.shongo;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class JadeReportSet extends AbstractReportSet
{
    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorReport extends cz.cesnet.shongo.JadeReport
    {
        protected String description;

        public UnknownErrorReport()
        {
        }

        public UnknownErrorReport(String description)
        {
            setDescription(description);
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Unknown error: ${description}";
            message = message.replace("${description}", (description == null ? "" : description));
            return message;
        }
    }

    /**
     * Exception for {@link UnknownErrorReport}.
     */
    public static class UnknownErrorException extends cz.cesnet.shongo.JadeException
    {
        protected UnknownErrorReport report;

        public UnknownErrorException(UnknownErrorReport report)
        {
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, UnknownErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UnknownErrorException(String description)
        {
            UnknownErrorReport report = new UnknownErrorReport();
            report.setDescription(description);
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, String description)
        {
            super(throwable);
            UnknownErrorReport report = new UnknownErrorReport();
            report.setDescription(description);
            this.report = report;
        }

        public String getDescription()
        {
            return getReport().getDescription();
        }

        @Override
        public UnknownErrorReport getReport()
        {
            return report;
        }
    }

    /**
     * Receiver agent {@link #receiverAgent} is not available now.
     */
    public static class AgentNotFoundReport extends cz.cesnet.shongo.JadeReport
    {
        protected String receiverAgent;

        public AgentNotFoundReport()
        {
        }

        public AgentNotFoundReport(String receiverAgent)
        {
            setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} is not available now.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link AgentNotFoundReport}.
     */
    public static class AgentNotFoundException extends cz.cesnet.shongo.JadeException
    {
        protected AgentNotFoundReport report;

        public AgentNotFoundException(AgentNotFoundReport report)
        {
            this.report = report;
        }

        public AgentNotFoundException(Throwable throwable, AgentNotFoundReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AgentNotFoundException(String receiverAgent)
        {
            AgentNotFoundReport report = new AgentNotFoundReport();
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public AgentNotFoundException(Throwable throwable, String receiverAgent)
        {
            super(throwable);
            AgentNotFoundReport report = new AgentNotFoundReport();
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        @Override
        public AgentNotFoundReport getReport()
        {
            return report;
        }
    }

    /**
     * Sender agent {@link #senderAgent} is not started yet.
     */
    public static class AgentNotStartedReport extends cz.cesnet.shongo.JadeReport
    {
        protected String senderAgent;

        public AgentNotStartedReport()
        {
        }

        public AgentNotStartedReport(String senderAgent)
        {
            setSenderAgent(senderAgent);
        }

        public String getSenderAgent()
        {
            return senderAgent;
        }

        public void setSenderAgent(String senderAgent)
        {
            this.senderAgent = senderAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Sender agent ${sender-agent} is not started yet.";
            message = message.replace("${sender-agent}", (senderAgent == null ? "" : senderAgent));
            return message;
        }
    }

    /**
     * Exception for {@link AgentNotStartedReport}.
     */
    public static class AgentNotStartedException extends cz.cesnet.shongo.JadeException
    {
        protected AgentNotStartedReport report;

        public AgentNotStartedException(AgentNotStartedReport report)
        {
            this.report = report;
        }

        public AgentNotStartedException(Throwable throwable, AgentNotStartedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AgentNotStartedException(String senderAgent)
        {
            AgentNotStartedReport report = new AgentNotStartedReport();
            report.setSenderAgent(senderAgent);
            this.report = report;
        }

        public AgentNotStartedException(Throwable throwable, String senderAgent)
        {
            super(throwable);
            AgentNotStartedReport report = new AgentNotStartedReport();
            report.setSenderAgent(senderAgent);
            this.report = report;
        }

        public String getSenderAgent()
        {
            return getReport().getSenderAgent();
        }

        @Override
        public AgentNotStartedReport getReport()
        {
            return report;
        }
    }

    /**
     * Abstract command error.
     */
    public static abstract class CommandAbstractErrorReport extends cz.cesnet.shongo.JadeReport
    {
        protected String command;

        public CommandAbstractErrorReport()
        {
        }

        public CommandAbstractErrorReport(String command)
        {
            setCommand(command);
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }


    }

    /**
     * Exception for {@link CommandAbstractErrorReport}.
     */
    public static abstract class CommandAbstractErrorException extends cz.cesnet.shongo.JadeException
    {
        protected CommandAbstractErrorReport report;

        public CommandAbstractErrorException()
        {
        }

        public CommandAbstractErrorException(Throwable throwable)
        {
            super(throwable);
        }

        public String getCommand()
        {
            return getReport().getCommand();
        }

        @Override
        public CommandAbstractErrorReport getReport()
        {
            return report;
        }
    }

    /**
     * Unknown error: {@link #description}
     */
    public static class CommandUnknownErrorReport extends CommandAbstractErrorReport
    {
        protected String description;

        public CommandUnknownErrorReport()
        {
        }

        public CommandUnknownErrorReport(String command, String description)
        {
            setCommand(command);
            setDescription(description);
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Unknown error: ${description}";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${description}", (description == null ? "" : description));
            return message;
        }
    }

    /**
     * Exception for {@link CommandUnknownErrorReport}.
     */
    public static class CommandUnknownErrorException extends CommandAbstractErrorException
    {
        public CommandUnknownErrorException(CommandUnknownErrorReport report)
        {
            this.report = report;
        }

        public CommandUnknownErrorException(Throwable throwable, CommandUnknownErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandUnknownErrorException(String command, String description)
        {
            CommandUnknownErrorReport report = new CommandUnknownErrorReport();
            report.setCommand(command);
            report.setDescription(description);
            this.report = report;
        }

        public CommandUnknownErrorException(Throwable throwable, String command, String description)
        {
            super(throwable);
            CommandUnknownErrorReport report = new CommandUnknownErrorReport();
            report.setCommand(command);
            report.setDescription(description);
            this.report = report;
        }

        public String getDescription()
        {
            return getReport().getDescription();
        }

        @Override
        public CommandUnknownErrorReport getReport()
        {
            return (CommandUnknownErrorReport) report;
        }
    }

    /**
     * Command {@link #command} has timeout.
     */
    public static class CommandTimeoutReport extends CommandAbstractErrorReport
    {
        protected String receiverAgent;

        public CommandTimeoutReport()
        {
        }

        public CommandTimeoutReport(String command, String receiverAgent)
        {
            setCommand(command);
            setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Command ${command} has timeout.";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link CommandTimeoutReport}.
     */
    public static class CommandTimeoutException extends CommandAbstractErrorException
    {
        public CommandTimeoutException(CommandTimeoutReport report)
        {
            this.report = report;
        }

        public CommandTimeoutException(Throwable throwable, CommandTimeoutReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandTimeoutException(String command, String receiverAgent)
        {
            CommandTimeoutReport report = new CommandTimeoutReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public CommandTimeoutException(Throwable throwable, String command, String receiverAgent)
        {
            super(throwable);
            CommandTimeoutReport report = new CommandTimeoutReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        @Override
        public CommandTimeoutReport getReport()
        {
            return (CommandTimeoutReport) report;
        }
    }

    /**
     * Receiver agent {@link #receiverAgent} doesn't implement command {@link #command}.
     */
    public static class CommandNotSupportedReport extends CommandAbstractErrorReport
    {
        protected String receiverAgent;

        public CommandNotSupportedReport()
        {
        }

        public CommandNotSupportedReport(String command, String receiverAgent)
        {
            setCommand(command);
            setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} doesn't implement command ${command}.";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link CommandNotSupportedReport}.
     */
    public static class CommandNotSupportedException extends CommandAbstractErrorException
    {
        public CommandNotSupportedException(CommandNotSupportedReport report)
        {
            this.report = report;
        }

        public CommandNotSupportedException(Throwable throwable, CommandNotSupportedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandNotSupportedException(String command, String receiverAgent)
        {
            CommandNotSupportedReport report = new CommandNotSupportedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public CommandNotSupportedException(Throwable throwable, String command, String receiverAgent)
        {
            super(throwable);
            CommandNotSupportedReport report = new CommandNotSupportedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        @Override
        public CommandNotSupportedReport getReport()
        {
            return (CommandNotSupportedReport) report;
        }
    }

    /**
     * Receiver agent {@link #receiverAgent} has refused command {@link #command}.
     */
    public static class CommandRefusedReport extends CommandAbstractErrorReport
    {
        protected String receiverAgent;

        public CommandRefusedReport()
        {
        }

        public CommandRefusedReport(String command, String receiverAgent)
        {
            setCommand(command);
            setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} has refused command ${command}.";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link CommandRefusedReport}.
     */
    public static class CommandRefusedException extends CommandAbstractErrorException
    {
        public CommandRefusedException(CommandRefusedReport report)
        {
            this.report = report;
        }

        public CommandRefusedException(Throwable throwable, CommandRefusedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandRefusedException(String command, String receiverAgent)
        {
            CommandRefusedReport report = new CommandRefusedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public CommandRefusedException(Throwable throwable, String command, String receiverAgent)
        {
            super(throwable);
            CommandRefusedReport report = new CommandRefusedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        @Override
        public CommandRefusedReport getReport()
        {
            return (CommandRefusedReport) report;
        }
    }

    /**
     * Receiver agent {@link #receiverAgent} didn't understand command {@link #command}.
     */
    public static class CommandNotUnderstoodReport extends CommandAbstractErrorReport
    {
        protected String receiverAgent;

        public CommandNotUnderstoodReport()
        {
        }

        public CommandNotUnderstoodReport(String command, String receiverAgent)
        {
            setCommand(command);
            setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} didn't understand command ${command}.";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link CommandNotUnderstoodReport}.
     */
    public static class CommandNotUnderstoodException extends CommandAbstractErrorException
    {
        public CommandNotUnderstoodException(CommandNotUnderstoodReport report)
        {
            this.report = report;
        }

        public CommandNotUnderstoodException(Throwable throwable, CommandNotUnderstoodReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandNotUnderstoodException(String command, String receiverAgent)
        {
            CommandNotUnderstoodReport report = new CommandNotUnderstoodReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public CommandNotUnderstoodException(Throwable throwable, String command, String receiverAgent)
        {
            super(throwable);
            CommandNotUnderstoodReport report = new CommandNotUnderstoodReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        @Override
        public CommandNotUnderstoodReport getReport()
        {
            return (CommandNotUnderstoodReport) report;
        }
    }

    /**
     * Receiver agent {@link #receiverAgent} throws CommandException while processing command {@link #command}: {@link #reason}
     */
    public static class CommandFailedReport extends CommandAbstractErrorReport
    {
        protected String receiverAgent;

        protected String reason;

        public CommandFailedReport()
        {
        }

        public CommandFailedReport(String command, String receiverAgent, String reason)
        {
            setCommand(command);
            setReceiverAgent(receiverAgent);
            setReason(reason);
        }

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} throws CommandException while processing command ${command}: ${reason}";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${reason}", (reason == null ? "" : reason));
            return message;
        }
    }

    /**
     * Exception for {@link CommandFailedReport}.
     */
    public static class CommandFailedException extends CommandAbstractErrorException
    {
        public CommandFailedException(CommandFailedReport report)
        {
            this.report = report;
        }

        public CommandFailedException(Throwable throwable, CommandFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandFailedException(String command, String receiverAgent, String reason)
        {
            CommandFailedReport report = new CommandFailedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            report.setReason(reason);
            this.report = report;
        }

        public CommandFailedException(Throwable throwable, String command, String receiverAgent, String reason)
        {
            super(throwable);
            CommandFailedReport report = new CommandFailedReport();
            report.setCommand(command);
            report.setReceiverAgent(receiverAgent);
            report.setReason(reason);
            this.report = report;
        }

        public String getReceiverAgent()
        {
            return getReport().getReceiverAgent();
        }

        public String getReason()
        {
            return getReport().getReason();
        }

        @Override
        public CommandFailedReport getReport()
        {
            return (CommandFailedReport) report;
        }
    }

    /**
     * Sender agent {@link #senderAgent} cannot decode response from command {@link #command}.
     */
    public static class CommandResultDecodingFailedReport extends CommandAbstractErrorReport
    {
        protected String senderAgent;

        public CommandResultDecodingFailedReport()
        {
        }

        public CommandResultDecodingFailedReport(String command, String senderAgent)
        {
            setCommand(command);
            setSenderAgent(senderAgent);
        }

        public String getSenderAgent()
        {
            return senderAgent;
        }

        public void setSenderAgent(String senderAgent)
        {
            this.senderAgent = senderAgent;
        }


        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Sender agent ${sender-agent} cannot decode response from command ${command}.";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${sender-agent}", (senderAgent == null ? "" : senderAgent));
            return message;
        }
    }

    /**
     * Exception for {@link CommandResultDecodingFailedReport}.
     */
    public static class CommandResultDecodingFailedException extends CommandAbstractErrorException
    {
        public CommandResultDecodingFailedException(CommandResultDecodingFailedReport report)
        {
            this.report = report;
        }

        public CommandResultDecodingFailedException(Throwable throwable, CommandResultDecodingFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandResultDecodingFailedException(String command, String senderAgent)
        {
            CommandResultDecodingFailedReport report = new CommandResultDecodingFailedReport();
            report.setCommand(command);
            report.setSenderAgent(senderAgent);
            this.report = report;
        }

        public CommandResultDecodingFailedException(Throwable throwable, String command, String senderAgent)
        {
            super(throwable);
            CommandResultDecodingFailedReport report = new CommandResultDecodingFailedReport();
            report.setCommand(command);
            report.setSenderAgent(senderAgent);
            this.report = report;
        }

        public String getSenderAgent()
        {
            return getReport().getSenderAgent();
        }

        @Override
        public CommandResultDecodingFailedReport getReport()
        {
            return (CommandResultDecodingFailedReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UnknownErrorReport.class);
        addReportClass(AgentNotFoundReport.class);
        addReportClass(AgentNotStartedReport.class);
        addReportClass(CommandAbstractErrorReport.class);
        addReportClass(CommandUnknownErrorReport.class);
        addReportClass(CommandTimeoutReport.class);
        addReportClass(CommandNotSupportedReport.class);
        addReportClass(CommandRefusedReport.class);
        addReportClass(CommandNotUnderstoodReport.class);
        addReportClass(CommandFailedReport.class);
        addReportClass(CommandResultDecodingFailedReport.class);
    }
}