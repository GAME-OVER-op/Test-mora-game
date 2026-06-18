package com.mora.gamedock;

/** Мгновенный снимок показателей системы. */
public class Metrics {
    public double cpuLoad = 0;     // 0..1
    public double cpuFreqGhz = 0;
    public double gpuBusy = 0;     // 0..1
    public double gpuFreqMhz = 0;
    public int battery = 0;        // %
    public double tempC = 0;       // °C
}
