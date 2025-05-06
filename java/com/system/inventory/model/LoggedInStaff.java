package com.system.inventory.model;

public class LoggedInStaff {
    private String staffName;
    private String position;

    public LoggedInStaff(String staffName, String position) {
        this.staffName = staffName;
        this.position = position;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getPosition() {
        return position;
    }


}
