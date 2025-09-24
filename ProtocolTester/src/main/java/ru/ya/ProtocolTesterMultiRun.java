package ru.ya;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-run protocol tester:
 * - performs RUNS runs
 * - each run does ITERATIONS requests using THREAD_COUNT threads
 * - prints per-run statistics and then averages across runs + aggregated statistics
 */
public class ProtocolTesterMultiRun {
    // CONFIGURE
    private static final String SENDER_URL = "http://localhost:8000/api/sender/send?protocol=grpc";
    private static final String REQUEST_JSON = """
            [
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              },
              {
                "n": 4,
                "m": 4,
                "feederRow": 0,
                "feederCol": 0,
                "fleasCount": 16,
                "fleas": [
                  {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
                  {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
                  {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
                  {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
                ]
              }
            ]
            """;

    private static final int RUNS = 5;            // number of full runs

    private static final int ITERATIONS = 30;     // requests per run
    private static final int THREAD_COUNT = 10;    // parallel threads per run

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    public static void main(String[] args) throws Exception {
        System.out.println("ProtocolTesterMultiRun");
        System.out.println("Target: " + SENDER_URL);
        System.out.println("Runs: " + RUNS + ", Iterations/run: " + ITERATIONS + ", Threads: " + THREAD_COUNT);
        System.out.println();

        // ensure output dir exists (optional)
        try {
            Files.createDirectories(Paths.get("output"));
        } catch (Exception ignored) {}

        // store per-run aggregated metrics
        List<Double> runAvgMs = new ArrayList<>();
        List<Double> runMedianMs = new ArrayList<>();
        List<Double> runP90Ms = new ArrayList<>();
        List<Double> runP95Ms = new ArrayList<>();
        List<Double> runMaxMs = new ArrayList<>();
        List<Double> runThroughput = new ArrayList<>();
        List<Long> allDurationsNs = new ArrayList<>(); // aggregated raw durations across all runs

        for (int run = 1; run <= RUNS; run++) {
            System.out.printf("=== RUN %d/%d ===%n", run, RUNS);

            // run single batch
            RunResult result = runSingleBatch(ITERATIONS, THREAD_COUNT);

            // print per-run summary (milliseconds)
            System.out.println();
            System.out.printf("Run %d summary:%n", run);
            System.out.printf("Submitted: %d, Success: %d, Failures: %d%n",
                    result.submitted, result.successes, result.failures);
            System.out.printf("Wall time (submit->all-complete): %.3f s, Throughput: %.2f req/s%n",
                    result.wallSec, result.throughput);

            printStatsMs(result.durationsNs);

            // collect metrics for averaging
            runAvgMs.add(nanosToMillisDouble(avg(result.durationsNs)));
            runMedianMs.add(nanosToMillisDouble(median(result.sortedDurationsNs)));
            runP90Ms.add(nanosToMillisDouble(percentile(result.sortedDurationsNs, 90)));
            runP95Ms.add(nanosToMillisDouble(percentile(result.sortedDurationsNs, 95)));
            runMaxMs.add(nanosToMillisDouble(Collections.max(result.sortedDurationsNs)));
            runThroughput.add(result.throughput);

            // aggregate raw durations
            allDurationsNs.addAll(result.durationsNs);

            // short pause between runs to stabilize network (optional)
            Thread.sleep(500);
        }

        // After all runs: compute means of per-run metrics and aggregated statistics
        System.out.println();
        System.out.println("=== MULTI-RUN SUMMARY ===");
        System.out.printf("Runs: %d, Iterations/run: %d, Total requests attempted: %d%n",
                RUNS, ITERATIONS, RUNS * ITERATIONS);

        // mean of per-run metrics
        System.out.printf("Mean of per-run averages: %s ms%n", formatDouble(mean(runAvgMs)));
        System.out.printf("Mean of per-run medians: %s ms%n", formatDouble(mean(runMedianMs)));
        System.out.printf("Mean of per-run p90: %s ms%n", formatDouble(mean(runP90Ms)));
        System.out.printf("Mean of per-run p95: %s ms%n", formatDouble(mean(runP95Ms)));
        System.out.printf("Mean of per-run max: %s ms%n", formatDouble(mean(runMaxMs)));
        System.out.printf("Mean throughput (req/s) across runs: %s req/s%n", formatDouble(mean(runThroughput)));

        // aggregated stats across all successful requests
        System.out.println();
        System.out.println("Aggregated statistics across all successful requests (all runs combined):");
        if (!allDurationsNs.isEmpty()) {
            printStatsMs(allDurationsNs);
        } else {
            System.out.println("No successful requests collected.");
        }

        System.out.println("Done.");
    }

    /** Perform a single run: submit N tasks on THREADS and collect per-request roundtrip durations */
    private static RunResult runSingleBatch(int iterations, int threads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CompletionService<Long> completion = new ExecutorCompletionService<>(executor);
        AtomicInteger failures = new AtomicInteger(0);
        List<Long> durationsNs = Collections.synchronizedList(new ArrayList<>(iterations));

        long submitStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            final int idx = i + 1;
            completion.submit(() -> {
                try {
                    long duration = singlePostRoundtrip(SENDER_URL, REQUEST_JSON);
                    durationsNs.add(duration);
                    System.out.printf("  #%d completed: %.3f ms (thread=%s)%n", idx, nanosToMillis(duration), Thread.currentThread().getName());
                    return duration;
                } catch (Exception e) {
                    failures.incrementAndGet();
                    System.err.printf("  #%d failed: %s%n", idx, e.toString());
                    return -1L;
                }
            });
        }

        // wait for completions
        int received = 0;
        for (int i = 0; i < iterations; i++) {
            try {
                Future<Long> f = completion.take();
                Long v = f.get(); // task catches exceptions and increments failures
                received++;
            } catch (ExecutionException ee) {
                failures.incrementAndGet();
                System.err.println("Task exception: " + ee.getMessage());
            }
        }
        long submitEnd = System.nanoTime();
        executor.shutdown();

        // compute throughput for this run: successes / wall time
        int successes = durationsNs.size();
        long wallNs = submitEnd - submitStart;
        double wallSec = wallNs / 1_000_000_000.0;
        double throughput = wallSec > 0 ? successes / wallSec : 0.0;

        // sort copy for percentiles
        List<Long> sorted = new ArrayList<>(durationsNs);
        Collections.sort(sorted);

        return new RunResult(iterations, successes, failures.get(), durationsNs, sorted, wallSec, throughput);
    }

    /** Perform a single POST and return round-trip duration in ns */
    private static long singlePostRoundtrip(String targetUrl, String json) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        byte[] payload = json.getBytes(StandardCharsets.UTF_8);

        long sendStart = System.nanoTime();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int code = conn.getResponseCode();
        if (code != 200 && code != 201 && code != 204) {
            String err = readStream(conn.getErrorStream());
            throw new RuntimeException("HTTP " + code + (err == null ? "" : " : " + err));
        }

        // read full response
        String response = readStream(conn.getInputStream());
        long recvTime = System.nanoTime();
        conn.disconnect();

        // response can be validated/ignored
        return recvTime - sendStart;
    }

    private static String readStream(InputStream in) {
        if (in == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /* ----- statistics helpers ----- */

    private static void printStatsMs(List<Long> durationsNs) {
        List<Long> copy = new ArrayList<>(durationsNs);
        Collections.sort(copy);
        int n = copy.size();
        double sumNs = 0;
        for (Long v : copy) sumNs += v;
        double avgNs = n > 0 ? sumNs / n : 0;
        long minNs = n > 0 ? copy.get(0) : 0;
        long maxNs = n > 0 ? copy.get(n - 1) : 0;
        double medianNs = median(copy);
        double p90Ns = percentile(copy, 90);
        double p95Ns = percentile(copy, 95);
        double stddevMs = stddev(copy) / 1_000_000.0;

        DecimalFormat df = new DecimalFormat("#,##0.000");

        System.out.println();
        System.out.println("Per-request round-trip (milliseconds):");
        System.out.printf("count: %d%n", n);
        System.out.printf("min: %s ms%n", df.format(nanosToMillis(minNs)));
        System.out.printf("avg: %s ms%n", df.format(nanosToMillisDouble(avgNs)));
        System.out.printf("median: %s ms%n", df.format(nanosToMillisDouble(medianNs)));
        System.out.printf("p90: %s ms%n", df.format(nanosToMillisDouble(p90Ns)));
        System.out.printf("p95: %s ms%n", df.format(nanosToMillisDouble(p95Ns)));
        System.out.printf("max: %s ms%n", df.format(nanosToMillis(maxNs)));
        System.out.printf("stddev: %s ms%n", df.format(stddevMs));
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        double idx = (p / 100.0) * (n - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted.get(lo);
        double w = idx - lo;
        return Math.round(sorted.get(lo) * (1 - w) + sorted.get(hi) * w);
    }

    private static double median(List<Long> sorted) {
        int n = sorted.size();
        if (n == 0) return 0;
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static double avg(List<Long> durations) {
        if (durations.isEmpty()) return 0.0;
        double sum = 0;
        for (Long v : durations) sum += v;
        return sum / durations.size();
    }

    private static double stddev(List<Long> values) {
        int n = values.size();
        if (n <= 1) return 0.0;
        double mean = 0;
        for (Long v : values) mean += v;
        mean /= n;
        double sumsq = 0;
        for (Long v : values) {
            double d = v - mean;
            sumsq += d * d;
        }
        return Math.sqrt(sumsq / (n - 1));
    }

    /* Convert nanoseconds to milliseconds - overloads for long and double */
    private static double nanosToMillis(long ns) {
        return ns / 1_000_000.0;
    }

    private static double nanosToMillisDouble(double ns) {
        return ns / 1_000_000.0;
    }

    private static String formatDouble(double v) {
        return new DecimalFormat("#,##0.000").format(v);
    }

    private static double mean(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double s = 0;
        for (Double d : vals) s += d;
        return s / vals.size();
    }

    /** Simple data holder for a run */
    private static class RunResult {
        final int submitted;
        final int successes;
        final int failures;
        final List<Long> durationsNs;
        final List<Long> sortedDurationsNs;
        final double wallSec;
        final double throughput;

        RunResult(int submitted, int successes, int failures, List<Long> durationsNs, List<Long> sortedDurationsNs, double wallSec, double throughput) {
            this.submitted = submitted;
            this.successes = successes;
            this.failures = failures;
            this.durationsNs = durationsNs;
            this.sortedDurationsNs = sortedDurationsNs;
            this.wallSec = wallSec;
            this.throughput = throughput;
        }
    }
}
