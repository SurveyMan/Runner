package edu.umass.cs.runner.system.output;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyResponseStruct {
    public final String primaryResponseId;
    public final AnswerStruct answerStruct;
    public final double score;
    public final double threshold;
    public final boolean classification;
    public final Map<String, String> otherValues;

    public SurveyResponseStruct(
            String primaryResponseId,
            AnswerStruct answerStruct,
            double score,
            double threshold,
            boolean classification,
            Map<String, String> otherValues
            )
    {
        this.primaryResponseId = primaryResponseId;
        this.answerStruct = answerStruct;
        this.score = score;
        this.threshold = threshold;
        this.classification = classification;
        this.otherValues = otherValues;
    }

    public SurveyResponseStruct(
            String primaryResponseId,
            AnswerStruct answerStruct,
            double score,
            double threshold,
            boolean classification)
    {
        this(primaryResponseId, answerStruct, score, threshold, classification, new HashMap<String, String>());
    }

    public String jsonize()
    {
        List<String> strings = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.otherValues.entrySet()) {
            strings.add(String.format("\"%s\" : \"%s\"", entry.getKey(), entry.getValue()));
        }
        String other = "";
        if (!strings.isEmpty())
            other = ", { " + StringUtils.join(strings, ", ") + "}";
        return String.format("{ " +
                "\"srid\" : \"%s\", " +
                "\"answers\"  : %s, " +
                "\"score\" : %f, " +
                "\"threshold\" : %f, " +
                "\"classification\" : %b" +
                " %s" +
                "}",
                this.primaryResponseId,
                this.answerStruct.jsonize(),
                this.score,
                this.threshold,
                this.classification,
                other
                );
    }
}
