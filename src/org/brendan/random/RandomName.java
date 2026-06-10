package org.brendan.random;

import com.nxlight.framework.pal.workflow.common.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class RandomName {

    private static final int MIN_ITEMS = 1;
    private static final int MAX_ITEMS = 1000;

    private HashMap<String, Person> map = new HashMap<String, Person>();
    private ArrayList<Person> list = new ArrayList<Person>();

    private CommonController c;

    private DataList firstList;
    private DataList lastList;

    private int namesNumber;

    private int firstRows;
    private int lastRows;

    public RandomName(CommonController c) {
        this.c = c;
    }

    public RandomName(CommonController c, int namesNumber)
            throws WorkflowException {

        this.c = c;

        firstList = c.getPal().getDataList("firstNames");
        lastList = c.getPal().getDataList("lastNames");

        firstRows = firstList.getRecordCount();
        lastRows = lastList.getRecordCount();

        this.namesNumber = namesNumber;
    }

    // MOVED FROM Brendan.java
    public String execute(Payload payload) throws WorkflowException {

        int items = getSafeItemCount(payload.get("experimentItems"));

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

        c.debug("Plugin Health Check: Online");
        c.debug("Items received by plugin: " + items);
        c.debug("Request classification: " + requestSize);

        long javaStart = System.currentTimeMillis();

        if (payload.getData().getDefaultBoolean("old", true)) {
            payload.addDataList(createRandomList1(items, payload.getData()));
        } else {
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

        PacketDataList generatedList = c.createDataList(
                "newNames",
                new String[]{"first", "last"}
        );

        boolean includeGender = data.getValue("gender") != null;

        if (includeGender) {
            generatedList.addColumn("gender");
        }

        RandomName random = new RandomName(c, namesNumber);

        for (int i = 0; i < namesNumber; i++) {

            Person person = random.getfullname1();

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

        PacketDataList generatedList = c.createDataList(
                "newNames",
                new String[]{"first", "last"}
        );

        boolean includeGender = data.getValue("gender") != null;

        if (includeGender) {
            generatedList.addColumn("gender");
        }

        RandomName random = new RandomName(c, namesNumber);

        for (int i = 0; i < namesNumber; i++) {

            Person person = random.getfullname2();

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
                        c.getDateUtil().parseDate(newBirthDate);

                int newAge =
                        c.getDateUtil().getAge(parsedBirthDate);

                datalist.getRecord(i).setInt("age", newAge);
            }
        }
    }

    private void addAddress(PacketDataList datalist, int num)
            throws WorkflowException {

        datalist.addColumn("address");

        DataList streetList =
                c.getPal().getDataList("streets");

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
                c.getPal().getDataList("cities");

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
                c.getBusinessUtil().getStateList("US");

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

    /*
        Version 1
        Duplicate-safe version
    */
    public Person getfullname1() {

        String fullName;
        String firstName;
        String lastName;
        String gender;

        Person duplicateCheck;

        do {

            int firstNameNum = randomInt(firstRows);

            firstName =
                    firstList.getRecord(firstNameNum)
                            .get("firstName");

            gender =
                    firstList.getRecord(firstNameNum)
                            .get("gender");

            lastName =
                    lastList.getRecord(randomInt(lastRows))
                            .get("lastName");

            fullName = firstName + " " + lastName;

            duplicateCheck = map.get(fullName);

        } while (duplicateCheck != null);

        Person person = new Person();

        person.firstName = firstName;
        person.lastName = lastName;
        person.fullName = fullName;
        person.gender = gender;

        map.put(fullName, person);

        return person;
    }

    /*
        Version 2
        Faster pre-generated list version
    */
    public Person getfullname2() {

        if (list.isEmpty()) {

            for (int i = 0; i < namesNumber; i++) {

                DataRecord first =
                        firstList.getRecord(randomInt(firstRows));

                DataRecord last =
                        lastList.getRecord(randomInt(lastRows));

                Person person = new Person();

                person.firstName = first.get("firstName");
                person.lastName = last.get("lastName");

                person.fullName =
                        person.firstName + " " + person.lastName;

                person.gender = first.get("gender");

                list.add(person);
            }
        }

        int index = randomInt(list.size());

        return list.remove(index);
    }

    private int randomInt(int max) {
        return (int) Math.floor(Math.random() * max);
    }
}