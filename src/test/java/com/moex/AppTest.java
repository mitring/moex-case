package com.moex;

import org.junit.Assert;
import org.junit.Test;

public class AppTest {

    private String runTest(String fileName) {
        DiscreteAuction auction = new DiscreteAuction();
        auction.readData(ClassLoader.getSystemResourceAsStream(fileName));
        return auction.makeDeal().toString();
    }

    @Test
    public void testDataset01() {
        Assert.assertEquals("0 n/a", runTest("dataset_01.txt"));
    }

    @Test
    public void testDataset02() {
        Assert.assertEquals("150 15.30", runTest("dataset_02.txt"));
    }

    @Test
    public void testDataset03() {
        Assert.assertEquals("1328 73.20", runTest("dataset_03.txt"));
    }

    @Test
    public void testLargeDataset() {
        Assert.assertEquals("124973654 50.47", runTest("dataset_1M.txt"));
    }

}
