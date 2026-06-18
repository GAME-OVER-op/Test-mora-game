package com.mora.gamedock;

/** Мгновенный снимок показателей системы. */
public class Metrics {
    public double cpuLoad = 0;     // 0..1 (нагрузка, инфо)
    public double cpuFreqGhz = 0;
    public double cpuFill = 0;     // 0..1 = текущая частота / максимум
    public double gpuBusy = 0;     // 0..1 (занятость, инфо)
    public double gpuFreqMhz = 0;
    public double gpuFill = 0;     // 0..1 = текущая частота / максимум
    public int battery = 0;        // %
    public double tempC = 0;       // °C
}
