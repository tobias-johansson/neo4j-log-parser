package parser;

import cypher.anonymized.AnonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an entire query log. Uses a Cypher query parser.
 */
public class QueryLogParser
{

    CypherQueryParser parser = new CypherQueryParser();

    // Returns, for each unique neo4j database, the queries in the log, differentiated by their cypher string.
    @Deprecated
    public Map<String,Map<String,List<QueryLogEntry>>> parseAndMapQueries( Map<String, List<String>> queryStringsPerFolder ){
        Map<String, Map<String, List<QueryLogEntry>>> queriesByFolder = new HashMap<>();

        for ( Map.Entry<String, List<String>> folderAndQueryStrings : queryStringsPerFolder.entrySet() )
        {
            Map<String, List<QueryLogEntry>> queriesByCypherString =
                    parseAllQueriesInSingleFolder( folderAndQueryStrings.getKey(), folderAndQueryStrings.getValue() );
            queriesByFolder.put( folderAndQueryStrings.getKey(), queriesByCypherString );
        }

        return queriesByFolder;
    }

    public Map<String, List<QueryLogEntry>> parseAllQueriesInSingleFolder( String folder, List<String> lines )
    {
        parser.cache = new HashMap<>();
        int size = lines.size();
        AnonMapper.resetForLabels();
        System.out.println( "[PARSER] Parsing queries in folder: " + folder + " (log_size = " + size + ")");
        Map<String, List<QueryLogEntry>> queriesMappedByCypherString = new HashMap<>();
        int counter = 0;

        for ( String singleQueryString : lines )
        {
            // Just to check the progress we keep a counter.
            counter = updateCounter( size, counter );

            QueryLogEntry query = parser.parse( folder, singleQueryString );
            if( query.cypherQuery != null)
            {
                if ( !queriesMappedByCypherString.containsKey( query.cypherQuery ) )
                {
                    queriesMappedByCypherString.put( query.cypherQuery, new ArrayList<>() );
                }
                queriesMappedByCypherString.get( query.cypherQuery ).add( query );
            }
        }
        return queriesMappedByCypherString;
    }

    private int updateCounter( int size, int counter )
    {
        counter++;
        if ( counter % 10000 == 0 ){
            System.out.println( "[PARSER] " + counter + "/" + size + " queries parsed...");
        }
        return counter;
    }
}