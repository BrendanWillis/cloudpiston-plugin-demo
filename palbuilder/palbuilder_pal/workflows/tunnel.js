//@include("lib");
var c;
var payload;
var request;
var pal;

function run(controller)
{
	c = controller;
    pal=c.getPal();
    request=c.getRequest();
    payload=c.createPayload();
    
    switch(request.getAction())
    {
        case "getRecords":
            createList(request.getPayload().getData());
            break;
    }
    var resp=c.getResponse();
    resp.setPayload(payload);
    return resp;
}
