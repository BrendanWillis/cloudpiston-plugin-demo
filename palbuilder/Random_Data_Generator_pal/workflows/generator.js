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
            
        case "getChart":
            page=c.getPage("D3");
            break;
        
        case "generateChart":
            generateChart();
            payload.setValue("fragment", "chartSearch");
            break;
        
        case "showchartSearch":
            payload.setValue("fragment", "chartSearch");
            break;
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

    payload.set("old", "false");

    var plugin = pal.getPluginSocket("Brendan");
    plugin.setController(c);
    plugin.setPayload(payload);

    var pluginStart = new Date().getTime();
    var result = plugin.submit();
    var pluginEnd = new Date().getTime();

    var pluginResult = result.readBody();
    var roundTripTime = pluginEnd - pluginStart;

    payload.set("pluginStatus", "Online");
    payload.set("pluginResult", pluginResult);
    payload.set("pluginRoundTripTime", roundTripTime + " ms");

    payload.set("javaGeneratorTime", payload.get("javaGeneratorTime") + " ms");

    payload.set("javaScriptGeneratorTime", "Not run");
    payload.set("javaScriptResult", "JavaScript generator was not used for this run.");

    addTimingLog(
        items,
        payload.get("javaGeneratorTime"),
        payload.get("pluginRoundTripTime"),
        "Not run"
    );
    payload.addData(data);
    c.debug(payload);
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
    payload.set("javaGeneratorTime", "Not run");
    payload.set("pluginRoundTripTime", "Not run");
    payload.set("pluginResult", "Java plugin was not used for this run.");
    payload.set("pluginMessage", "JavaScript generator ran by itself.");

    payload.set("javaScriptGeneratorTime", jsTime + " ms");
    payload.set("javaScriptResult", "JavaScript generator completed.");
    
    
    payload.addData(data);
    c.debug(payload);

    addTimingLog(
        items,
        "Not run",
        "Not run",
        jsTime + " ms"
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
    payload.set("scriptureRoundTripTime", (scriptureEnd - scriptureStart) + " ms");
    
    c.debug(payload);
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

function testChart ()
{
    return getChart;
}

function generateChart()
{
    // Get the data the user typed into the PAL page.
    var chartData = request.getData();

    // Tell Brendan.java to route this request to ChartBuilder.
    payload.set("module", "charts");

    // Send the searched word to Java.
    // This key must match Brendan.java: payload.get("chartSearchWord")
    payload.set("chartSearchWord", chartData.get("chartSearchWord"));

    // Get the Java plugin socket.
    var chartPlugin = pal.getPluginSocket("Brendan");

    // Give the plugin access to the current controller.
    chartPlugin.setController(c);

    // Give the plugin the payload containing chartSearchWord and module.
    chartPlugin.setPayload(payload);

    // Track how long the Java plugin round trip takes.
    var chartStart = new Date().getTime();

    // Run the Java plugin.
    var chartResult = chartPlugin.submit();

    var chartEnd = new Date().getTime();

    // Read the message returned by Brendan.java.
    var chartResultText = chartResult.readBody();

    // Store result info so the PAL page can display it.
    payload.set("chartPluginResult", chartResultText);
    payload.set("chartRoundTripTime", (chartEnd - chartStart) + " ms");

    // Print payload to debug so we can inspect chartData.
    c.debug(payload);
}
