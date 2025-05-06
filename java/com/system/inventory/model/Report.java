package com.system.inventory.model;

public class Report {

    int qty, total;
    double price;

    String name, brand, series;

    public Report (String name, String brand, String series, Integer total, Integer qty, Double price){
        this.name = name;
        this.brand = brand;
        this.series = series;
        this. total = total;
        this.qty = qty;
        this.price = price;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }
}
