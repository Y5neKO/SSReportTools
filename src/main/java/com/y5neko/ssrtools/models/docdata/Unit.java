package com.y5neko.ssrtools.models.docdata;

import java.util.ArrayList;
import java.util.List;

public class Unit {
    private String unitName;
    private List<SystemInfo> systems = new ArrayList<>();

    public Unit(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public List<SystemInfo> getSystems() {
        return systems;
    }

    public void setSystems(List<SystemInfo> systems) {
        this.systems = systems;
    }
}