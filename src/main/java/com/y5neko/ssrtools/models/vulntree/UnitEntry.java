package com.y5neko.ssrtools.models.vulntree;

import java.util.ArrayList;
import java.util.List;

// 单位（包含多个系统）
public class UnitEntry {
    public String unit;
    public List<SystemEntry> systems = new ArrayList<>();
}