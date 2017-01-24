package com.zenjava.test;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.Stage;

public class Main extends javafx.application.Application {

    public static void main(String[] args) throws IOException{
        if( args != null ){
            appendToLogFile("gotArgs: " + String.join(", ", Arrays.asList(args)));
        }
        Main.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        printMap(getParameters().getNamed(), "named args within jfx > ");
        printList(getParameters().getRaw(), "raw args within jfx");
        printList(getParameters().getUnnamed(), "unnamed args within jfx");

        System.exit(0);
    }

    public static void appendToLogFile(String something) throws IOException {
        if( something != null ){
            java.nio.file.Files.write(java.nio.file.Paths.get("test.txt"), something.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
    }

    public static void printMap(Map map, String linePrefix) throws IOException {
        for( Iterator it = map.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry) it.next();
            appendToLogFile("\n" + linePrefix + " > " + entry.getKey() + " = " + entry.getValue());
        }
    }

    public static void printList(List<String> someList, String linePrefix) {
        someList.forEach((String value) -> {
            try{
                appendToLogFile("\n" + linePrefix + " > " + value);
            } catch(IOException ex){
                // NO-OP
                ex.printStackTrace();
            }
        });
    }

}
