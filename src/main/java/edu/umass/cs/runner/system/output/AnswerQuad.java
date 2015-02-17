package edu.umass.cs.runner.system.output;

public class AnswerQuad {
    public final String quid;
    public final String oid;
    public final int qindexseen;
    public final int oindexseen;

    public AnswerQuad(
            String quid,
            String oid,
            int qindexseen,
            int oindexseen)
    {
        this.qindexseen = qindexseen;
        this.oid = oid;
        this.oindexseen = oindexseen;
        this.quid = quid;
    }

    public String jsonize()
    {
        return String.format("{ \"quid\" : \"%s\", \"oid\" : \"%s\", \"qindexseen\" : %d, \"oindexseen\" : %d}",
                this.quid,
                this.oid,
                this.qindexseen,
                this.oindexseen
                );
    }

}
