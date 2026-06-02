package org.brendan;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.DataList;
import com.nxlight.framework.pal.workflow.common.DataRecord;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

import java.util.ArrayList;
import java.util.HashMap;

public class RandomName {

    private HashMap<String, Person> map = new HashMap<String, Person>();
    private ArrayList<Person> list = new ArrayList<Person>();

    private CommonController c;

    private DataList firstList;
    private DataList lastList;

    private int namesNumber;

    private int firstRows;
    private int lastRows;

    public RandomName(CommonController c, int namesNumber)
            throws WorkflowException {

        this.c = c;

        firstList = c.getPal().getDataList("firstNames");
        lastList = c.getPal().getDataList("lastNames");

        firstRows = firstList.getRecordCount();
        lastRows = lastList.getRecordCount();

        this.namesNumber = namesNumber;
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