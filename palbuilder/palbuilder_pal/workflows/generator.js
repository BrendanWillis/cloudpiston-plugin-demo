//@include("lib");
var c;
var page;
var pal;
var payload;
var request;
var packet;

function run(controller)
{
    c = controller;
    c.getMonitor().setMaxTimeout();
    page = c.getPage("main");
    pal = c.getPal();
    payload = c.createPayload();
    request = c.getRequest();

    payload.set("experimentMessage", "Hello from generator.js");

    packet = c.getUser().getProfile().getPacket();
    if (packet == null)
    {
        packet = c.getUser().getProfile().createPacket();
    }

    switch (c.getAction())
    {
        default:
        case "startOver":
            payload.setValue("fragment", "inputBar");
            break;
        
        case "showScriptureSearch":
            payload.setValue("fragment", "scriptureSearch");
            break;
        
        case "searchScriptures":
            var scriptureData = request.getData();
        
            payload.set("scriptureSearchWord", scriptureData.get("scriptureSearchWord"));
            payload.set("scriptureMaxResults", scriptureData.get("scriptureMaxResults"));
            payload.set("scriptureSecondWord", scriptureData.get("scriptureSecondWord"));
            payload.set("scriptureDistance", scriptureData.get("scriptureDistance"));
        
            c.debug("Scripture search word = " + scriptureData.get("scriptureSearchWord"));
            c.debug("Scripture max results = " + scriptureData.get("scriptureMaxResults"));
        
            payload.setValue("fragment", "scriptureSearch");
            break;

        case "clearTimingLog":
            packet.setDataList(c.createDataList("timingLog", [
                "run",
                "items",
                "javaTime",
                "roundTripTime",
                "jsTime"
            ]));

            payload.addDataList(packet.getDataList("timingLog"));
            payload.setValue("fragment", "inputBar");
            break;
        case "submit2":
        case "submit":
            var data = request.getData();
            var items = data.get("items");
            c.debug("FORM age = " + data.get("age"));
            c.debug("FORM city = " + data.get("city"));
            c.debug("FORM state = " + data.get("state"));
            c.debug("FORM email = " + data.get("email"));

            payload.set("selectedGender", data.get("gender") != null ? "Yes" : "No");
            payload.set("selectedAge", data.get("age") != null ? "Yes" : "No");
            payload.set("selectedBirthDate", data.get("birthDate") != null ? "Yes" : "No");
            payload.set("selectedAddress", data.get("address") != null ? "Yes" : "No");
            payload.set("selectedCity", data.get("city") != null ? "Yes" : "No");
            payload.set("selectedState", data.get("state") != null ? "Yes" : "No");
            payload.set("selectedSSN", data.get("ssn") != null ? "Yes" : "No");
            payload.set("selectedPhone", data.get("phoneNumber") != null ? "Yes" : "No");
            payload.set("selectedZipCode", data.get("zipCode") != null ? "Yes" : "No");
            payload.set("selectedEmail", data.get("email") != null ? "Yes" : "No");
            payload.set("old", c.isAction("submit")?"true":"false");
            payload.set("experimentItems", items);
            payload.set("userEmail",c.getUser().getProfile().getEmailAddress());

            if (data.get("showDebugInfo") != null)
            {
                payload.setBoolean("showDebugInfo", true);
            }

            var plugin = pal.getPluginSocket("Brendan");
            plugin.setController(c);
            plugin.setPayload(payload);

            var pluginStart = new Date().getTime();
            var result = plugin.submit();
            var pluginEnd = new Date().getTime();

            var pluginResult = result.readBody();

            payload.set("pluginStatus", "Online");
            payload.set("pluginRoundTripTime", pluginEnd - pluginStart);
            payload.set("pluginResult", pluginResult);
            c.debug(payload);
            var jsStart = new Date().getTime();
            data.set("userEmail",c.getUser().getProfile().getEmailAddress());
            createList(data);
            var jsEnd = new Date().getTime();

            var jsTime = jsEnd - jsStart;
            payload.set("javaScriptGeneratorTime", jsTime);

            addTimingLog(
                items,
                payload.get("javaGeneratorTime"),
                payload.get("pluginRoundTripTime"),
                jsTime
            );

            payload.setValue("fragment", "listDisplay");
            break;

        case "download":
            return downloadList();
    }

    page.addPayload(payload);
    return page;
}

function addTimingLog(items, javaTime, roundTripTime, jsTime)
{
    var timingLog = packet.getDataList("timingLog");

    if (timingLog == null)
    {
        timingLog = c.createDataList("timingLog", [
            "run",
            "items",
            "javaTime",
            "roundTripTime",
            "jsTime"
        ]);
    }

    var runNumber = timingLog.getRecordCount() + 1;

    var rec = timingLog.insertRecord();
    rec.setDataValue("run", runNumber);
    rec.setDataValue("items", items);
    rec.setDataValue("javaTime", javaTime);
    rec.setDataValue("roundTripTime", roundTripTime);
    rec.setDataValue("jsTime", jsTime);

    packet.setDataList(timingLog);
    payload.addDataList(timingLog);
}

function downloadList()
{
    var response = c.createDownloadResponse();
    var list = packet.getDataList("newNames");
    var file = list.toFile("newNames.csv", "csv");
    response.setFileContent(file);
    return response;
}
