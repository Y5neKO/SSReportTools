package com.y5neko.ssrtools.models.docdata;

import java.util.ArrayList;
import java.util.List;

public class SystemInfo {
    private String systemName;
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    public SystemInfo(String systemName) {
        this.systemName = systemName;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }
}