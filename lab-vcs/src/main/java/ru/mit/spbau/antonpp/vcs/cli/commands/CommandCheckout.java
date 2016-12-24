package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CheckoutException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "checkout", commandDescription = "Checkout revision")
public class CommandCheckout extends AbstractCommand {

    @Parameter(required = true, description = "Name of the branch or commit hash", arity = 1)
    private List<String> name;

    public CommandCheckout() {
        super(true);
    }

    @Override
    protected void runInternal() {
        try {
            repository.checkout(name.get(0));
        } catch (CheckoutException | SerializationException e) {
            exitWithError(e, e.getMessage());
        }
    }
}
