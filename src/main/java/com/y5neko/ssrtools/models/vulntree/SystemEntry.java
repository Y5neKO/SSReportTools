package com.y5neko.ssrtools.models.vulntree;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统条目模型
 */
public class SystemEntry {
    public String system;
    public List<Vuln> vulns = new ArrayList<>();
}