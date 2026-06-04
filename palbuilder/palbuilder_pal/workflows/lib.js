function createList(data)
{
    var namesNumber = data.getDefaultInt("items",0); //number of names the user wants
    var min = 0;
    var max = 10000;
    
    //checks to make sure that the input is between 1 and 500
    if (namesNumber <= min || namesNumber > max)
    {
        payload.set("error","input not valid. Please enter a number between 1 and 500.");
        payload.set("fragment", "inputBar");
    }
    else
    {
        var list = createRandomList(namesNumber,data);
        columnsList(data);
        payload.addDataList(list);
        payload.setValue("fragment", "listDisplay");
        payload.addDataList(list);
        return list;
    }
}


//this function creates of datalist of all column headings needed, and determines whether a column is being used. used to properly display table on page.
function columnsList(data)
{
    var columns = c.createDataList("columns", ["column"]);
    
    var gender = data.get("gender");
    var age = data.get("age");
    var birthDate = data.get("birthDate");
    var address = data.get("address");
    var city = data.get("city");
    var state = data.get("state");
    var ssn = data.get("ssn");
    var phoneNumber = data.get("phoneNumber");
    var zipCode = data.get("zipCode");
    var email = data.get("email");
    
    columns.insertRecord().setDataValue("column", "First Name");
    columns.insertRecord().setDataValue("column", "Last Name");
    
    if (gender != null)
    {
        columns.insertRecord().setDataValue("column", "Gender");
        payload.setBoolean("gender", true);
    }
    if (age != null)
    {
        columns.insertRecord().setDataValue("column", "Age");
        payload.setBoolean("age", true);
    }
    if (birthDate != null)
    {
        columns.insertRecord().setDataValue("column", "Birth Date");
        payload.setBoolean("birthDate", true);
    }
    if (address != null)
    {
        columns.insertRecord().setDataValue("column", "Address");
        payload.setBoolean("address", true);
    }
    if (city != null)
    {
        columns.insertRecord().setDataValue("column", "City");
        payload.setBoolean("city", true);
    }
    if (state != null)
    {
        columns.insertRecord().setDataValue("column", "State");
        payload.setBoolean("state", true);
    }
    if (zipCode != null)
    {
        columns.insertRecord().setDataValue("column", "Zip Code");
        payload.setBoolean("zipCode", true);
    }
    if (phoneNumber != null)
    {
        columns.insertRecord().setDataValue("column", "Phone Number");
        payload.setBoolean("phoneNumber", true);
    }
    if (ssn != null)
    {
        columns.insertRecord().setDataValue("column", "SSN");
        payload.setBoolean("ssn", true);
    }
    if (email != null)
    {
        columns.insertRecord().setDataValue("column", "Email");
        payload.setBoolean("email", true);
    }
    payload.addDataList(columns);
}

function createRandomList(namesNumber,data)
{
    var generatedList = c.createDataList("newNames", ["first", "last"]); //the datalist that will be displayed to the user
    var firstList = pal.getDataList("firstNames"); //datalist of first names
    var lastList = pal.getDataList("lastNames"); //datalist of last names
    var keys = c.createData();
    var firstRows = firstList.getRecordCount(); // number of rows in firstNames datalist(used to choose random item from datalist)
    var lastRows = lastList.getRecordCount(); // number of rows in lastNames datalist(used to choose random item from datalist)
    //chooses a random first and last name for the number of names the user wants, then adds them to generatedList    
    for (var i = 0; i < namesNumber; i++)
    {
        do
        {
            var firstNameNum = Math.floor(Math.random() * firstRows);
            var firstName = firstList.getRecord(firstNameNum).get("firstName");
            var gender = firstList.getRecord(firstNameNum).get("gender");
            
            var lastName = lastList.getRecord(Math.floor(Math.random() * lastRows)).get("lastName");
            var fullName = firstName + " " + lastName;
            
            var test = keys.getValue(fullName);
        } while (test != null);
        
        keys.setValue(fullName, fullName);
        
        var rec = generatedList.insertRecord();
        rec.setDataValue("first", firstName);
        rec.setDataValue("last", lastName);
        
        if (data.getValue("gender") != null)
        {
            generatedList.addColumn("gender");
            rec.setDataValue("gender", gender);
        }
    }
    generatedList = additionalInfo(generatedList,data, namesNumber);
    
    return generatedList;
}

function additionalInfo(datalist,data, num)
{
    var age = data.get("age");
    var birthDate = data.get("birthDate");
    var address = data.get("address");
    var city = data.get("city");
    var state = data.get("state");
    var ssn = data.get("ssn");
    var phoneNumber = data.get("phoneNumber");
    var zipCode = data.get("zipCode");
    var email = data.get("email");
    
    if (age != null)
    {
        datalist.addColumn("age");
        for (var i = 0; i < num; i++)
        {
            var newAge = Math.floor(Math.random() * 100); //gives a random age between 0 and 100
            datalist.getRecord(i).setInt("age", newAge);
        }
    }
    if (birthDate != null)
    {
        datalist.addColumn("birthDate");
        for (var i = 0; i < num; i++)
        {
            var month = Math.floor(Math.random() * 12) + 1;
            if (month == 4 || month == 6 || month == 9 || month == 11)
            {
                var day = Math.floor(Math.random() * 30) + 1;
            }
            else if (month == 2)
            {
                var day = Math.floor(Math.random() * 28) + 1;
            }
            else
            {
                var day = Math.floor(Math.random() * 31) + 1;
            }
            var year = Math.floor(Math.random() * 95) + 1920; //generates a year between 1920 and 2015
            var newBirthDate = month + "/" + day + "/" + year;
            datalist.getRecord(i).setDataValue("birthDate", newBirthDate);
            
            //if the user wants birthDate and age, this produces an age that goes with the generated Birthdate, replacing the previous randomly generated age.
            if (age != null)
            {
                newBirthDate = c.getDateUtil().parseDate(newBirthDate);
                var newAge = c.getDateUtil().getAge(newBirthDate);
                datalist.getRecord(i).setInt("age", newAge);
            }
        }
    }
    if (address != null)
    {
        datalist.addColumn("address");
        var streetList = pal.getDataList("streets");
        var streetListRows = streetList.getRecordCount(); // number of rows in streets datalist
        for (var i = 0; i < num; i++)
        {
            var houseNum = Math.floor(Math.random() * 3000) + 1; // gives a random house number between 1 and 3,000
            var street = streetList.getRecord(Math.floor(Math.random() * streetListRows)).get("streets");
            var newAddress = houseNum + street;
            datalist.getRecord(i).setDataValue("address", newAddress);
        }
    }
    if (city != null)
    {
        var cityList = pal.getDataList("cities");
        var cityListRows = cityList.getRecordCount(); // number of rows in city datalist
        datalist.addColumn("city");
        for (var i = 0; i < num; i++)
        {
            var newCity = cityList.getRecord(Math.floor(Math.random() * cityListRows)).get("cities");
            datalist.getRecord(i).setDataValue("city", newCity);
        }
    }
    if (state != null)
    {
        datalist.addColumn("state");
        for (var i = 0; i < num; i++)
        {
            var newState = c.getBusinessUtil().getStateList("US").getRecord(Math.floor(Math.random() * 50)).get("name");
            datalist.getRecord(i).setDataValue("state", newState);
        }
    }
    if (ssn != null)
    {
        datalist.addColumn("ssn");
        for (var i = 0; i < num; i++)
        {
            var newSsn = "";
            for (var j = 0; j < 3; j++)
            {
                var newNum = Math.floor(Math.random() * 10).toString();
                newSsn = newSsn + newNum;
            }
            newSsn = newSsn + "-";
            for (var k = 0; k < 2; k++)
            {
                var newNum = Math.floor(Math.random() * 10).toString();
                newSsn = newSsn + newNum;
            }
            newSsn = newSsn + "-";
            for (var l = 0; l < 4; l++)
            {
                var newNum = Math.floor(Math.random() * 10).toString();
                newSsn = newSsn + newNum;
            }
            datalist.getRecord(i).setDataValue("ssn", newSsn);
        }
    }
    if (phoneNumber != null)
    {
        datalist.addColumn("phoneNumber");
        var phones=data.getDefaultValue("phones",null,true);
        var parts=phones==null?null:phones.split(",");
        var x=0;
        for (var i = 0; i < num; i++)
        {
            var newPhone = "";
            if (phones==null)
            {
                for (var j = 0; j < 2; j++)
                {
                    for (var k = 0; k < 3; k++)
                    {
                        var newNum = Math.floor(Math.random() * 10).toString();
                        newPhone = newPhone + newNum;
                    }
                    newPhone = newPhone + "-";
                }
                for (var l = 0; l < 4; l++)
                {
                    var newNum = Math.floor(Math.random() * 10).toString();
                    newPhone = newPhone + newNum;
                }
            }
            else
            {
                newPhone=parts[x];
                x++;
                if (x>parts.length-1)
                {
                    x=0;
                }
            }
            datalist.getRecord(i).setDataValue("phoneNumber", newPhone);
        }
    }
    if (zipCode != null)
    {
        datalist.addColumn("zipCode");
        for (var i = 0; i < num; i++)
        {
            var newZipCode = "";
            for (var j = 0; j < 5; j++)
            {
                var newNum = Math.floor(Math.random() * 10).toString();
                newZipCode = newZipCode + newNum;
            }
            datalist.getRecord(i).setDataValue("zipCode", newZipCode); 
        }
    }
    if (email != null)
    {
        datalist.addColumn("email");
        var userEmail=data.get("userEmail");
        if (userEmail)
        {
            for (var i = 0; i < num; i++)
            {
                var m = userEmail;
                m=m.replace("@","+"+i+"@");
                datalist.getRecord(i).setDataValue("email", m); 
            }
        }
    }
    
    
    
    
    return datalist;
}

