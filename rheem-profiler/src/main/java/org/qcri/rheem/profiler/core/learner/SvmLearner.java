package org.qcri.rheem.profiler.core.learner;

import org.junit.*;
import smile.data.AttributeDataset;
import smile.data.parser.ArffParser;
import smile.math.Math;
import smile.math.kernel.GaussianKernel;
import smile.math.kernel.MercerKernel;
import smile.regression.LASSO;
import smile.regression.RandomForest;
import smile.regression.RidgeRegression;
import smile.regression.SVR;
import smile.validation.CrossValidation;
import smile.validation.LOOCV;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/**
 * Created by FLVBSLV on 10/15/2017.
 */
public class SvmLearner {

    double[][] longley = {
            {234.289,      235.6,        159.0,    107.608, 1947,   60.323},
            {259.426,      232.5,        145.6,    108.632, 1948,   61.122},
            {258.054,      368.2,        161.6,    109.773, 1949,   60.171},
            {284.599,      335.1,        165.0,    110.929, 1950,   61.187},
            {328.975,      209.9,        309.9,    112.075, 1951,   63.221},
            {346.999,      193.2,        359.4,    113.270, 1952,   63.639},
            {365.385,      187.0,        354.7,    115.094, 1953,   64.989},
            {363.112,      357.8,        335.0,    116.219, 1954,   63.761},
            {397.469,      290.4,        304.8,    117.388, 1955,   66.019},
            {419.180,      282.2,        285.7,    118.734, 1956,   67.857},
            {442.769,      293.6,        279.8,    120.445, 1957,   68.169},
            {444.546,      468.1,        263.7,    121.950, 1958,   66.513},
            {482.704,      381.3,        255.2,    123.366, 1959,   68.655},
            {502.601,      393.1,        251.4,    125.368, 1960,   69.564},
            {518.173,      480.6,        257.2,    127.852, 1961,   69.331},
            {554.894,      400.7,        282.7,    130.081, 1962,   70.551}
    };

    double[] y = {
            83.0,  88.5,  88.2,  89.5,  96.2,  98.1,  99.0, 100.0, 101.2,
            104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9
    };

    double[] residuals = {
            -0.6008156,  1.5502732,  0.1032287, -1.2306486, -0.3355139,  0.2693345,  0.8776759,
            0.1222429, -2.0086121, -0.4859826,  1.0663129,  1.2274906, -0.3835821,  0.2710215,
            0.1978569, -0.6402823
    };

    public SvmLearner() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    public static class doubleInstance {
        public int label;

        public int getLabel() {
            return label;
        }

        public double[] getX() {
            return x;
        }

        public double[] x;

        public doubleInstance(int label, double[] x) {
            this.label = label;
            this.x = x;
        }
    }


    public static List<RidgeRegressionLearner.doubleInstance> readDataSet(String file) throws FileNotFoundException {
        List<RidgeRegressionLearner.doubleInstance> dataset = new ArrayList<RidgeRegressionLearner.doubleInstance>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(file));
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\\s+");

                // skip first column and last column is the label
                int i = 1;
                //int[] data = new int[columns.length-2];

                //for (i=1; i<columns.length-1; i++) {
                //    data[i-1] = Integer.parseInt(columns[i]);
                //}
                double[] data = new double[columns.length-1];
                for (i=0; i<columns.length-1; i++) {
                    data[i] = Double.parseDouble(columns[i]);
                }

                int label = Integer.parseInt(columns[i]);
                RidgeRegressionLearner.doubleInstance instance = new RidgeRegressionLearner.doubleInstance(label, data);
                dataset.add(instance);
            }
        } finally {
            if (scanner != null)
                scanner.close();
        }
        return dataset;
    }

    /**
     * Test of learn method, of class RidgeRegressionLearner.
     */
    @Test
    public void learnCosts() throws FileNotFoundException {
        System.out.println("learn");

        // read data
        List<RidgeRegressionLearner.doubleInstance> instanceList = readDataSet(this.getClass().getResource("/planVectors").getFile());

        double[][] X = new double[instanceList.size()][instanceList.get(0).getX().length];
        double[] Y = new double[instanceList.size()];

        //instanceList.stream().map((Logistic.Instance::getX)).collect(new int[1]);
        int index =0;
        for(RidgeRegressionLearner.doubleInstance x:instanceList){
            X[index]=x.getX();
            Y[index]=x.getLabel();
            index++;
        }

        y=Y;
        longley = X;
        //RidgeRegression model = new RidgeRegression(longley, y, 0.0);
        //SVR model = new SVR(longley, y,new GaussianKernel(0.1), 1.0,1);
        RandomForest model = new RandomForest(X, Y, 1000);
        double rss = 0.0;
        int n = longley.length;
        for (int i = 0; i < n; i++) {
            double r =  y[i] - model.predict(longley[i]);
            //assertEquals(residuals[i], r, 1E-7);
            rss += r * r;
        }
        System.out.println("Training MSE = " + rss/n);

        //model = new RidgeRegression(longley, y, 0.1);
        //model = new SVR(longley, y, new GaussianKernel(1), 1.0,1);
        for(int i=1;i<=12;i++) {
            System.out.println(String.format("estimated time for %s-%s in %s : %s (real %f)", Double.toString(longley[i][103]), Double.toString(longley[i][104]),
                    (i % 2 == 0 ? "java" : "spark"),Double.toString(model.predict(longley[i])),y[i]));
        }
        System.out.println(Double.toString(model.predict(longley[1])));
        //System.out.println(Double.toString(model.predict(longley[3])));
        //System.out.println(Double.toString(model.predict(longley[4])));
        /*
        assertEquals(-1.354007e+03, model.intercept(), 1E-3);
        assertEquals(5.457700e-02, model.coefficients()[0], 1E-7);
        assertEquals(1.198440e-02, model.coefficients()[1], 1E-7);
        assertEquals(1.261978e-02, model.coefficients()[2], 1E-7);
        assertEquals(-1.856041e-01, model.coefficients()[3], 1E-7);
        assertEquals(7.218054e-01, model.coefficients()[4], 1E-7);
        assertEquals(5.884884e-01, model.coefficients()[5], 1E-7);


        LOOCV loocv = new LOOCV(n);
        rss = 0.0;
        for (int i = 0; i < n; i++) {
            double[][] trainx = Math.slice(longley, loocv.train[i]);
            double[] trainy = Math.slice(y, loocv.train[i]);
            RidgeRegression ridge = new RidgeRegression(trainx, trainy, 0.1);

            double r = y[loocv.test[i]] - ridge.predict(longley[loocv.test[i]]);
            rss += r * r;
        }
        */
        //System.out.println("LOOCV MSE = " + rss/n);
    }

    /**
     * Test of learn method, of class RidgeRegressionLearner.
     */
    @Test
    public void testLearn() {
        System.out.println("learn");
        RidgeRegression model = new RidgeRegression(longley, y, 0.0);

        double rss = 0.0;
        int n = longley.length;
        for (int i = 0; i < n; i++) {
            double r =  y[i] - model.predict(longley[i]);
            assertEquals(residuals[i], r, 1E-7);
            rss += r * r;
        }
        System.out.println("Training MSE = " + rss/n);

        model = new RidgeRegression(longley, y, 0.1);

        assertEquals(-1.354007e+03, model.intercept(), 1E-3);
        assertEquals(5.457700e-02, model.coefficients()[0], 1E-7);
        assertEquals(1.198440e-02, model.coefficients()[1], 1E-7);
        assertEquals(1.261978e-02, model.coefficients()[2], 1E-7);
        assertEquals(-1.856041e-01, model.coefficients()[3], 1E-7);
        assertEquals(7.218054e-01, model.coefficients()[4], 1E-7);
        assertEquals(5.884884e-01, model.coefficients()[5], 1E-7);

        LOOCV loocv = new LOOCV(n);
        rss = 0.0;
        for (int i = 0; i < n; i++) {
            double[][] trainx = Math.slice(longley, loocv.train[i]);
            double[] trainy = Math.slice(y, loocv.train[i]);
            RidgeRegression ridge = new RidgeRegression(trainx, trainy, 0.1);

            double r = y[loocv.test[i]] - ridge.predict(longley[loocv.test[i]]);
            rss += r * r;
        }

        System.out.println("LOOCV MSE = " + rss/n);
    }

    /**
     * Test of predict method, of class RidgeRegressionLearner.
     */
    @Test
    public void testPredict() {
        System.out.println("predict");

        for (int lambda = 0; lambda <= 20; lambda+=2) {
            int n = longley.length;

            LOOCV loocv = new LOOCV(n);
            double rss = 0.0;
            for (int i = 0; i < n; i++) {
                double[][] trainx = Math.slice(longley, loocv.train[i]);
                double[] trainy = Math.slice(y, loocv.train[i]);
                RidgeRegression ridge = new RidgeRegression(trainx, trainy, 0.01*lambda);

                double r = y[loocv.test[i]] - ridge.predict(longley[loocv.test[i]]);
                rss += r * r;
            }

            System.out.format("LOOCV MSE with lambda %.2f = %.3f%n", 0.01*lambda, rss/n);
        }
    }

    /**
     * Test of learn method, of class LinearRegression.
     */
    //@Test
    public void testCPU() {
        System.out.println("CPU");
        ArffParser parser = new ArffParser();
        parser.setResponseIndex(6);
        try {
            AttributeDataset data = parser.parse(smile.data.parser.IOUtils.getTestDataFile("weka/cpu.arff"));
            double[][] datax = data.toArray(new double[data.size()][]);
            double[] datay = data.toArray(new double[data.size()]);

            int n = datax.length;
            int k = 10;

            CrossValidation cv = new CrossValidation(n, k);
            double rss = 0.0;
            for (int i = 0; i < k; i++) {
                double[][] trainx = Math.slice(datax, cv.train[i]);
                double[] trainy = Math.slice(datay, cv.train[i]);
                double[][] testx = Math.slice(datax, cv.test[i]);
                double[] testy = Math.slice(datay, cv.test[i]);

                RidgeRegression ridge = new RidgeRegression(trainx, trainy, 10.0);

                for (int j = 0; j < testx.length; j++) {
                    double r = testy[j] - ridge.predict(testx[j]);
                    rss += r * r;
                }
            }

            System.out.println("10-CV MSE = " + rss / n);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
