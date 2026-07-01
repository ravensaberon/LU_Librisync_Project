package com.latteandletters.util;

public class AddressFormValue {

    private String province = "Laguna";
    private String cityMunicipality;
    private String barangay;
    private String street;
    private String zipcode;

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCityMunicipality() {
        return cityMunicipality;
    }

    public void setCityMunicipality(String cityMunicipality) {
        this.cityMunicipality = cityMunicipality;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }
}
