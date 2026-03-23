package wn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

class PicocliAnnotationDemoTest {

    @Test
    void commandAnnotationRegistersRootMetadataAndSubcommand() {
        CommandLine cli = new CommandLine(new DemoRootCommand());

        assertEquals("demo", cli.getCommandName());
        assertTrue(cli.getSubcommands().containsKey("hello"));
    }

    @Test
    void mixinStandardHelpOptionsAddsHelpOutput() {
        StringWriter output = new StringWriter();
        CommandLine cli = new CommandLine(new DemoRootCommand());
        cli.setOut(new PrintWriter(output));

        int exitCode = cli.execute("--help");

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Usage: demo"));
        assertTrue(output.toString().contains("-h, --help"));
    }

    @Test
    void optionAnnotationBindsCommandLineArgumentsToFields() {
        CommandLine cli = new CommandLine(new DemoRootCommand());

        int exitCode = cli.execute("hello", "--name", "Alice", "--uppercase");
        HelloCommand hello = (HelloCommand) cli.getSubcommands().get("hello").getCommand();

        assertEquals(0, exitCode);
        assertEquals("HELLO, Alice", hello.renderedGreeting);
    }

    @Command(
            name = "demo",
            mixinStandardHelpOptions = true,
            subcommands = { HelloCommand.class })
    static class DemoRootCommand implements Runnable {

        @Override
        public void run() {
            // Root command keeps the top-level metadata and child command table.
        }
    }

    @Command(name = "hello", description = "Print a greeting")
    static class HelloCommand implements Callable<Integer> {
        @Option(names = "--name", required = true, description = "Name to greet")
        String name;

        @Option(names = "--uppercase", description = "Print HELLO instead of Hello")
        boolean uppercase;

        String renderedGreeting;

        @Override
        public Integer call() {
            String prefix = uppercase ? "HELLO" : "Hello";
            renderedGreeting = "%s, %s".formatted(prefix, name);
            return 0;
        }
    }
}
