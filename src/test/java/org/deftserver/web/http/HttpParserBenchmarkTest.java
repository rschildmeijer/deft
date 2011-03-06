package org.deftserver.web.http;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpParserBenchmarkTest {

    private String request = "POST /path/script.cgi HTTP/1.0\r\n"
            + "From: frog@jmarshall.com\r\n" + "User-Agent: HTTPTool/1.0\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Content-Length: 32\r\n\r\n"
            + "home=Cosby&favorite+flavor=flies\r\n";

    private static final int CYCLES = 100000;

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpParserBenchmarkTest.class);

    private static final HttpRequestParser parser = new HttpRequestParser();

    @Test
    public void loadTestHttpRequestParser() {

        ByteBuffer buff = ByteBuffer.wrap(request.getBytes());
        for (int nb = 0; nb < 10; nb++) {
            int i = 0;
            StopWatch sw = new StopWatch();
            TimeStat serTime = new TimeStat();
            while (i < CYCLES) {
                buff.clear();
                sw.start();
                HttpRequest req = parseWithParser(buff);
                serTime.add(sw.stop());
                req.getBodyBuffer().flip();
                assertEquals("home=Cosby&favorite+flavor=flies", req.getBody());
                Assert.assertTrue("Request should be finished",
                        req.isFinished());
                assertEquals("frog@jmarshall.com", req.getHeader("from"));
                assertEquals("HTTPTool/1.0", req.getHeader("user-agent"));
                assertEquals("application/x-www-form-urlencoded",
                        req.getHeader("content-type"));
                assertEquals("32", req.getHeader("content-length"));
                assertEquals("POST", req.getMethod().toString());
                i++;
            }
            LOG.info(
                    "TimeStats parsing with HttpRequestParser\t\t: Min={}, Max={}, Avg={}",
                    new String[] { serTime.min + "", serTime.max + "",
                            serTime.getAvg(CYCLES) + "" });
        }
    }

    @Test
    public void loadTestHttpRequest() {
        ByteBuffer buff = ByteBuffer.wrap(request.getBytes());
        for (int nb = 0; nb < 10; nb++) {
            int i = 0;
            StopWatch sw = new StopWatch();
            TimeStat serTime = new TimeStat();
            while (i < CYCLES) {
                buff.clear();
                sw.start();
                HttpRequest req = parseWithRequest(buff);
                serTime.add(sw.stop());
                // assertEquals("home=Cosby&favorite+flavor=flies",
                // req.getBody());
                Assert.assertTrue("Request should be finished",
                        req.isFinished());
                assertEquals("frog@jmarshall.com", req.getHeader("from"));
                assertEquals("HTTPTool/1.0", req.getHeader("user-agent"));
                assertEquals("application/x-www-form-urlencoded",
                        req.getHeader("content-type"));
                assertEquals("32", req.getHeader("content-length"));
                assertEquals("POST", req.getMethod().toString());
                i++;
            }
            LOG.info(
                    "TimeStats parsing with HttpRequest\t\t: Min={}, Max={}, Avg={}",
                    new String[] { serTime.min + "", serTime.max + "",
                            serTime.getAvg(CYCLES) + "" });
        }
    }

    private HttpRequest parseWithParser(ByteBuffer buffer) {
        return parser.parseRequestBuffer(buffer);
    }

    private HttpRequest parseWithRequest(ByteBuffer buffer) {
        return HttpRequestFactory.of(buffer);
    }

    /**
     * Objet utilis pour calculer les temps de traitements minimum, maximum et
     * moyen. A chaque itration, il suffit d'ajouter le temps de traitement au
     * TimeStat pour mettre jour les statistiques de temps.
     * 
     * @author slm
     * 
     */
    class TimeStat {

        long avg = 0;

        long min = 0;

        long max = 0;

        public void add(long time) {
            avg += time;

            if (min == 0) {
                min = time;
                max = time;
            } else if (min > time) {
                min = time;
            } else if (max < time) {
                max = time;
            }
        }

        public long getAvg(int cycles) {
            return avg / cycles;
        }
    }

    /**
     * Compteur de temps en nanosecondes
     * 
     * @author slm
     * 
     */
    class StopWatch {

        long nanos;

        /**
         * Initialisation du compteur
         */
        public void start() {
            nanos = System.nanoTime();
        }

        /**
         * Arret du compteur et retour du temps en nanosecondes depuis le
         * dernier appel {@link #start()}
         * 
         * @return
         */
        public long stop() {
            return System.nanoTime() - nanos;
        }

    }

}
