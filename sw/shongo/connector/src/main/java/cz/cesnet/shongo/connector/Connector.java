package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.jade.ConnectorContainerCommandSet;
import cz.cesnet.shongo.connector.jade.command.ManageCommand;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.Logging;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a device connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Connector
{
    private static Logger logger = LoggerFactory.getLogger(Connector.class);

    public static final String DEFAULT_CONFIGURATION_FILENAME = "connector.cfg.xml";

    /**
     * Connector configuration.
     */
    private Configuration configuration = new Configuration();

    /**
     * Jade container.
     */
    Container jadeContainer;

    /**
     * Jade agent names.
     * FIXME: agents are added, but not removed...
     */
    List<String> jadeAgents = new ArrayList<String>();

    /**
     * Constructor.
     */
    public Connector()
    {
        configuration = new Configuration();
        // System properties has the highest priority
        configuration.addConfiguration(new SystemConfiguration());
    }

    /**
     * @return controller host
     */
    public String getControllerHost()
    {
        return configuration.getString(Configuration.CONTROLLER_HOST);
    }

    /**
     * @return controller host
     */
    public int getControllerPort()
    {
        return configuration.getInt(Configuration.CONTROLLER_PORT);
    }

    /**
     * @return period for checking connection the controller
     */
    public Duration getControllerConnectionCheckPeriod()
    {
        return configuration.getDuration(Configuration.CONTROLLER_CONNECTION_CHECK_PERIOD);
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(Configuration.JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(Configuration.JADE_PORT);
    }

    /**
     * Load default configuration for the connector
     */
    private void loadDefaultConfiguration()
    {
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(getClass().getClassLoader().getResource("default.cfg.xml"));
            configuration.addConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default connector configuration!", exception);
        }
    }

    /**
     * Loads connector configuration from an XML file.
     *
     * @param configurationFilename name of file containing the connector configuration
     */
    private void loadConfiguration(String configurationFilename)
    {
        // Passed configuration has lower priority
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(configurationFilename);
            configuration.addConfiguration(xmlConfiguration);
        }
        catch (ConfigurationException e) {
            logger.warn(e.getMessage());
        }
        // Default configuration has the lowest priority
        loadDefaultConfiguration();
    }

    /**
     * Init connector.
     */
    public void start()
    {
        logger.info("Starting Connector JADE container on {}:{}...", getJadeHost(), getJadePort());
        logger.info("Connecting to the JADE main container {}:{}...", getControllerHost(), getControllerPort());

        jadeContainer = Container
                .createContainer(getControllerHost(), getControllerPort(), getJadeHost(), getJadePort());
        jadeContainer.start();

        // start configured agents
        for (HierarchicalConfiguration instCfg : configuration.configurationsAt("instances.instance")) {
            String agentName = instCfg.getString("name");
            addAgent(agentName);
        }
        configureAgents();
    }

    /**
     * Load agents configuration
     */
    public void configureAgents()
    {
        // Configure agents
        for (HierarchicalConfiguration instCfg : configuration.configurationsAt("instances.instance")) {
            String agentName = instCfg.getString("name");
            if (instCfg.getProperty("device.connectorClass") != null) {
                // manage a device
                ManageCommand cmd = new ManageCommand(
                        instCfg.getString("device.connectorClass"),
                        instCfg.getString("device.host"),
                        instCfg.getInt("device.port"),
                        instCfg.getString("device.auth.username"),
                        instCfg.getString("device.auth.password")
                );
                jadeContainer.performCommand(agentName, cmd);
            }
        }
    }

    /**
     * Run connector shell.
     */
    public void run()
    {
        final Shell shell = new Shell();
        shell.setPrompt("connector");
        shell.setExitCommand("exit", "Shutdown the connector");
        shell.addCommands(ContainerCommandSet.createContainerCommandSet(jadeContainer));

        shell.addCommand("add", "Add a new connector instance", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 2) {
                    Shell.printError("You must specify the new agent name.");
                    return;
                }
                addAgent(args[1]);
            }
        });
        shell.addCommand("list", "List all connector agent instances", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                for (String agent : jadeAgents) {
                    Shell.printInfo("Connector [%s]", agent);
                }
            }
        });
        shell.addCommand("select", "Select current connector instance", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 2) {
                    shell.setPrompt("connector");
                    shell.removeCommands(
                            ConnectorContainerCommandSet.createContainerAgentCommandSet(jadeContainer, null));
                    return;
                }
                String agentName = args[1];
                for (String agent : jadeAgents) {
                    if (agent.equals(agentName)) {
                        shell.setPrompt(agentName + "@connector");
                        shell.addCommands(
                                ConnectorContainerCommandSet.createContainerAgentCommandSet(jadeContainer, agentName));

                        return;
                    }
                }
                Shell.printError("Agent [%s] was not found!", agentName);
            }
        });

        // Thread that checks the connection to the main controller
        // and if it is down it tries to connect.
        final Thread connectThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                boolean startFailed = false;
                while (true) {
                    try {
                        Thread.sleep(getControllerConnectionCheckPeriod().getMillis());
                    }
                    catch (InterruptedException e) {
                    }
                    // We want to reconnect if container is not started or when the
                    // previous start failed
                    if (startFailed || jadeContainer.isStarted() == false) {
                        logger.info("Reconnecting to the JADE main container {}:{}...", getControllerHost(),
                                getControllerPort());
                        startFailed = false;
                        if (jadeContainer.start()) {
                            configureAgents();
                        }
                        else {
                            startFailed = true;
                        }
                    }
                }
            }
        });
        connectThread.start();

        shell.run();

        connectThread.stop();

        stop();
    }

    /**
     * Adds an agent of a given name to the connector.
     *
     * @param name
     */
    private void addAgent(String name)
    {
        jadeContainer.addAgent(name, ConnectorAgent.class);
        jadeAgents.add(name);
    }

    /**
     * De init connector
     */
    public void stop()
    {
        logger.info("Stopping Connector JADE container...");
        jadeContainer.stop();
    }

    /**
     * Main method of device connector.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Logging.installBridge();

        // Create options
        Option optionHelp = new Option(null, "help", false, "Print this usage information");
        Option optionHost = OptionBuilder.withLongOpt("host")
                .withArgName("HOST")
                .hasArg()
                .withDescription("Set the local interface address on which the connector Jade container will run")
                .create("h");
        Option optionPort = OptionBuilder.withLongOpt("port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the connector Jade container will run")
                .create("p");
        Option optionController = OptionBuilder.withLongOpt("controller")
                .withArgName("HOST:PORT")
                .hasArg()
                .withDescription("Set the url on which the controller is running")
                .create("c");
        Option optionConfig = OptionBuilder.withLongOpt("config")
                .withArgName("FILENAME")
                .hasArg()
                .withDescription("Connector configuration")
                .create("g");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionPort);
        options.addOption(optionController);
        options.addOption(optionConfig);
        options.addOption(optionHelp);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // Print help
        if (commandLine.hasOption(optionHelp.getLongOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>()
            {
                public int compare(Option opt1, Option opt2)
                {
                    if (opt1.getOpt() == null && opt2.getOpt() != null) {
                        return -1;
                    }
                    if (opt1.getOpt() != null && opt2.getOpt() == null) {
                        return 1;
                    }
                    if (opt1.getOpt() == null && opt2.getOpt() == null) {
                        return opt1.getLongOpt().compareTo(opt2.getLongOpt());
                    }
                    return opt1.getOpt().compareTo(opt2.getOpt());
                }
            });
            formatter.printHelp("connector", options);
            System.exit(0);
        }

        // Process parameters
        if (commandLine.hasOption(optionHost.getOpt())) {
            System.setProperty(Configuration.JADE_HOST, commandLine.getOptionValue(optionHost.getOpt()));
        }
        if (commandLine.hasOption(optionPort.getOpt())) {
            System.setProperty(Configuration.JADE_PORT, commandLine.getOptionValue(optionPort.getOpt()));
        }
        if (commandLine.hasOption(optionController.getOpt())) {
            String url = commandLine.getOptionValue(optionController.getOpt());
            String[] urlParts = url.split(":");
            if (urlParts.length == 1) {
                System.setProperty(Configuration.CONTROLLER_HOST, urlParts[0]);
            }
            else if (urlParts.length == 2) {
                System.setProperty(Configuration.CONTROLLER_HOST, urlParts[0]);
                System.setProperty(Configuration.CONTROLLER_PORT, urlParts[1]);
            }
            else {
                System.err.println("Failed to parse controller url. It should be in <HOST:URL> format.");
                System.exit(-1);
            }
        }

        final Connector connector = new Connector();

        // load configuration
        String configFilename = null;
        if (commandLine.hasOption(optionConfig.getOpt())) {
            configFilename = commandLine.getOptionValue(optionConfig.getOpt());
        }
        else {
            if (new File(DEFAULT_CONFIGURATION_FILENAME).exists()) {
                configFilename = DEFAULT_CONFIGURATION_FILENAME;
            }
        }
        if (configFilename != null) {
            logger.info("Connector loading configuration from {}", configFilename);
            connector.loadConfiguration(configFilename);
        }
        else {
            connector.loadDefaultConfiguration();
        }

        connector.start();

        logger.info("Connector successfully started.");

        connector.run();

        logger.info("Connector exiting...");
    }
}
