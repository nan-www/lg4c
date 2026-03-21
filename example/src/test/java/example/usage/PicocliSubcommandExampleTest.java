package example.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class PicocliSubcommandExampleTest {

    @Test
    void rootCommandRegistersSubcommands() {
        CommandLine commandLine = new CommandLine(new PicocliSubcommandExample.RootCommand());

        assertTrue(commandLine.getSubcommands().containsKey("greet"));
        assertTrue(commandLine.getSubcommands().containsKey("math"));
    }

    @Test
    void nestedSubcommandExecutes() {
        StringWriter output = new StringWriter();
        CommandLine commandLine = new CommandLine(new PicocliSubcommandExample.RootCommand());
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("math", "add", "7", "5");

        assertEquals(0, exitCode);
        assertEquals("sum=12\n", output.toString());
    }
}
