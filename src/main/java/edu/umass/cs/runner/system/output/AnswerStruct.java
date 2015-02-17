package edu.umass.cs.runner.system.output;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AnswerStruct {
    public final List<AnswerQuad> answerQuads;

    public AnswerStruct(List<AnswerQuad> answerQuads) {
        this.answerQuads = answerQuads;
    }

    public String jsonize()
    {
        List<String> strings = new ArrayList<String>();
        for (AnswerQuad answerQuad: this.answerQuads)
            strings.add(answerQuad.jsonize());
        return String.format("[ %s ]", StringUtils.join(strings, ", "));
    }
}
