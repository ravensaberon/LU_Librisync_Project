package com.lulibrisync.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class StudentProfileUpdateRequest implements Serializable {

    private String name;
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
        this.name = name;
        this.course = course;
        this.yearLevel = yearLevel;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
