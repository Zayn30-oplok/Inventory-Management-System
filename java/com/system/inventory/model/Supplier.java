package com.system.inventory.model;

public class Supplier {

    private int id;
    private String name, brands, contact;

    public Supplier(Integer id, String name, String brands, String contact){
        this.id = id;
        this.name = name;
        this.brands = brands;
        this.contact = contact;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrands() {
        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
