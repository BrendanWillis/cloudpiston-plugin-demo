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
            searchScriptures();
            payload.setValue("fragment", "scriptureSearch");
            break;

        case "clearTimingLog":
            clearTimingLog();
            payload.setValue("fragment", "inputBar");
            break;

        case "submitJava":
            runJavaGenerator();
            payload.setValue("fragment", "listDisplay");
            break;

        case "submitJavaScript":
            runJavaScriptGenerator();
            payload.setValue("fragment", "listDisplay");
            break;

        case "download":
            return downloadList();
    }

    page.addPayload(payload);
    return page;
}

function setupRandomPayload(data)
{
    var items = data.get("items");

    payload.set("module", "random");

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

    payload.set("experimentItems", items);
    payload.set("userEmail", c.getUser().getProfile().getEmailAddress());

    return items;
}

function runJavaGenerator()
{
    var data = request.getData();
    var items = setupRandomPayload(data);

    payload.set("old", "true");

    var plugin = pal.getPluginSocket("Brendan");
    plugin.setController(c);
    plugin.setPayload(payload);

    var pluginStart = new Date().getTime();
    var result = plugin.submit();
    var pluginEnd = new Date().getTime();

    var pluginResult = result.readBody();

    payload.set("pluginStatus", "Online");
    payload.set("javaPluginResult", pluginResult);
    payload.set("pluginResult", pluginResult);
    payload.set("pluginRoundTripTime", pluginEnd - pluginStart);
    payload.set("javaScriptGeneratorTime", "");

    addTimingLog(
        items,
        payload.get("javaGeneratorTime"),
        payload.get("pluginRoundTripTime"),
        ""
    );
}

function runJavaScriptGenerator()
{
    var data = request.getData();
    var items = setupRandomPayload(data);

    data.set("userEmail", c.getUser().getProfile().getEmailAddress());

    var jsStart = new Date().getTime();
    createList(data);
    var jsEnd = new Date().getTime();

    var jsTime = jsEnd - jsStart;

    payload.set("pluginStatus", "Not used");
    payload.set("pluginRoundTripTime", "");
    payload.set("javaGeneratorTime", "");
    payload.set("pluginResult", "Java plugin was not used.");
    payload.set("javaPluginResult", "Java plugin was not used.");
    payload.set("javaScriptGeneratorTime", jsTime);
    payload.set("javaScriptResult", "JavaScript generator completed.");

    addTimingLog(
        items,
        "",
        "",
        jsTime
    );
}

function searchScriptures()
{
    var scriptureData = request.getData();

    payload.set("module", "scriptures");

    payload.set("scriptureSearchWord", scriptureData.get("scriptureSearchWord"));
    payload.set("scriptureMaxResults", scriptureData.get("scriptureMaxResults"));
    payload.set("scriptureSecondWord", scriptureData.get("scriptureSecondWord"));
    payload.set("scriptureDistance", scriptureData.get("scriptureDistance"));

    var scripturePlugin = pal.getPluginSocket("Brendan");
    scripturePlugin.setController(c);
    scripturePlugin.setPayload(payload);

    var scriptureStart = new Date().getTime();
    var scriptureResult = scripturePlugin.submit();
    var scriptureEnd = new Date().getTime();

    var scriptureResultText = scriptureResult.readBody();

    payload.set("scripturePluginResult", scriptureResultText);
    payload.set("scriptureRoundTripTime", scriptureEnd - scriptureStart);
}

function clearTimingLog()
{
    packet.setDataList(c.createDataList("timingLog", [
        "run",
        "items",
        "javaTime",
        "roundTripTime",
        "jsTime"
    ]));

    payload.addDataList(packet.getDataList("timingLog"));
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