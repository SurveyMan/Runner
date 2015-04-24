(function () {
    $.getJSON("?json/survey_data", function (data) {
        console.log(data);
        survey = JSON.parse(data["survey"]);
        $("#surveyfilename").append(survey.filename);
        $("#backend").append(data["backend"]);
        $("#targetresponses").append(data["targetresponses"]);
        $("#expectedcost").append("$" + data["expectedcost"]);
        $("#currentcost").append("$0.00");
        $("#classificationmethod").append(data["classificationmethod"]);
    });
})();

var updateResponses = function () {
    $.getJSON("?json/response_data", function (data) {
        console.log(data);
        var table = $("#responses");
        var i, j;
        for (i = 0 ; i < data.length ; i++) {
            var responseJSON = data[i],
                id = responseJSON["srid"],
                row = $("#srid_"+id),
                pairs = _.pairs(responseJSON),
                header = $("#responseheader"),
                headerwritten = (header.length != 0);
            // check if we have the header row
            if (! headerwritten)
                table.append("<tr id=\"responseheader\"></tr>");
            if (row.length === 0) {
                table.append("<tr id=\"" + id + "\"></tr>");
                row = $("#"+id);
            }
            for (j = 0 ; j < _.size(pairs) ; j++) {
                var key = pairs[j][0],
                    value = pairs[j][1];
                if (key === "answers")
                    continue;
                if (! headerwritten)
                    $("#responseheader").append("<td id=\"" + key + "\"><b>" + key + "</b></td>");
                var cell = $("#"+id+key);
                if (cell.length === 0) {
                    row.append("<td id=\"" + id + key + "\"></td>");
                    cell = $("#" + id + key);
                }
                cell.html(value);
            }
        }
    });
}

updateResponses();