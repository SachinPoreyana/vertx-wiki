package io.vertx.starter.geolocation;


import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.lang.*;
import java.io.*;

public class DistanceCalculator
{
  public static final Logger LOGGER = LoggerFactory.getLogger(DistanceCalculator.class);
  public static Double geoDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
    if ((lat1 == lat2) && (lon1 == lon2)) {
      return 0D;
    }
    else {
      double theta = lon1 - lon2;
      double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
      dist = Math.acos(dist);
      dist = Math.toDegrees(dist);
      dist = dist * 60 * 1.1515;
      LOGGER.error("THe distance is " + dist);
      return (dist);
    }
  }
}
