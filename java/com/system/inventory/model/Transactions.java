package com.system.inventory.model;

public class Transactions {

    int qty, id;
    double price;
    String name, type, brand, series, sup, date;

    public Transactions(Integer id, String type, String name, String sup, String brand, String series,
                        Integer qty, Double price, String date){

        this.id = id;
        this.type = type;
        this.name = name;
        this.sup = sup;
        this.brand = brand;
        this.series = series;
        this.qty = qty;
        this.price = price;
        this.date = date;

    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getSup() {
        return sup;
    }

    public void setSup(String sup) {
        this.sup = sup;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
