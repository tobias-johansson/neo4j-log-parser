package analyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogFileCollector
{

    public static Map<String, List<String>> getAllFilesInFolder( Map<String,List<String>> fileNames, String folderLocation ){
        File folder = new File( folderLocation );
        File[] listOfFiles = folder.listFiles();

        for ( File listOfFile : listOfFiles )
            if ( listOfFile.isFile() )
            {
                if ( listOfFile.getName().contains( "query.log" ) && !listOfFile.getName().endsWith( ".zip" ))
                {
                    if ( !fileNames.containsKey(  folderLocation  )){
                        fileNames.put( folderLocation, new ArrayList<>());
                    }
                    fileNames.get( folderLocation ).add (listOfFile.getAbsolutePath());
                }
            }else{
                fileNames.putAll( getAllFilesInFolder( fileNames, listOfFile.getAbsolutePath() ) );
            }


        return fileNames;
    }
}