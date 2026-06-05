package org.brendan;

import com.nxlight.framework.pal.workflow.common.*;
import com.nxlight.framework.pal.workflow.plugin.Plugin;

import java.util.Date;

public class Brendan extends Plugin {

    private static final int MIN_ITEMS = 1;
    private static final int MAX_ITEMS = 1000;

    public String execute() throws WorkflowException {

        int items = getSafeItemCount(payload.get("experimentItems"));

        ScriptureSearch scriptureSearch =
                new ScriptureSearch(controller);

//        scriptureSearch.testResource();
//        scriptureSearch.listXhtmlFiles();
//        scriptureSearch.searchWord("faith", 10);
        String searchWord = payload.get("scriptureSearchWord");
        String maxResultsText = payload.get("scriptureMaxResults");

        String secondWord = payload.get("scriptureSecondWord");
        String distanceText = payload.get("scriptureDistance");

        int maxResults = 10;

        try {
            maxResults = Integer.parseInt(maxResultsText);
        }
        catch (Exception e) {
            maxResults = 10;
        }

        int distance = 0;
        try {
            distance = Integer.parseInt(distanceText);
        }
        catch (Exception e) {
            distance = 0;
        }
        if (searchWord != null && searchWord.length()> 0) {
            if (secondWord != null && secondWord.length() > 0) {

                scriptureSearch.searchProximity( searchWord, secondWord, distance, maxResults);
            } else {
                scriptureSearch.searchWord(searchWord, maxResults);
            }
        }

        String requestSize;

        if (items >= 1000) {
            requestSize = "Max-size request";
        } else if (items > 100) {
            requestSize = "Large request";
        } else {
            requestSize = "Small request";
        }

        payload.set("requestSize", requestSize);
        payload.set("pluginMessage", "Plugin analyzed " + items + " requested records.");

        controller.debug("Plugin Health Check: Online");
        controller.debug("Items received by plugin: " + items);
        controller.debug("Request classification: " + requestSize);
        controller.debug("Second word = " + secondWord);
        controller.debug("Distance = " + distance);

        long javaStart = System.currentTimeMillis();
        if (payload.getData().getDefaultBoolean("old",true))
        {
            payload.addDataList(createRandomList1(items, payload.getData()));

        }
        else
        {
            payload.addDataList(createRandomList2(items, payload.getData()));

        }

        long javaEnd = System.currentTimeMillis();
        long javaGeneratorTime = javaEnd - javaStart;

        payload.set("javaGeneratorTime", String.valueOf(javaGeneratorTime));

        return "Plugin completed analysis for " + items + " records.";
    }

    private int getSafeItemCount(String message) {

        int items;

        try {
            items = Integer.parseInt(message);
        } catch (Exception e) {
            items = MIN_ITEMS;
        }

        if (items < MIN_ITEMS) {
            items = MIN_ITEMS;
        }

        if (items > MAX_ITEMS) {
            items = MAX_ITEMS;
        }

        return items;
    }

    private DataList createRandomList1(int namesNumber, Data data) throws WorkflowException {

        PacketDataList generatedList = controller.createDataList(
                "newNames",
                new String[]{"first", "last"}
        );

        boolean includeGender = data.getValue("gender") != null;

        if (includeGender) {
            generatedList.addColumn("gender");
        }

        RandomName random = new RandomName (controller, namesNumber);

        for (int i = 0; i < namesNumber; i++) {


            Person person= random.getfullname1();


            PacketDataRecord rec = generatedList.insertRecord();

            rec.setDataValue("first", person.firstName);
            rec.setDataValue("last", person.lastName);

            if (includeGender) {
                rec.setDataValue("gender", person.gender);
            }
        }

        return additionalInfo(generatedList, data, namesNumber);
    }
    private DataList createRandomList2(int namesNumber, Data data) throws WorkflowException {

        PacketDataList generatedList = controller.createDataList(
                "newNames",
                new String[]{"first", "last"}
        );

        boolean includeGender = data.getValue("gender") != null;

        if (includeGender) {
            generatedList.addColumn("gender");
        }

        RandomName random = new RandomName (controller, namesNumber);

        for (int i = 0; i < namesNumber; i++) {


            Person person= random.getfullname2();


            PacketDataRecord rec = generatedList.insertRecord();

            rec.setDataValue("first", person.firstName);
            rec.setDataValue("last", person.lastName);

            if (includeGender) {
                rec.setDataValue("gender", person.gender);
            }
        }

        return additionalInfo(generatedList, data, namesNumber);
    }
    private PacketDataList additionalInfo(PacketDataList datalist, Data data, int num)
            throws WorkflowException {

        String age = data.get("selectedAge");
        String birthDate = data.get("selectedBirthDate");
        String address = data.get("selectedAddress");
        String city = data.get("selectedCity");
        String state = data.get("selectedState");
        String ssn = data.get("selectedSSN");
        String phoneNumber = data.get("selectedPhone");
        String zipCode = data.get("selectedZipCode");
        String email = data.get("selectedEmail");

        if ("Yes".equals(age)) {
            addAge(datalist, num);
        }

        if ("Yes".equals(birthDate)) {
            addBirthDate(datalist, num, "Yes".equals(age));
        }

        if ("Yes".equals(address)) {
            addAddress(datalist, num);
        }

        if ("Yes".equals(city)) {
            addCity(datalist, num);
        }

        if ("Yes".equals(state)) {
            addState(datalist, num);
        }

        if ("Yes".equals(ssn)) {
            addSsn(datalist, num);
        }

        if ("Yes".equals(phoneNumber)) {
            addPhoneNumber(datalist, data, num);
        }

        if ("Yes".equals(zipCode)) {
            addZipCode(datalist, num);
        }

        if ("Yes".equals(email)) {
            addEmail(datalist, data, num);
        }

        return datalist;
    }

    private void addAge(PacketDataList datalist, int num) throws WorkflowException {

        datalist.addColumn("age");

        for (int i = 0; i < num; i++) {

            int newAge = randomInt(100);

            datalist.getRecord(i).setInt("age", newAge);
        }
    }

    private void addBirthDate(PacketDataList datalist, int num, boolean updateAge)
            throws WorkflowException {

        datalist.addColumn("birthDate");

        for (int i = 0; i < num; i++) {

            int month = randomInt(12) + 1;
            int day = getRandomDayForMonth(month);
            int year = randomInt(95) + 1920;

            String newBirthDate = month + "/" + day + "/" + year;

            datalist.getRecord(i).setDataValue("birthDate", newBirthDate);

            if (updateAge) {

                Date parsedBirthDate =
                        controller.getDateUtil().parseDate(newBirthDate);

                int newAge =
                        controller.getDateUtil().getAge(parsedBirthDate);

                datalist.getRecord(i).setInt("age", newAge);
            }
        }
    }

    private void addAddress(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("address");

        DataList streetList =
                controller.getPal().getDataList("streets");

        int streetListRows = streetList.getRecordCount();

        for (int i = 0; i < num; i++) {

            int houseNum = randomInt(3000) + 1;

            String street =
                    streetList.getRecord(randomInt(streetListRows))
                            .get("streets");

            String newAddress = houseNum + " " + street;

            datalist.getRecord(i).setDataValue("address", newAddress);
        }
    }

    private void addCity(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("city");

        DataList cityList =
                controller.getPal().getDataList("cities");

        int cityListRows = cityList.getRecordCount();

        for (int i = 0; i < num; i++) {

            String newCity =
                    cityList.getRecord(randomInt(cityListRows))
                            .get("cities");

            datalist.getRecord(i).setDataValue("city", newCity);
        }
    }

    private void addState(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("state");

        DataList stateList =
                controller.getBusinessUtil().getStateList("US");

        for (int i = 0; i < num; i++) {

            String newState =
                    stateList.getRecord(randomInt(50))
                            .get("name");

            datalist.getRecord(i).setDataValue("state", newState);
        }
    }

    private void addSsn(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("ssn");

        for (int i = 0; i < num; i++) {

            String newSsn =
                    randomDigits(3)
                            + "-"
                            + randomDigits(2)
                            + "-"
                            + randomDigits(4);

            datalist.getRecord(i).setDataValue("ssn", newSsn);
        }
    }

    private void addPhoneNumber(PacketDataList datalist, Data data, int num)
            throws WorkflowException {

        datalist.addColumn("phoneNumber");

        String phones =
                data.getDefaultValue("phones", null, true);

        String[] parts =
                phones == null ? null : phones.split(",");

        int x = 0;

        for (int i = 0; i < num; i++) {

            String newPhone;

            if (phones == null || parts.length == 0) {

                newPhone =
                        randomDigits(3)
                                + "-"
                                + randomDigits(3)
                                + "-"
                                + randomDigits(4);

            } else {

                newPhone = parts[x].trim();

                x++;

                if (x > parts.length - 1) {
                    x = 0;
                }
            }

            datalist.getRecord(i)
                    .setDataValue("phoneNumber", newPhone);
        }
    }

    private void addZipCode(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("zipCode");

        for (int i = 0; i < num; i++) {

            datalist.getRecord(i)
                    .setDataValue("zipCode", randomDigits(5));
        }
    }

    private void addEmail(PacketDataList datalist, Data data, int num)
            throws WorkflowException {

        datalist.addColumn("email");

        String userEmail = data.get("userEmail");

        if (userEmail != null) {

            for (int i = 0; i < num; i++) {

                String m = userEmail;
                m = m.replace("@", "+" + i + "@");

                datalist.getRecord(i).setDataValue("email", m);

            }
        }

    }

    private int getRandomDayForMonth(int month) {

        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return randomInt(30) + 1;
        }

        if (month == 2) {
            return randomInt(28) + 1;
        }

        return randomInt(31) + 1;
    }

    private String randomDigits(int amount) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < amount; i++) {
            result.append(randomInt(10));
        }

        return result.toString();
    }

    private int randomInt(int max) {
        return (int) Math.floor(Math.random() * max);
    }
}