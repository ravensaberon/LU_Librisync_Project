package com.latteandletters.dto;

import com.latteandletters.model.User;

import java.io.Serializable;
import java.time.LocalDate;

public class StudentProfileUpdateRequest implements Serializable {

    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private String course;
    private String yearLevel;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;

    public StudentProfileUpdateRequest() {
    }

    public StudentProfileUpdateRequest(String name,
                                      String course,
                                       String yearLevel,
                                       String phone,
                                       String address,
                                       LocalDate dateOfBirth) {
        setName(name);
        this.course = course;
        this.yearLevel = yearLevel;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    public StudentProfileUpdateRequest(String firstName,
                                       String middleName,
                                       String lastName,
                                       String suffix,
                                       String course,
                                       String yearLevel,
                                       String phone,
                                       String address,
                                       LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.suffix = suffix;
        this.course = course;
        this.yearLevel = yearLevel;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        User user = new User();
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setSuffix(suffix);
        return user.getName();
    }

    public void setName(String name) {
        User user = new User();
        user.setName(name);
        this.firstName = user.getFirstName();
        this.middleName = user.getMiddleName();
        this.lastName = user.getLastName();
        this.suffix = user.getSuffix();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
