/*
 * Filename	: ReverseGeocoding.java
 * Function	:
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.api.bean;

import java.util.List;

public class ReverseGeocode {
  public static class Point {
    public double x;
    public double y;
  }

  public static class AddressDetail {
    public String country;
    public String sido;
    public String sigugun;
    public String dongmyun;
    public String rest;
  }

  public static class Item {
    public String address;
    public AddressDetail addrdetail;
    public boolean isRoadAddress;
    public Point point;
  }

  public static class Result {
    public int total;
    public String userquery;
    public List<Item> items;

    public Item getFirstItem() {
      if (items != null && items.size() > 0) {
        return items.get(0);
      }
      return null;
    }
  }

  public Result result;
}
