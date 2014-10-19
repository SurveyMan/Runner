package edu.umass.cs.runner.system.localhost;

import edu.umass.cs.runner.Library;
import edu.umass.cs.runner.Runner;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LocalLibrary extends Library {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

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
    public void init() {
        try{
            super.props = new Properties();
            super.props.load(new FileReader(Library.PARAMS));
        }catch(IOException io){
            Runner.LOGGER.fatal(io);
            System.err.println(io.getMessage());
            System.exit(1);
        }
    }
}
