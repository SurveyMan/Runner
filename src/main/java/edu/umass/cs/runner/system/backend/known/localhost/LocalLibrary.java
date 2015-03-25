package edu.umass.cs.runner.system.backend.known.localhost;

import edu.umass.cs.runner.system.backend.AbstractLibrary;
import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.utils.Slurpie;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

public class LocalLibrary extends AbstractLibrary {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

    private List<String> backendHeaders = new ArrayList<String>();

    public LocalLibrary(String propertiesURL) {
        if (propertiesURL == null || propertiesURL.equals(""))
            init();
        else {
            try {
                super.props = new Properties();
                super.props.load(new FileReader(propertiesURL));
            } catch (FileNotFoundException e) {
                Runner.LOGGER.warn(e);
                Runner.LOGGER.info(e.getLocalizedMessage()+"\nUsing default value instead...");
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getActionForm() {
        return "";
    }

    @Override
    public synchronized void init() {
        try {
            super.props = new Properties();
            super.props.load(new FileReader(AbstractLibrary.PARAMS));
            assert !super.props.isEmpty() : AbstractLibrary.PARAMS + " is empty";
        } catch(FileNotFoundException fnfe) {
            Scanner scanner = new Scanner(System.in);
            PrintWriter printWriter = new PrintWriter(System.out);
            String params;
            FileWriter fileWriter;
            try {
                while (true) {
                    printWriter.print(String.format(
                                    "\nCannot find file %1$s. Would you like to:\n" +
                                            "\t[1] Use the default %1$s from the Runner jar?\n" +
                                            "\t[2] Use the deprecated global %1$s in ~/surveyman?\n" +
                                            "\t[3] Write your own %1$s and run this program again when you're done?\n" +
                                            "surveyman>", AbstractLibrary.PARAMS)
                    );
                    printWriter.flush();
                    int selection = scanner.nextInt();
                    switch (selection) {
                        case 1:
                            params = Slurpie.slurp("params.properties");
                            assert !params.isEmpty() : "params.properties is empty";
                            LOGGER.debug(params);
                            fileWriter = new FileWriter(AbstractLibrary.PARAMS);
                            fileWriter.write(params);
                            init();
                            return;
                        case 2:
                            params = Slurpie.slurp(String.format("%1$s%2$ssurveyman%2$sparams.properties",
                                    System.getProperty("user.home"), AbstractLibrary.fileSep));
                            fileWriter = new FileWriter(AbstractLibrary.PARAMS);
                            fileWriter.write(params);
                            init();
                            return;
                        case 3:
                            System.exit(0);
                        default:
                            printWriter.print("Unknown selection " + selection);
                    }
                }
            } catch (IOException e) {
                Runner.LOGGER.fatal(e);
                System.err.println(e.getMessage());
                System.exit(1);
            } finally {
                scanner.close();
            }
        }catch(IOException io){
            Runner.LOGGER.fatal(io);
            System.err.println(io.getMessage());
            System.exit(1);
        }
    }

    @Override
    public List<String> getBackendHeaders()
    {
        return backendHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LocalLibrary) {
            LocalLibrary that = (LocalLibrary) o;
            Set<String> thisBackendHeaders = new HashSet<String>(this.backendHeaders);
            Set<String> thatBackendHeaders = new HashSet<String>(that.backendHeaders);
            if (thisBackendHeaders.equals(thatBackendHeaders))
                return true;
            else {
                LOGGER.debug(String.format("Backend headers not equal (%s vs. %s)",
                        StringUtils.join(thisBackendHeaders, ","),
                        StringUtils.join(thatBackendHeaders, ",")));
                return false;
            }
        } else return false;
    }

}
