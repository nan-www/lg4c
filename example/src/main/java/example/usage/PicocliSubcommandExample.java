package example.usage;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

public final class PicocliSubcommandExample {
    private PicocliSubcommandExample() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }

    @Command(
            name = "demo",
            description = "Picocli subcommand example",
            mixinStandardHelpOptions = true,
            subcommands = { GreetCommand.class, MathCommand.class })
    static class RootCommand implements Runnable {

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "greet", description = "Print a greeting")
    static class GreetCommand implements Runnable {
        @Option(names = "--name", required = true, description = "Name to greet")
        String name;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().getOut().printf("hello, %s%n", name);
        }
    }

    @Command(
            name = "math",
            description = "Math operations",
            subcommands = { AddCommand.class, MultiplyCommand.class })
    static class MathCommand implements Runnable {
        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @Command(name = "add", description = "Add two integers")
    static class AddCommand implements Runnable {
        @Parameters(index = "0", description = "Left operand")
        int left;

        @Parameters(index = "1", description = "Right operand")
        int right;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().getOut().printf("sum=%d%n", left + right);
        }
    }

    @Command(name = "multiply", description = "Multiply two integers")
    static class MultiplyCommand implements Runnable {
        @Parameters(index = "0", description = "Left operand")
        int left;

        @Parameters(index = "1", description = "Right operand")
        int right;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().getOut().printf("product=%d%n", left * right);
        }
    }
}
