package analyzer.cypher;

import analyzer.cypher.anonymized.AnonymousLabelAndNameMapper;
import org.apache.commons.collections.CollectionUtils;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.internal.ir.PatternLength;
import org.neo4j.cypher.internal.ir.PatternRelationship;
import org.neo4j.cypher.internal.ir.VarPatternLength;
import org.neo4j.cypher.internal.v4_0.expressions.RelTypeName;
import org.neo4j.cypher.internal.v4_0.expressions.SemanticDirection;

public class ParsedRelationshipBlock
{
    public final String leftNodeName;
    public final List<String> leftLabels;
    public final String rightNodeName;
    public final List<String> rightLabels;
    public final String relName;
    public final List<String> types;

    private String anonLeftNode;
    public List<String> anonLeftLabels = new ArrayList<>();
    private String anonRightNode;
    public List<String> anonRightLabels = new ArrayList<>();
    private String anonRelName;
    public List<String> anonTypes = new ArrayList<>();

    public final SemanticDirection direction;

    public int getMinLength()
    {
        return minLength;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    private int minLength = 1;
    private int maxLength = 1;

    public ParsedRelationshipBlock ( String leftNodeName, List<String> leftLabels,
            String relName, List<String> types,
            List<String> rightLabels, String rightNodeName,
            SemanticDirection direction, int minLength, int maxLength ){
        this.leftLabels = leftLabels;
        this.rightLabels = rightLabels;
        this.types = types;
        this.direction = direction;
        this.leftNodeName = leftNodeName;
        this.relName = relName;
        this.rightNodeName = rightNodeName;
        this.minLength = minLength;
        this.maxLength = maxLength;

        anonymyzeLabelsAndTypes();
    }

    public ParsedRelationshipBlock ( String leftNodeName, List<String> leftLabels,
            String relName, String type,
            List<String> rightLabels, String rightNodeName,
            SemanticDirection direction, int minLength, int maxLength ){
        List<String> types = new ArrayList<>();
        types.add( type );
        this.leftLabels = leftLabels;
        this.rightLabels = rightLabels;
        this.types = types;
        this.direction = direction;
        this.leftNodeName = leftNodeName;
        this.relName = relName;
        this.rightNodeName = rightNodeName;
        this.minLength = minLength;
        this.maxLength = maxLength;

        anonymyzeLabelsAndTypes();
    }

    public ParsedRelationshipBlock ( PatternRelationship patternRelationship, Map<String, List<String>> nodeLabelMap )
    {
        this.leftNodeName = patternRelationship.left();
        this.rightNodeName = patternRelationship.right();

        if ( nodeLabelMap.containsKey( leftNodeName ))
            leftLabels = nodeLabelMap.get( leftNodeName );
        else
            leftLabels = new ArrayList<>( );

        if ( nodeLabelMap.containsKey( rightNodeName ))
            rightLabels = nodeLabelMap.get( rightNodeName );
        else
            rightLabels = new ArrayList<>( );

        this.direction = patternRelationship.dir();
        this.relName = patternRelationship.name();
        PatternLength patternLength = patternRelationship.length();

        if ( patternLength instanceof VarPatternLength ){
            VarPatternLength varPatternLength = (VarPatternLength) patternLength;
            minLength = varPatternLength.min();
            maxLength = varPatternLength.implicitPatternNodeCount();
            if ( varPatternLength.max().toString().equals( "None" )){
                maxLength = 1000000; // RPQ's
            }

        }
        types = new ArrayList<>(  );
        for ( RelTypeName relTypeName : JavaConversions.asJavaIterable( patternRelationship.types()) )
            types.add( relTypeName.name() );


        anonymyzeLabelsAndTypes();
    }

    public String toString(){
        return
                "(" + leftNodeName + labels(leftLabels) + ")" + getLeftRelPrefix() +
                "-[" + relName + labels(types) + relCountAsString() +" ]-" + getRightRelPrefix() +
        "(" + rightNodeName + labels(rightLabels) + ")";
    }

    public String toPatternString(){
        return
                "(" + labels(leftLabels) + ")" + getLeftRelPrefix() +
                        "-[" + labels(types) + relCountAsString() +" ]-" + getRightRelPrefix() +
                        "("  + labels(rightLabels) + ")";
    }


    protected String getLeftRelPrefix()
    {
        return direction.toString().equals( "INCOMING" ) ? "<" : "";
    }

    protected String getRightRelPrefix()
    {
        return direction.toString().equals( "OUTGOING" ) ? ">" : "";
    }


    public String toAnonymyzedString(){
        return
                "(" + anonLeftNode + labels(anonLeftLabels) + ")" + getLeftRelPrefix() +
                        "-[" + anonRelName + labels(anonTypes)  + relCountAsString() +"]-" + getRightRelPrefix() +
                        "(" + anonRightNode + labels(anonRightLabels) + ")";
    }

    public String relCountAsString(){
        if ( minLength == 1 && maxLength == 1000000)
            return "*";
        else if ( minLength != maxLength ){
            if ( maxLength == 1000000 ){
                return "*"+minLength + "..*";
            }else{
                return "*"+minLength + ".." + maxLength;
            }
        }else if ( minLength == 1){
            return "";
        }else{
            return "*" + minLength;
        }
    }

    public String labels( List<String> labels ){

        if ( labels == null || labels.size() == 0 ){
            return ": ";
        } else if ( labels.size() == 1 ){
            return ":"+labels.get( 0 );
        }else {
            return ":"+labels.toString();
        }
    }

    public void anonymyzeLabelsAndTypes(){

        // Anonimyze the labels and types.
        if ( types != null )
            for ( String type : types )
                anonTypes.add( AnonymousLabelAndNameMapper.getRelType( type ) );

        if ( leftLabels != null)
            for ( String leftLabel : leftLabels )
                anonLeftLabels.add( AnonymousLabelAndNameMapper.getNodeLabel( leftLabel ) );

        if ( rightLabels != null)
            for ( String rightLabel : rightLabels )
                anonRightLabels.add( AnonymousLabelAndNameMapper.getNodeLabel( rightLabel ) );

        // Anonimyze the names
        anonLeftNode = AnonymousLabelAndNameMapper.getNodeName( leftNodeName );
        anonRightNode = AnonymousLabelAndNameMapper.getNodeName( rightNodeName );
        anonRelName = AnonymousLabelAndNameMapper.getRelName( relName );

    }

    @Override
    public boolean equals(Object obj){
        if ( ! (obj instanceof  ParsedRelationshipBlock )){
            return false;
        }
        ParsedRelationshipBlock otherBlock = (ParsedRelationshipBlock) obj;

        return otherBlock.minLength == minLength &&
                otherBlock.maxLength == maxLength &&
                otherBlock.direction == direction &&
                CollectionUtils.isEqualCollection(leftLabels, otherBlock.leftLabels) &&
                CollectionUtils.isEqualCollection(rightLabels, otherBlock.rightLabels) &&
                CollectionUtils.isEqualCollection(types, otherBlock.types);

    }

    public String hashableString(){
        return labels(leftLabels) + direction + minLength + maxLength + labels( rightLabels ) + labels(types);
    }
}
