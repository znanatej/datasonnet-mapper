package com.datasonnet.commands;


import com.datasonnet.Mapper;
import com.datasonnet.spi.DataFormatService;
import picocli.CommandLine;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "validate",
        description = "Validate a DataSonnet map"
)
public class Validate implements Callable<Void> {

    @CommandLine.Parameters(
            index = "0",
            description = "Map file"
    )
    private File datasonnet;

    @CommandLine.Option(names = {"-i", "--includes-function"})
    boolean includesFunction;


    @Override
    public Void call() throws Exception {
        Mapper mapper = new Mapper(Main.readFile(datasonnet),
                Collections.emptyList(), // inputs
                Collections.emptyMap(),  // imports
                !includesFunction,       // should wrap as func
                Collections.emptyList(),  // additional libs
                DataFormatService.DEFAULT); // default service
        System.out.println("Validates!");
        return null;
    }
}
