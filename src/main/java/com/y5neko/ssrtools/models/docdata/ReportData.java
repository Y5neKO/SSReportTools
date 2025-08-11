package com.y5neko.ssrtools.models.docdata;

import java.util.ArrayList;
import java.util.List;

/**
 * 报告数据模型
 */
public class ReportData {
    private List<Unit> units = new ArrayList<>();

    public List<Unit> getUnits() {
        return units;
    }

    public void setUnits(List<Unit> units) {
        this.units = units;
    }
}