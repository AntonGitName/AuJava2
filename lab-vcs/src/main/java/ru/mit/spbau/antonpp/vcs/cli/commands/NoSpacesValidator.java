package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author antonpp
 * @since 28/10/16
 */
public class NoSpacesValidator implements IValueValidator<String> {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value.split(" ").length > 1) {
            throw new ParameterException("Branch name must be without spaces.");
        }
    }
}
