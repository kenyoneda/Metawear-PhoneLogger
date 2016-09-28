package com.mbientlab.metawear.starter;

import com.mbientlab.metawear.data.CartesianFloat;

/**
 * Class to hold sensor timestamp & record
 */
public class SensorRecord {
    private String timestamp;
    private CartesianFloat record;

    public SensorRecord(String timestamp, CartesianFloat record) {
        this.timestamp = timestamp;
        this.record = record;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public CartesianFloat getRecord() {
        return record;
    }

    @Override
    public String toString() {
        String s = record.toString();
        return timestamp + ", " + s.substring(1, s.length() - 1);
    }
}

